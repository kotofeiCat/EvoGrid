package com.EvoGrid.battle.service;

import com.EvoGrid.battle.client.UserClient;
import com.EvoGrid.battle.client.WorldClient;
import com.EvoGrid.battle.dto.*;
import com.EvoGrid.battle.model.AbilityCategory;
import com.EvoGrid.battle.model.BattleSession;
import com.EvoGrid.battle.model.BattleStatus;
import com.EvoGrid.battle.model.StatusEffectType;
import com.EvoGrid.battle.repository.BattleSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BattleService {

    private static final Logger log = LoggerFactory.getLogger(BattleService.class);
    private static final double MAX_BATTLE_DISTANCE_METERS = 100.0; // 70м радиус + запас на погрешность GPS
    private static final double STAB_MULTIPLIER = 1.25;
    private static final double CRIT_MULTIPLIER = 1.50;
    private static final int BASE_CRIT_CHANCE_PERCENT = 10;

    private static final Set<String> ADVANTAGE_PAIRS = Set.of(
            "FIRE->FOREST", "FOREST->WATER", "WATER->FIRE"
    );

    private final BattleSessionRepository battleRepository;
    private final UserClient userClient;
    private final WorldClient worldClient;
    private final AbilityCatalog abilityCatalog;

    public BattleService(BattleSessionRepository battleRepository,
                         UserClient userClient,
                         WorldClient worldClient,
                         AbilityCatalog abilityCatalog) {
        this.battleRepository = battleRepository;
        this.userClient = userClient;
        this.worldClient = worldClient;
        this.abilityCatalog = abilityCatalog;
    }

    @Transactional
    public BattleStateDto startBattle(StartBattleRequest req, Long authUserId) {
        Long effectiveUserId = authUserId != null ? authUserId : req.userId();
        if (effectiveUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }

        UserProfileDto user = fetchUser(effectiveUserId);
        WorldSpawnDto spawn = fetchSpawn(req.spawnId());

        if (!spawn.active()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Этот покемон уже пойман или скрылся!");
        }

        double distance = calculateDistanceMeters(
                user.latitude(), user.longitude(),
                spawn.latitude(), spawn.longitude()
        );
        if (distance > MAX_BATTLE_DISTANCE_METERS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Покемон слишком далеко (" + Math.round(distance) + " м). Подойдите ближе!");
        }

        OwnedPokemonDto fighter = chooseFighterFromParty(user, req.partySlot());

        String startLog = String.format(
                "Бой начался! Тренер %s выпустил [%s (%s, SPD:%d)] против дикого [%s (%s, SPD:%d)].",
                user.email(), fighter.name(), fighter.element(), fighter.speed(),
                spawn.name(), spawn.element(), spawn.speed()
        );

        BattleSession session = battleRepository.save(new BattleSession(
                user.id(), spawn.spawnId(), fighter.speciesId(),
                fighter.name(), fighter.element(), fighter.maxHp(), fighter.attack(), fighter.defense(), fighter.speed(),
                spawn.speciesId(), spawn.name(), spawn.element(), spawn.archetype(),
                spawn.maxHp(), spawn.attack(), spawn.defense(), spawn.speed(),
                startLog
        ));

        return toDto(session);
    }

    // Сохранен для обратной совместимости (вызывает слот 1 по умолчанию)
    @Transactional
    public BattleStateDto attackTurn(Long battleId, Long authUserId) {
        return attackTurn(battleId, authUserId, 1);
    }

    @Transactional
    public BattleStateDto attackTurn(Long battleId, Long authUserId, Integer requestedSlot) {
        BattleSession b = findBattleOrThrow(battleId);
        verifyBattleOwnership(b, authUserId);

        if (b.getStatus() != BattleStatus.IN_PROGRESS) {
            return toDto(b);
        }

        int playerSlot = normalizeSlot(requestedSlot);
        if (b.getPlayerCooldown(playerSlot) > 0) {
            AbilityDto cdAbility = abilityCatalog.getAbilityBySlot(b.getPlayerSpeciesId(), b.getPlayerPokemonName(), playerSlot);
            b.appendLog("Способность «" + cdAbility.name() + "» еще перезаряжается (" + b.getPlayerCooldown(playerSlot) + " х.). Используется обычная атака!");
            playerSlot = 1;
        }

        AbilityDto playerAbility = abilityCatalog.getAbilityBySlot(b.getPlayerSpeciesId(), b.getPlayerPokemonName(), playerSlot);
        int enemySlot = chooseEnemyAbilitySlot(b);
        AbilityDto enemyAbility = abilityCatalog.getAbilityBySlot(b.getEnemySpeciesId(), b.getEnemyPokemonName(), enemySlot);

        int playerEffSpd = b.getEffectivePlayerSpeed();
        int enemyEffSpd = b.getEffectiveEnemySpeed();

        boolean playerFirst;
        if (playerEffSpd > enemyEffSpd) {
            playerFirst = true;
        } else if (enemyEffSpd > playerEffSpd) {
            playerFirst = false;
        } else {
            playerFirst = ThreadLocalRandom.current().nextBoolean();
        }

        b.appendLog(String.format("--- Ход %d (SPD: %s %d vs %s %d) ---",
                b.getTurnNumber(), b.getPlayerPokemonName(), playerEffSpd, b.getEnemyPokemonName(), enemyEffSpd));

        if (playerFirst) {
            executeActorTurn(b, true, playerAbility);
            if (b.getEnemyCurrentHp() > 0 && b.getPlayerCurrentHp() > 0) {
                executeActorTurn(b, false, enemyAbility);
            }
        } else {
            executeActorTurn(b, false, enemyAbility);
            if (b.getPlayerCurrentHp() > 0 && b.getEnemyCurrentHp() > 0) {
                executeActorTurn(b, true, playerAbility);
            }
        }

        // Если никто не погиб от прямых ударов — применяем тики конца раунда (Горение, Яд, Реген)
        if (b.getEnemyCurrentHp() > 0 && b.getPlayerCurrentHp() > 0) {
            applyEndOfRoundTicks(b);
        }

        // Финальная проверка победителя раунда
        if (b.getEnemyCurrentHp() <= 0) {
            b.setEnemyCurrentHp(0);
            b.setStatus(BattleStatus.WON_BY_KO);
            b.appendLog("Дикий " + b.getEnemyPokemonName() + " повержен! +35 XP.");
            safeDeactivateSpawn(b.getSpawnId());
            giveXpReward(b.getUserId(), 35, false);
        } else if (b.getPlayerCurrentHp() <= 0) {
            handlePlayerDefeat(b, false);
        } else {
            b.setTurnNumber(b.getTurnNumber() + 1);
        }

        return toDto(b);
    }

    @Transactional
    public BattleStateDto throwPokeball(Long battleId, Long authUserId) {
        BattleSession b = findBattleOrThrow(battleId);
        verifyBattleOwnership(b, authUserId);

        if (b.getStatus() != BattleStatus.IN_PROGRESS) {
            return toDto(b);
        }

        UserProfileDto user = fetchUser(b.getUserId());
        if (user.pokeballs() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "У вас закончились покеболы!");
        }

        double hpRatio = (double) b.getEnemyCurrentHp() / b.getEnemyMaxHp();
        double catchChance = Math.min(0.95, 0.35 + (1.0 - hpRatio) * 0.65);
        int chancePercent = (int) Math.round(catchChance * 100);

        b.appendLog(String.format("--- Ход %d ---", b.getTurnNumber()));
        b.appendLog(String.format("Бросок покебола в %s (HP: %d/%d, шанс: %d%%)...",
                b.getEnemyPokemonName(), b.getEnemyCurrentHp(), b.getEnemyMaxHp(), chancePercent));

        if (ThreadLocalRandom.current().nextDouble() <= catchChance) {
            b.setStatus(BattleStatus.CAPTURED);
            b.appendLog("Успех! " + b.getEnemyPokemonName() + " пойман! (+50 XP)");
            safeDeactivateSpawn(b.getSpawnId());
            safeApplyReward(b.getUserId(), new BattleRewardRequest(
                    50, true, true,
                    b.getEnemySpeciesId(), b.getEnemyPokemonName(), b.getEnemyElement(), b.getEnemyArchetype(),
                    b.getEnemyMaxHp(), b.getEnemyAttack(), b.getEnemyDefense(), b.getEnemySpeed()
            ));
        } else {
            b.appendLog("Неудача! " + b.getEnemyPokemonName() + " вырвался из покебола!");
            giveXpReward(b.getUserId(), 0, true);

            int enemySlot = chooseEnemyAbilitySlot(b);
            AbilityDto enemyAbility = abilityCatalog.getAbilityBySlot(b.getEnemySpeciesId(), b.getEnemyPokemonName(), enemySlot);
            executeActorTurn(b, false, enemyAbility);

            if (b.getEnemyCurrentHp() > 0 && b.getPlayerCurrentHp() > 0) {
                applyEndOfRoundTicks(b);
            }

            if (b.getEnemyCurrentHp() <= 0) {
                b.setEnemyCurrentHp(0);
                b.setStatus(BattleStatus.WON_BY_KO);
                b.appendLog("Дикий " + b.getEnemyPokemonName() + " повержен! +35 XP.");
                safeDeactivateSpawn(b.getSpawnId());
                giveXpReward(b.getUserId(), 35, false);
            } else if (b.getPlayerCurrentHp() <= 0) {
                handlePlayerDefeat(b, false);
            } else {
                b.setTurnNumber(b.getTurnNumber() + 1);
            }
        }

        return toDto(b);
    }

    public BattleStateDto getBattleState(Long battleId, Long authUserId) {
        BattleSession b = findBattleOrThrow(battleId);
        verifyBattleOwnership(b, authUserId);
        return toDto(b);
    }

    // ==================== ВЫПОЛНЕНИЕ ДЕЙСТВИЯ БОЙЦА (СИММЕТРИЧНО ДЛЯ ИГРОКА И ИИ) ====================
    private void executeActorTurn(BattleSession b, boolean isPlayer, AbilityDto ability) {
        String actorName = isPlayer ? b.getPlayerPokemonName() : "Дикий " + b.getEnemyPokemonName();
        int actorSpd = isPlayer ? b.getEffectivePlayerSpeed() : b.getEffectiveEnemySpeed();

        // Сброс неиспользованной защитной стойки с прошлого хода перед новым действием
        if (isPlayer) {
            b.setPlayerBlockPercent(0);
            b.setPlayerEvasionTurns(0);
        } else {
            b.setEnemyBlockPercent(0);
            b.setEnemyEvasionTurns(0);
        }

        // 1. Проверка контроля (Сон / Оглушение)
        int sleepTurns = isPlayer ? b.getPlayerSleepTurns() : b.getEnemySleepTurns();
        if (sleepTurns > 0) {
            if (isPlayer) b.setPlayerSleepTurns(sleepTurns - 1);
            else b.setEnemySleepTurns(sleepTurns - 1);
            b.appendLog(String.format("[SPD %d] %s крепко спит и пропускает ход!", actorSpd, actorName));
            return;
        }

        int stunTurns = isPlayer ? b.getPlayerStunTurns() : b.getEnemyStunTurns();
        if (stunTurns > 0) {
            if (isPlayer) b.setPlayerStunTurns(stunTurns - 1);
            else b.setEnemyStunTurns(stunTurns - 1);
            b.appendLog(String.format("[SPD %d] %s оглушен (сбит с ног) и пропускает ход!", actorSpd, actorName));
            return;
        }

        // 2. Установка перезарядки выбранной способности (+1, т.к. в конце раунда все кулдауны уменьшаются на 1)
        if (ability.maxCooldown() > 0) {
            if (isPlayer) b.setPlayerCooldown(ability.slot(), ability.maxCooldown() + 1);
            else b.setEnemyCooldown(ability.slot(), ability.maxCooldown() + 1);
        }

        // 3. Обработка защитных и вспомогательных приемов (target = SELF)
        if ("SELF".equalsIgnoreCase(ability.target())) {
            executeSelfUtilityOrDefense(b, isPlayer, actorName, actorSpd, ability);
            return;
        }

        // 4. Обработка атакующей способности (target = ENEMY)
        executeOffensiveAbility(b, isPlayer, actorName, actorSpd, ability);
    }

    private void executeSelfUtilityOrDefense(BattleSession b, boolean isPlayer, String actorName, int actorSpd, AbilityDto ability) {
        int maxHp = isPlayer ? b.getPlayerMaxHp() : b.getEnemyMaxHp();
        int curHp = isPlayer ? b.getPlayerCurrentHp() : b.getEnemyCurrentHp();
        StringBuilder details = new StringBuilder();

        // Жертва собственным HP (Вулканический раж Пихача)
        if (ability.selfDamagePercentMaxHp() > 0) {
            int sacrifice = Math.max(1, (int) Math.round(maxHp * (ability.selfDamagePercentMaxHp() / 100.0)));
            curHp = Math.max(1, curHp - sacrifice);
            if (isPlayer) b.setPlayerCurrentHp(curHp);
            else b.setEnemyCurrentHp(curHp);
            details.append(String.format("жертвует %d HP, ", sacrifice));
        }

        // Лечение (с проверкой < 25% HP для Синдерхута)
        if (ability.healPercentMaxHp() > 0) {
            int healPct = ability.healPercentMaxHp();
            if (ability.doubleHealLowHp() && (curHp * 4 <= maxHp)) {
                healPct *= 2;
                details.append("Второе дыхание (x2 хил при HP < 25%)! ");
            }
            int healAmount = Math.max(1, (int) Math.round(maxHp * (healPct / 100.0)));
            int newHp = Math.min(maxHp, curHp + healAmount);
            int actualHealed = newHp - curHp;
            if (isPlayer) b.setPlayerCurrentHp(newHp);
            else b.setEnemyCurrentHp(newHp);
            details.append(String.format("восстанавливает +%d HP (%d/%d HP), ", actualHealed, newHp, maxHp));
        }

        // Защитная стойка: Блок
        if (ability.blockPercent() > 0) {
            if (isPlayer) b.setPlayerBlockPercent(ability.blockPercent());
            else b.setEnemyBlockPercent(ability.blockPercent());
            details.append(String.format("ставит блок %d%%, ", ability.blockPercent()));
        }

        // Защитная стойка: Уворот 65%
        if (ability.evasionChancePercent() > 0) {
            if (isPlayer) b.setPlayerEvasionTurns(1);
            else b.setEnemyEvasionTurns(1);
            details.append(String.format("уходит в уворот (%d%% шанс промаха врага), ", ability.evasionChancePercent()));
        }

        // Применение основного и дополнительного эффектов
        applyAbilityEffect(b, isPlayer, ability.primaryEffect(), ability.primaryEffectChancePercent(), ability.primaryEffectDuration(), details);
        applyAbilityEffect(b, isPlayer, ability.secondaryEffect(), 100, ability.secondaryEffectDuration(), details);

        String effectText = details.length() > 2
                ? details.substring(0, details.length() - 2)
                : ability.description();

        b.appendLog(String.format("[SPD %d] %s использует «%s»: %s.", actorSpd, actorName, ability.name(), effectText));
    }

    private void executeOffensiveAbility(BattleSession b, boolean isPlayer, String actorName, int actorSpd, AbilityDto ability) {
        String targetName = isPlayer ? b.getEnemyPokemonName() : b.getPlayerPokemonName();

        // 1. Проверка точности и статуса Ослепления (BLIND: -35% к точности)
        double hitChance = ability.accuracy() / 100.0;
        int actorBlind = isPlayer ? b.getPlayerBlindTurns() : b.getEnemyBlindTurns();
        if (actorBlind > 0) {
            hitChance *= 0.65;
        }
        if (ThreadLocalRandom.current().nextDouble() > hitChance) {
            b.appendLog(String.format("[SPD %d] %s использует «%s», но промахивается (ослепление/точность)!",
                    actorSpd, actorName, ability.name()));
            return;
        }

        // 2. Проверка Уворота цели (EVASION: 65% шанс промаха)
        int targetEvasion = isPlayer ? b.getEnemyEvasionTurns() : b.getPlayerEvasionTurns();
        if (targetEvasion > 0) {
            if (isPlayer) b.setEnemyEvasionTurns(0);
            else b.setPlayerEvasionTurns(0);

            if (ThreadLocalRandom.current().nextInt(100) < 65) {
                b.appendLog(String.format("[SPD %d] %s атакует «%s», но %s ловко уворачивается (65%% уворот)!",
                        actorSpd, actorName, ability.name(), targetName));
                return;
            }
        }

        // 3. Проверка Ракушечного барьера (SHELL_BARRIER Выдрочки: 100% поглощение удара)
        int targetBarrier = isPlayer ? b.getEnemyShellBarrierTurns() : b.getPlayerShellBarrierTurns();
        if (targetBarrier > 0) {
            if (isPlayer) b.setEnemyShellBarrierTurns(0);
            else b.setPlayerShellBarrierTurns(0);
            b.appendLog(String.format("[SPD %d] %s использует «%s», но Ракушечный барьер (%s) полностью поглощает удар (0 урона)!",
                    actorSpd, actorName, ability.name(), targetName));
            return;
        }

        // 4. Расчет базовых параметров и модификаторов
        int rawAtk = isPlayer ? b.getPlayerAttack() : b.getEnemyAttack();
        int rawDef = isPlayer ? b.getEnemyDefense() : b.getPlayerDefense();
        String actorElement = isPlayer ? b.getPlayerElement() : b.getEnemyElement();
        String targetElement = isPlayer ? b.getEnemyElement() : b.getPlayerElement();
        String moveElement = (ability.element() == null || "NEUTRAL".equalsIgnoreCase(ability.element()))
                ? "NEUTRAL"
                : ability.element();

        double atkMod = (isPlayer ? b.getPlayerAtkUpTurns() : b.getEnemyAtkUpTurns()) > 0 ? 1.30 : 1.0;
        double defMod = (isPlayer ? b.getEnemyDefUpTurns() : b.getPlayerDefUpTurns()) > 0 ? 1.35 : 1.0;

        double elemMult = getElementMultiplier(moveElement, targetElement);
        boolean hasStab = ability.stab() || (!"NEUTRAL".equals(moveElement) && moveElement.equalsIgnoreCase(actorElement));
        double stabMult = hasStab ? STAB_MULTIPLIER : 1.0;

        // Водный заряд Сплэшикана (+35% к следующей водной атаке)
        double waterBoostMult = 1.0;
        int actorWaterBoost = isPlayer ? b.getPlayerWaterBoostTurns() : b.getEnemyWaterBoostTurns();
        if (actorWaterBoost > 0 && "WATER".equalsIgnoreCase(moveElement)) {
            waterBoostMult = 1.35;
            if (isPlayer) b.setPlayerWaterBoostTurns(0);
            else b.setEnemyWaterBoostTurns(0);
        }

        // Проверка критического удара (Анализ монокля или 10% базовый шанс) и защиты от крита
        boolean targetCritImmune = (isPlayer ? b.getEnemyCritImmuneTurns() : b.getPlayerCritImmuneTurns()) > 0;
        int guaranteedCrit = isPlayer ? b.getPlayerGuaranteedCritTurns() : b.getEnemyGuaranteedCritTurns();
        boolean isCrit = false;
        if (guaranteedCrit > 0) {
            if (isPlayer) b.setPlayerGuaranteedCritTurns(0);
            else b.setEnemyGuaranteedCritTurns(0);
            if (!targetCritImmune) {
                isCrit = true;
            }
        } else if (!targetCritImmune && ThreadLocalRandom.current().nextInt(100) < BASE_CRIT_CHANCE_PERCENT) {
            isCrit = true;
        }
        double critMult = isCrit ? CRIT_MULTIPLIER : 1.0;

        // Проверка блока цели и пробития блока (Вулканический раж Пихача)
        int targetBlockPct = isPlayer ? b.getEnemyBlockPercent() : b.getPlayerBlockPercent();
        boolean actorPiercesBlock = (isPlayer ? b.getPlayerRageTurns() : b.getEnemyRageTurns()) > 0;
        double blockMult = 1.0;
        String blockNote = "";
        if (targetBlockPct > 0) {
            if (isPlayer) b.setEnemyBlockPercent(0);
            else b.setPlayerBlockPercent(0);

            if (actorPiercesBlock) {
                blockNote = " [Блок пробит ражем!]";
            } else {
                blockMult = 1.0 - (targetBlockPct / 100.0);
                blockNote = String.format(" [Блок -%d%%]", targetBlockPct);
            }
        }

        double effectiveAtk = rawAtk * atkMod + ability.power() * 0.65;
        double effectiveDef = rawDef * defMod * 0.45;
        double baseFormula = Math.max(6.0, effectiveAtk - effectiveDef);

        int dmg = Math.max(1, (int) Math.round(
                baseFormula * elemMult * stabMult * waterBoostMult * critMult * blockMult
        ));

        if (isPlayer) {
            b.setEnemyCurrentHp(Math.max(0, b.getEnemyCurrentHp() - dmg));
        } else {
            b.setPlayerCurrentHp(Math.max(0, b.getPlayerCurrentHp() - dmg));
        }

        int targetCurHp = isPlayer ? b.getEnemyCurrentHp() : b.getPlayerCurrentHp();
        int targetMaxHp = isPlayer ? b.getEnemyMaxHp() : b.getPlayerMaxHp();

        StringBuilder tags = new StringBuilder();
        if (hasStab) tags.append(" STAB x1.25");
        if (elemMult != 1.0) tags.append(String.format(" Стихия x%.2f", elemMult));
        if (waterBoostMult > 1.0) tags.append(" Резервуар x1.35");
        if (isCrit) tags.append(" КРИТ x1.5!");
        tags.append(blockNote);

        // Наложение эффекта самой атаки (Горение, Яд, Оглушение, Замедление, Ослепление)
        StringBuilder statusNotes = new StringBuilder();
        applyAbilityEffect(b, isPlayer, ability.primaryEffect(), ability.primaryEffectChancePercent(), ability.primaryEffectDuration(), statusNotes);

        // Проверка заряда «Лавовое купание» Шаломандры (гарантированный поджог следующим ударом)
        int nextHitBurn = isPlayer ? b.getPlayerNextHitBurnTurns() : b.getEnemyNextHitBurnTurns();
        if (nextHitBurn > 0) {
            if (isPlayer) b.setPlayerNextHitBurnTurns(0);
            else b.setEnemyNextHitBurnTurns(0);
            applyAbilityEffect(b, isPlayer, StatusEffectType.BURN, 100, 2, statusNotes);
        }

        String statusSuffix = statusNotes.length() > 2
                ? " -> " + statusNotes.substring(0, statusNotes.length() - 2)
                : "";

        b.appendLog(String.format("[SPD %d] %s применяет «%s»%s: %d урона! (У %s %d/%d HP)%s",
                actorSpd, actorName, ability.name(),
                tags.isEmpty() ? "" : " [" + tags.toString().trim() + "]",
                dmg, targetName, targetCurHp, targetMaxHp, statusSuffix));

        // 5. Проверка Огненных шипов цели (THORNS Красной панды)
        int targetThorns = isPlayer ? b.getEnemyThornsTurns() : b.getPlayerThornsTurns();
        if (targetThorns > 0) {
            int actorMaxHp = isPlayer ? b.getPlayerMaxHp() : b.getEnemyMaxHp();
            int thornDmg = Math.max(1, (int) Math.round(actorMaxHp * 0.10));
            if (isPlayer) {
                b.setPlayerCurrentHp(Math.max(0, b.getPlayerCurrentHp() - thornDmg));
            } else {
                b.setEnemyCurrentHp(Math.max(0, b.getEnemyCurrentHp() - thornDmg));
            }
            b.appendLog(String.format("Огненные шипы (%s) обжигают %s в ответ на %d урона!",
                    targetName, actorName, thornDmg));
        }
    }

    private void applyAbilityEffect(BattleSession b, boolean actorIsPlayer, StatusEffectType effect,
                                    int chancePercent, int duration, StringBuilder logBuilder) {
        if (effect == null || effect == StatusEffectType.NONE || chancePercent <= 0) {
            return;
        }
        if (chancePercent < 100 && ThreadLocalRandom.current().nextInt(100) >= chancePercent) {
            return;
        }

        // +1 ход для баффов и периодических статусов, которые тикают в конце раунда
        int effectiveDur = duration + 1;
        boolean targetHasCcImmune = actorIsPlayer ? b.getEnemyCcImmuneTurns() > 0 : b.getPlayerCcImmuneTurns() > 0;

        switch (effect) {
            // Негативные статусы на противника
            case BURN -> {
                if (targetHasCcImmune) {
                    logBuilder.append("Иммунитет к контролю блокирует Горение, ");
                } else {
                    if (actorIsPlayer) b.setEnemyBurnTurns(effectiveDur);
                    else b.setPlayerBurnTurns(effectiveDur);
                    logBuilder.append("Горение (" + duration + " х.), ");
                }
            }
            case POISON -> {
                if (targetHasCcImmune) {
                    logBuilder.append("Иммунитет к контролю блокирует Яд, ");
                } else {
                    if (actorIsPlayer) b.setEnemyPoisonTurns(effectiveDur);
                    else b.setPlayerPoisonTurns(effectiveDur);
                    logBuilder.append("Яд (" + duration + " х.), ");
                }
            }
            case SLOW -> {
                if (targetHasCcImmune) {
                    logBuilder.append("Иммунитет блокирует Замедление, ");
                } else {
                    if (actorIsPlayer) b.setEnemySlowTurns(effectiveDur);
                    else b.setPlayerSlowTurns(effectiveDur);
                    logBuilder.append("Замедление -35% SPD (" + duration + " х.), ");
                }
            }
            case BLIND -> {
                if (targetHasCcImmune) {
                    logBuilder.append("Иммунитет блокирует Ослепление, ");
                } else {
                    if (actorIsPlayer) b.setEnemyBlindTurns(effectiveDur);
                    else b.setPlayerBlindTurns(effectiveDur);
                    logBuilder.append("Ослепление -35% точности (" + duration + " х.), ");
                }
            }
            case STUN -> {
                if (targetHasCcImmune) {
                    logBuilder.append("Иммунитет блокирует Оглушение, ");
                } else {
                    if (actorIsPlayer) b.setEnemyStunTurns(duration);
                    else b.setPlayerStunTurns(duration);
                    logBuilder.append("Сбивание с ног (Оглушение на 1 ход), ");
                }
            }
            case SLEEP -> {
                if (targetHasCcImmune) {
                    logBuilder.append("Иммунитет блокирует Сон, ");
                } else {
                    if (actorIsPlayer) b.setEnemySleepTurns(duration);
                    else b.setPlayerSleepTurns(duration);
                    logBuilder.append("Усыпление врага (Сон на 1 ход), ");
                }
            }
            // Положительные эффекты на себя
            case CLEANSE -> {
                if (actorIsPlayer) b.cleansePlayerDebuffs();
                else b.cleanseEnemyDebuffs();
                logBuilder.append("снимает все негативные статусы, ");
            }
            case REGEN -> {
                if (actorIsPlayer) b.setPlayerRegenTurns(effectiveDur);
                else b.setEnemyRegenTurns(effectiveDur);
                logBuilder.append("Регенерация +10% HP (" + duration + " х.), ");
            }
            case ATK_UP -> {
                if (actorIsPlayer) b.setPlayerAtkUpTurns(effectiveDur);
                else b.setEnemyAtkUpTurns(effectiveDur);
                logBuilder.append("Атака +30% (" + duration + " х.), ");
            }
            case DEF_UP -> {
                if (actorIsPlayer) b.setPlayerDefUpTurns(effectiveDur);
                else b.setEnemyDefUpTurns(effectiveDur);
                logBuilder.append("Защита +35% (" + duration + " х.), ");
            }
            case THORNS -> {
                if (actorIsPlayer) b.setPlayerThornsTurns(effectiveDur);
                else b.setEnemyThornsTurns(effectiveDur);
                logBuilder.append("Огненные шипы (" + duration + " х.), ");
            }
            case RAGE_SPEED_PIERCE -> {
                if (actorIsPlayer) b.setPlayerRageTurns(effectiveDur);
                else b.setEnemyRageTurns(effectiveDur);
                logBuilder.append("Скорость x2 и пробитие блока (" + duration + " х.), ");
            }
            case NEXT_HIT_BURN -> {
                if (actorIsPlayer) b.setPlayerNextHitBurnTurns(effectiveDur);
                else b.setEnemyNextHitBurnTurns(effectiveDur);
                logBuilder.append("следующий удар подожжет врага, ");
            }
            case WATER_BOOST -> {
                if (actorIsPlayer) b.setPlayerWaterBoostTurns(effectiveDur);
                else b.setEnemyWaterBoostTurns(effectiveDur);
                logBuilder.append("следующая водная атака +35% урона, ");
            }
            case GUARANTEED_CRIT -> {
                if (actorIsPlayer) b.setPlayerGuaranteedCritTurns(effectiveDur);
                else b.setEnemyGuaranteedCritTurns(effectiveDur);
                logBuilder.append("гарантированный крит на следующий удар, ");
            }
            case SHELL_BARRIER -> {
                if (actorIsPlayer) b.setPlayerShellBarrierTurns(1);
                else b.setEnemyShellBarrierTurns(1);
                logBuilder.append("активирован Ракушечный барьер (поглотит следующий удар), ");
            }
            case CRIT_IMMUNE -> {
                if (actorIsPlayer) b.setPlayerCritImmuneTurns(effectiveDur);
                else b.setEnemyCritImmuneTurns(effectiveDur);
                logBuilder.append("защита от критов (" + duration + " х.), ");
            }
            case CC_IMMUNE -> {
                if (actorIsPlayer) b.setPlayerCcImmuneTurns(effectiveDur);
                else b.setEnemyCcImmuneTurns(effectiveDur);
                logBuilder.append("иммунитет к контролю (" + duration + " х.), ");
            }
            default -> {}
        }
    }

    // ==================== КОНЕЦ РАУНДА: ТИКИ СТАТУСОВ И КУЛДАУНОВ ====================
    private void applyEndOfRoundTicks(BattleSession b) {
        // 1. Горение (8% MaxHP) и Яд (10% MaxHP) и Регенерация (+10% MaxHP) для Игрока
        if (b.getPlayerBurnTurns() > 0 && b.getPlayerCurrentHp() > 0) {
            int dmg = Math.max(1, (int) Math.round(b.getPlayerMaxHp() * 0.08));
            b.setPlayerCurrentHp(Math.max(0, b.getPlayerCurrentHp() - dmg));
            b.setPlayerBurnTurns(b.getPlayerBurnTurns() - 1);
            b.appendLog(String.format("[Статус] %s получает %d урона от Горения (%d/%d HP).",
                    b.getPlayerPokemonName(), dmg, b.getPlayerCurrentHp(), b.getPlayerMaxHp()));
        }
        if (b.getPlayerPoisonTurns() > 0 && b.getPlayerCurrentHp() > 0) {
            int dmg = Math.max(1, (int) Math.round(b.getPlayerMaxHp() * 0.10));
            b.setPlayerCurrentHp(Math.max(0, b.getPlayerCurrentHp() - dmg));
            b.setPlayerPoisonTurns(b.getPlayerPoisonTurns() - 1);
            b.appendLog(String.format("[Статус] %s получает %d урона от Яда (%d/%d HP).",
                    b.getPlayerPokemonName(), dmg, b.getPlayerCurrentHp(), b.getPlayerMaxHp()));
        }
        if (b.getPlayerRegenTurns() > 0 && b.getPlayerCurrentHp() > 0) {
            int heal = Math.max(1, (int) Math.round(b.getPlayerMaxHp() * 0.10));
            b.setPlayerCurrentHp(Math.min(b.getPlayerMaxHp(), b.getPlayerCurrentHp() + heal));
            b.setPlayerRegenTurns(b.getPlayerRegenTurns() - 1);
            b.appendLog(String.format("[Статус] %s восстанавливает +%d HP от Ароматного чая (%d/%d HP).",
                    b.getPlayerPokemonName(), heal, b.getPlayerCurrentHp(), b.getPlayerMaxHp()));
        }

        // 2. Горение, Яд и Регенерация для Врага
        if (b.getEnemyBurnTurns() > 0 && b.getEnemyCurrentHp() > 0) {
            int dmg = Math.max(1, (int) Math.round(b.getEnemyMaxHp() * 0.08));
            b.setEnemyCurrentHp(Math.max(0, b.getEnemyCurrentHp() - dmg));
            b.setEnemyBurnTurns(b.getEnemyBurnTurns() - 1);
            b.appendLog(String.format("[Статус] Дикий %s получает %d урона от Горения (%d/%d HP).",
                    b.getEnemyPokemonName(), dmg, b.getEnemyCurrentHp(), b.getEnemyMaxHp()));
        }
        if (b.getEnemyPoisonTurns() > 0 && b.getEnemyCurrentHp() > 0) {
            int dmg = Math.max(1, (int) Math.round(b.getEnemyMaxHp() * 0.10));
            b.setEnemyCurrentHp(Math.max(0, b.getEnemyCurrentHp() - dmg));
            b.setEnemyPoisonTurns(b.getEnemyPoisonTurns() - 1);
            b.appendLog(String.format("[Статус] Дикий %s получает %d урона от Яда (%d/%d HP).",
                    b.getEnemyPokemonName(), dmg, b.getEnemyCurrentHp(), b.getEnemyMaxHp()));
        }
        if (b.getEnemyRegenTurns() > 0 && b.getEnemyCurrentHp() > 0) {
            int heal = Math.max(1, (int) Math.round(b.getEnemyMaxHp() * 0.10));
            b.setEnemyCurrentHp(Math.min(b.getEnemyMaxHp(), b.getEnemyCurrentHp() + heal));
            b.setEnemyRegenTurns(b.getEnemyRegenTurns() - 1);
            b.appendLog(String.format("[Статус] Дикий %s восстанавливает +%d HP от Регенерации (%d/%d HP).",
                    b.getEnemyPokemonName(), heal, b.getEnemyCurrentHp(), b.getEnemyMaxHp()));
        }

        // 3. Уменьшение длительности остальных баффов и дебаффов
        if (b.getPlayerSlowTurns() > 0) b.setPlayerSlowTurns(b.getPlayerSlowTurns() - 1);
        if (b.getPlayerBlindTurns() > 0) b.setPlayerBlindTurns(b.getPlayerBlindTurns() - 1);
        if (b.getPlayerCritImmuneTurns() > 0) b.setPlayerCritImmuneTurns(b.getPlayerCritImmuneTurns() - 1);
        if (b.getPlayerCcImmuneTurns() > 0) b.setPlayerCcImmuneTurns(b.getPlayerCcImmuneTurns() - 1);
        if (b.getPlayerAtkUpTurns() > 0) b.setPlayerAtkUpTurns(b.getPlayerAtkUpTurns() - 1);
        if (b.getPlayerDefUpTurns() > 0) b.setPlayerDefUpTurns(b.getPlayerDefUpTurns() - 1);
        if (b.getPlayerThornsTurns() > 0) b.setPlayerThornsTurns(b.getPlayerThornsTurns() - 1);
        if (b.getPlayerRageTurns() > 0) b.setPlayerRageTurns(b.getPlayerRageTurns() - 1);

        if (b.getEnemySlowTurns() > 0) b.setEnemySlowTurns(b.getEnemySlowTurns() - 1);
        if (b.getEnemyBlindTurns() > 0) b.setEnemyBlindTurns(b.getEnemyBlindTurns() - 1);
        if (b.getEnemyCritImmuneTurns() > 0) b.setEnemyCritImmuneTurns(b.getEnemyCritImmuneTurns() - 1);
        if (b.getEnemyCcImmuneTurns() > 0) b.setEnemyCcImmuneTurns(b.getEnemyCcImmuneTurns() - 1);
        if (b.getEnemyAtkUpTurns() > 0) b.setEnemyAtkUpTurns(b.getEnemyAtkUpTurns() - 1);
        if (b.getEnemyDefUpTurns() > 0) b.setEnemyDefUpTurns(b.getEnemyDefUpTurns() - 1);
        if (b.getEnemyThornsTurns() > 0) b.setEnemyThornsTurns(b.getEnemyThornsTurns() - 1);
        if (b.getEnemyRageTurns() > 0) b.setEnemyRageTurns(b.getEnemyRageTurns() - 1);

        // 4. Уменьшение перезарядок способностей
        b.tickAllCooldowns();
    }

    // ==================== ДЕРЕВО РЕШЕНИЙ ИИ (AI DECISION TREE) ====================
    private int chooseEnemyAbilitySlot(BattleSession b) {
        List<AbilityDto> abilities = abilityCatalog.getAbilitiesForPokemon(b.getEnemySpeciesId(), b.getEnemyPokemonName());
        AbilityDto slot1 = abilities.get(0);
        AbilityDto slot2 = abilities.get(1);
        AbilityDto slot3 = abilities.get(2);
        AbilityDto slot4 = abilities.get(3);

        boolean cd2Ready = b.getEnemyCooldown(2) == 0;
        boolean cd3Ready = b.getEnemyCooldown(3) == 0;
        boolean cd4Ready = b.getEnemyCooldown(4) == 0;

        double enemyHpRatio = (double) b.getEnemyCurrentHp() / b.getEnemyMaxHp();

        // Приоритет 1: Добивание (Lethal Check)
        if (cd2Ready && estimateAttackDamage(b, false, slot2) >= b.getPlayerCurrentHp()) {
            return 2;
        }
        if (estimateAttackDamage(b, false, slot1) >= b.getPlayerCurrentHp()) {
            return 1;
        }

        // Приоритет 2: Экстренное лечение / Ракушечный барьер / Снятие опасных дебаффов
        if (cd4Ready) {
            boolean hasHealOrBarrier = slot4.healPercentMaxHp() > 0 || slot4.primaryEffect() == StatusEffectType.SHELL_BARRIER;
            boolean hasCleanse = slot4.primaryEffect() == StatusEffectType.CLEANSE || slot4.secondaryEffect() == StatusEffectType.CLEANSE;
            boolean enemyDebuffed = b.getEnemyBurnTurns() > 0 || b.getEnemyPoisonTurns() > 0 || b.getEnemyBlindTurns() > 0;

            if (hasHealOrBarrier && enemyHpRatio <= 0.45) {
                return 4;
            }
            if (hasCleanse && enemyDebuffed && enemyHpRatio <= 0.70) {
                return 4;
            }
        }

        // Приоритет 3: Защитная стойка (Блок / Уворот) против забаффанного игрока или стихийной контры
        if (cd3Ready && enemyHpRatio <= 0.65) {
            boolean playerBuffed = b.getPlayerAtkUpTurns() > 0
                    || b.getPlayerGuaranteedCritTurns() > 0
                    || b.getPlayerWaterBoostTurns() > 0
                    || b.getPlayerRageTurns() > 0;
            boolean playerHasElementAdvantage = getElementMultiplier(b.getPlayerElement(), b.getEnemyElement()) > 1.0;

            if ((playerBuffed || playerHasElementAdvantage) && ThreadLocalRandom.current().nextInt(100) < 65) {
                return 3;
            }
        }

        // Приоритет 4: Тактический бафф / контроль из Слота 4, если HP комфортное
        if (cd4Ready && enemyHpRatio > 0.50 && slot4.healPercentMaxHp() == 0) {
            boolean alreadyBuffed = b.getEnemyAtkUpTurns() > 0
                    || b.getEnemyRageTurns() > 0
                    || b.getEnemyGuaranteedCritTurns() > 0
                    || b.getEnemyShellBarrierTurns() > 0;
            if (!alreadyBuffed && ThreadLocalRandom.current().nextInt(100) < 75) {
                return 4;
            }
        }

        // Приоритет 5: Особая атака (STAB + наложение статуса) по откату
        if (cd2Ready) {
            return 2;
        }

        // Приоритет 6: Базовая атака (Слот 1)
        return 1;
    }

    private int estimateAttackDamage(BattleSession b, boolean actorIsPlayer, AbilityDto ability) {
        int rawAtk = actorIsPlayer ? b.getPlayerAttack() : b.getEnemyAttack();
        int rawDef = actorIsPlayer ? b.getEnemyDefense() : b.getPlayerDefense();
        String actorElem = actorIsPlayer ? b.getPlayerElement() : b.getEnemyElement();
        String targetElem = actorIsPlayer ? b.getEnemyElement() : b.getPlayerElement();
        String moveElem = (ability.element() == null || "NEUTRAL".equalsIgnoreCase(ability.element()))
                ? "NEUTRAL"
                : ability.element();

        double mult = getElementMultiplier(moveElem, targetElem);
        if (ability.stab() || (!"NEUTRAL".equals(moveElem) && moveElem.equalsIgnoreCase(actorElem))) {
            mult *= STAB_MULTIPLIER;
        }
        double base = Math.max(6.0, (rawAtk + ability.power() * 0.65) - rawDef * 0.45);
        return Math.max(1, (int) Math.round(base * mult));
    }

    private int normalizeSlot(Integer slot) {
        if (slot == null || slot < 1 || slot > 4) {
            return 1;
        }
        return slot;
    }

    private void verifyBattleOwnership(BattleSession b, Long authUserId) {
        if (authUserId != null && !b.getUserId().equals(authUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ к чужому бою запрещен!");
        }
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    private void handlePlayerDefeat(BattleSession b, boolean pokeballUsed) {
        b.setPlayerCurrentHp(0);
        b.setStatus(BattleStatus.LOST);
        b.appendLog("Ваш " + b.getPlayerPokemonName() + " потерял сознание! +10 XP.");
        giveXpReward(b.getUserId(), 10, pokeballUsed);
    }

    public double getElementMultiplier(String atk, String def) {
        if (atk == null || def == null || atk.equals(def) || atk.equals("NEUTRAL") || def.equals("NEUTRAL")) {
            return 1.0;
        }
        return ADVANTAGE_PAIRS.contains(atk + "->" + def) ? 1.5 : 0.67;
    }

    private OwnedPokemonDto chooseFighterFromParty(UserProfileDto user, Integer slot) {
        List<OwnedPokemonDto> party = user.party();
        if (party == null || party.isEmpty()) {
            return new OwnedPokemonDto(0L, 2L, "Красная панда", "FIRE", "Средний ДПС", 70, 35, 20, 25, 1);
        }
        if (slot != null) {
            return party.stream().filter(p -> slot.equals(p.partySlot())).findFirst().orElse(party.get(0));
        }
        return party.get(0);
    }

    private UserProfileDto fetchUser(Long userId) {
        try {
            return userClient.getUserProfile(userId);
        } catch (Exception t) {
            log.warn("user-service недоступен ({}). Используется Fallback-профиль!", t.getMessage());
            List<OwnedPokemonDto> backup = List.of(
                    new OwnedPokemonDto(1L, 2L, "Красная панда", "FIRE", "Средний ДПС", 70, 35, 20, 25, 1)
            );
            return new UserProfileDto(userId, "trainer@evogrid.local", 1, 0, 10, 1, 55.7558, 37.6173, backup, backup);
        }
    }

    private WorldSpawnDto fetchSpawn(Long spawnId) {
        try {
            return worldClient.getSpawnById(spawnId);
        } catch (Exception t) {
            log.warn("world-service недоступен ({}). Используется Fallback-спавн!", t.getMessage());
            return new WorldSpawnDto(spawnId, 1L, "Крысюк", "NEUTRAL", "Базовый крип",
                    75, 75, 25, 25, 25, 55.7558, 37.6173, true);
        }
    }

    private void safeDeactivateSpawn(Long spawnId) {
        try {
            worldClient.deactivateSpawn(spawnId);
        } catch (Exception t) {
            log.warn("Не удалось деактивировать спавн {} (world-service недоступен)", spawnId);
        }
    }

    private void giveXpReward(Long userId, int xp, boolean pokeballUsed) {
        safeApplyReward(userId, new BattleRewardRequest(xp, pokeballUsed, false, null, null, null, null, 0, 0, 0, 0));
    }

    private void safeApplyReward(Long userId, BattleRewardRequest reward) {
        try {
            userClient.applyBattleReward(userId, reward);
        } catch (Exception t) {
            log.warn("Не удалось начислить награду игроку {} (user-service недоступен)", userId);
        }
    }

    private BattleSession findBattleOrThrow(Long battleId) {
        return battleRepository.findById(battleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Бой не найден: " + battleId));
    }

    private BattleStateDto toDto(BattleSession b) {
        List<String> logs = b.getCombatLog() != null ? Arrays.asList(b.getCombatLog().split("\n")) : List.of();

        List<AbilityDto> playerAbilities = abilityCatalog
                .getAbilitiesForPokemon(b.getPlayerSpeciesId(), b.getPlayerPokemonName())
                .stream()
                .map(a -> a.withCurrentCooldown(b.getPlayerCooldown(a.slot())))
                .toList();

        List<AbilityDto> enemyAbilities = abilityCatalog
                .getAbilitiesForPokemon(b.getEnemySpeciesId(), b.getEnemyPokemonName())
                .stream()
                .map(a -> a.withCurrentCooldown(b.getEnemyCooldown(a.slot())))
                .toList();

        return new BattleStateDto(
                b.getId(), b.getUserId(), b.getSpawnId(), b.getTurnNumber(), b.getStatus(),
                b.getPlayerPokemonName(), b.getPlayerElement(), b.getPlayerCurrentHp(), b.getPlayerMaxHp(),
                b.getPlayerAttack(), b.getPlayerDefense(), b.getPlayerSpeed(),
                b.getEnemyPokemonName(), b.getEnemyElement(), b.getEnemyArchetype(),
                b.getEnemyCurrentHp(), b.getEnemyMaxHp(), b.getEnemyAttack(), b.getEnemyDefense(), b.getEnemySpeed(),
                logs,
                b.getEffectivePlayerSpeed(),
                b.getEffectiveEnemySpeed(),
                b.getPlayerStatusBadges(),
                b.getEnemyStatusBadges(),
                playerAbilities,
                enemyAbilities
        );
    }
}