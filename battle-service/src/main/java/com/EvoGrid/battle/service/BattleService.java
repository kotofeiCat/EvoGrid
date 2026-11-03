package com.EvoGrid.battle.service;

import com.EvoGrid.battle.client.UserClient;
import com.EvoGrid.battle.client.WorldClient;
import com.EvoGrid.battle.dto.*;
import com.EvoGrid.battle.model.BattleSession;
import com.EvoGrid.battle.model.BattleStatus;
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

    private static final Set<String> ADVANTAGE_PAIRS = Set.of(
            "FIRE->FOREST", "FOREST->WATER", "WATER->FIRE"
    );

    private final BattleSessionRepository battleRepository;
    private final UserClient userClient;
    private final WorldClient worldClient;

    public BattleService(BattleSessionRepository battleRepository,
                         UserClient userClient,
                         WorldClient worldClient) {
        this.battleRepository = battleRepository;
        this.userClient = userClient;
        this.worldClient = worldClient;
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
                user.id(), spawn.spawnId(),
                fighter.name(), fighter.element(), fighter.maxHp(), fighter.attack(), fighter.defense(), fighter.speed(),
                spawn.speciesId(), spawn.name(), spawn.element(), spawn.archetype(),
                spawn.maxHp(), spawn.attack(), spawn.defense(), spawn.speed(),
                startLog
        ));

        return toDto(session);
    }

    @Transactional
    public BattleStateDto attackTurn(Long battleId, Long authUserId) {
        BattleSession b = findBattleOrThrow(battleId);
        verifyBattleOwnership(b, authUserId);

        if (b.getStatus() != BattleStatus.IN_PROGRESS) {
            return toDto(b);
        }

        b.appendLog("--- Ход " + b.getTurnNumber() + " ---");

        if (b.getPlayerSpeed() >= b.getEnemySpeed()) {
            executePlayerStrike(b);
            if (b.getEnemyCurrentHp() > 0) executeEnemyStrike(b);
        } else {
            executeEnemyStrike(b);
            if (b.getPlayerCurrentHp() > 0) executePlayerStrike(b);
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
            executeEnemyStrike(b);
            if (b.getPlayerCurrentHp() <= 0) {
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

    private void executePlayerStrike(BattleSession b) {
        double mult = getElementMultiplier(b.getPlayerElement(), b.getEnemyElement());
        int dmg = calculateDamage(b.getPlayerAttack(), b.getEnemyDefense(), mult);
        b.setEnemyCurrentHp(Math.max(0, b.getEnemyCurrentHp() - dmg));
        b.appendLog(String.format("[SPD %d] %s атакует (%s -> %s, x%.2f) на %d урона (У врага %d/%d HP)",
                b.getPlayerSpeed(), b.getPlayerPokemonName(), b.getPlayerElement(), b.getEnemyElement(),
                mult, dmg, b.getEnemyCurrentHp(), b.getEnemyMaxHp()));
    }

    private void executeEnemyStrike(BattleSession b) {
        double mult = getElementMultiplier(b.getEnemyElement(), b.getPlayerElement());
        int dmg = calculateDamage(b.getEnemyAttack(), b.getPlayerDefense(), mult);
        b.setPlayerCurrentHp(Math.max(0, b.getPlayerCurrentHp() - dmg));
        b.appendLog(String.format("[SPD %d] Дикий %s контратакует (%s -> %s, x%.2f) на %d урона (У вас %d/%d HP)",
                b.getEnemySpeed(), b.getEnemyPokemonName(), b.getEnemyElement(), b.getPlayerElement(),
                mult, dmg, b.getPlayerCurrentHp(), b.getPlayerMaxHp()));
    }

    public double getElementMultiplier(String atk, String def) {
        if (atk == null || def == null || atk.equals(def) || atk.equals("NEUTRAL") || def.equals("NEUTRAL")) {
            return 1.0;
        }
        return ADVANTAGE_PAIRS.contains(atk + "->" + def) ? 1.5 : 0.67;
    }

    private int calculateDamage(int attack, int defense, double mult) {
        return Math.max(1, (int) Math.round(Math.max(6.0, attack - defense * 0.45) * mult));
    }

    private OwnedPokemonDto chooseFighterFromParty(UserProfileDto user, Integer slot) {
        List<OwnedPokemonDto> party = user.party();
        if (party == null || party.isEmpty()) {
            return new OwnedPokemonDto(0L, 2L, "Резервный Лис", "FIRE", "Дамагер", 70, 35, 20, 25, 1);
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
                    new OwnedPokemonDto(1L, 2L, "Пиролис (Fallback)", "FIRE", "Дамагер", 70, 35, 20, 25, 1)
            );
            return new UserProfileDto(userId, "trainer@evogrid.local", 1, 0, 10, 1, 55.7558, 37.6173, backup, backup);
        }
    }

    private WorldSpawnDto fetchSpawn(Long spawnId) {
        try {
            return worldClient.getSpawnById(spawnId);
        } catch (Exception t) {
            log.warn("world-service недоступен ({}). Используется Fallback-спавн!", t.getMessage());
            return new WorldSpawnDto(spawnId, 1L, "Эквилибр (Fallback)", "NEUTRAL", "Баланс",
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
        return new BattleStateDto(
                b.getId(), b.getUserId(), b.getSpawnId(), b.getTurnNumber(), b.getStatus(),
                b.getPlayerPokemonName(), b.getPlayerElement(), b.getPlayerCurrentHp(), b.getPlayerMaxHp(),
                b.getPlayerAttack(), b.getPlayerDefense(), b.getPlayerSpeed(),
                b.getEnemyPokemonName(), b.getEnemyElement(), b.getEnemyArchetype(),
                b.getEnemyCurrentHp(), b.getEnemyMaxHp(), b.getEnemyAttack(), b.getEnemyDefense(), b.getEnemySpeed(),
                logs
        );
    }
}