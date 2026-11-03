package com.EvoGrid.battle.service;

import com.EvoGrid.battle.client.UserClient;
import com.EvoGrid.battle.client.WorldClient;
import com.EvoGrid.battle.dto.*;
import com.EvoGrid.battle.model.BattleSession;
import com.EvoGrid.battle.model.BattleStatus;
import com.EvoGrid.battle.model.Combatant;
import com.EvoGrid.battle.repository.BattleSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Сценарии боя: начать, сходить способностью, бросить покебол, истечение времени хода.
 * Сами правила — в {@link BattleEngine}, выбор врага — в {@link EnemyAi}.
 */
@Service
public class BattleService {

    private static final Logger log = LoggerFactory.getLogger(BattleService.class);

    private static final double MAX_BATTLE_DISTANCE_METERS = 100.0; // радиус 70 м + запас на погрешность GPS
    private static final int XP_WIN = 35;
    private static final int XP_CATCH = 50;
    private static final int XP_LOSS = 10;

    private final BattleSessionRepository repository;
    private final UserClient userClient;
    private final WorldClient worldClient;
    private final AbilityCatalog catalog;
    private final BattleEngine engine;
    private final EnemyAi enemyAi;
    private final Duration turnTime;

    public BattleService(BattleSessionRepository repository,
                         UserClient userClient,
                         WorldClient worldClient,
                         AbilityCatalog catalog,
                         BattleEngine engine,
                         EnemyAi enemyAi,
                         @Value("${battle.turn-seconds:30}") int turnSeconds) {
        this.repository = repository;
        this.userClient = userClient;
        this.worldClient = worldClient;
        this.catalog = catalog;
        this.engine = engine;
        this.enemyAi = enemyAi;
        this.turnTime = Duration.ofSeconds(Math.max(5, turnSeconds));
    }

    // ==================== СЦЕНАРИИ ====================

    @Transactional
    public BattleStateDto start(StartBattleRequest req, Long authUserId) {
        Long userId = authUserId != null ? authUserId : req.userId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }

        UserProfileDto user = fetchUser(userId);
        WorldSpawnDto spawn = fetchSpawn(req.spawnId());
        if (!spawn.active()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Этот покемон уже пойман или скрылся.");
        }

        double distance = distanceMeters(user.latitude(), user.longitude(), spawn.latitude(), spawn.longitude());
        if (distance > MAX_BATTLE_DISTANCE_METERS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Покемон слишком далеко (" + Math.round(distance) + " м). Подойдите ближе.");
        }

        OwnedPokemonDto f = chooseFighter(user, req.partySlot());
        Combatant player = new Combatant(f.speciesId(), f.name(), f.element(), f.archetype(),
                f.maxHp(), f.attack(), f.defense(), f.speed());
        Combatant enemy = new Combatant(spawn.speciesId(), spawn.name(), spawn.element(), spawn.archetype(),
                spawn.maxHp(), spawn.attack(), spawn.defense(), spawn.speed());

        BattleSession b = new BattleSession(user.id(), spawn.spawnId(), player, enemy,
                String.format("Бой начался: %s против дикого %s.", player.name, enemy.name),
                deadlineFromNow());
        return toDto(save(b));
    }

    /** Игрок выбрал способность (слот 1..4). */
    @Transactional
    public BattleStateDto useAbility(Long battleId, Long authUserId, Integer requestedSlot) {
        BattleSession b = load(battleId, authUserId);
        if (!b.inProgress()) return toDto(b);

        Combatant player = b.player();
        int slot = requestedSlot == null || requestedSlot < 1 || requestedSlot > 4 ? 1 : requestedSlot;
        AbilityDto ability = catalog.getAbilityBySlot(player.speciesId, player.name, slot);

        if (!player.skipsTurn() && player.cooldown(slot) > 0) {
            b.log("«" + ability.name() + "» ещё перезаряжается — используется обычная атака.");
            ability = catalog.getAbilityBySlot(player.speciesId, player.name, 1);
        }

        engine.playRound(b, ability, enemyAi.chooseAbility(b));
        afterRound(b);
        return toDto(save(b));
    }

    /**
     * Время на выбор вышло (или игрок спит/оглушён и пропускает ход).
     * Ход разыгрывается автоматически: обычная атака или пропуск хода.
     */
    @Transactional
    public BattleStateDto timeout(Long battleId, Long authUserId) {
        BattleSession b = load(battleId, authUserId);
        if (!b.inProgress()) return toDto(b);

        Combatant player = b.player();
        boolean expired = b.getTurnDeadline() == null || !Instant.now().isBefore(b.getTurnDeadline().minusSeconds(1));
        if (!expired && !player.skipsTurn()) {
            return toDto(b); // время ещё есть — ничего не делаем
        }

        if (!player.skipsTurn()) {
            b.log("Время на выбор вышло — автоматически используется обычная атака.");
        }
        engine.playRound(b, catalog.getAbilityBySlot(player.speciesId, player.name, 1), enemyAi.chooseAbility(b));
        afterRound(b);
        return toDto(save(b));
    }

    @Transactional
    public BattleStateDto throwPokeball(Long battleId, Long authUserId) {
        BattleSession b = load(battleId, authUserId);
        if (!b.inProgress()) return toDto(b);

        UserProfileDto user = fetchUser(b.getUserId());
        if (user.pokeballs() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Покеболы закончились.");
        }

        Combatant enemy = b.enemy();
        double chance = Math.min(0.95, 0.35 + (1.0 - enemy.hpRatio()) * 0.65);
        b.log(String.format("— Ход %d — Бросок покебола (шанс %d%%)…", b.getTurnNumber(), Math.round(chance * 100)));

        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            b.finish(BattleStatus.CAPTURED);
            b.log(enemy.name + " пойман! +" + XP_CATCH + " XP.");
            deactivateSpawn(b.getSpawnId());
            reward(b.getUserId(), new BattleRewardRequest(XP_CATCH, true, true,
                    enemy.speciesId, enemy.name, enemy.element, enemy.archetype,
                    enemy.maxHp, enemy.attack, enemy.defense, enemy.speed));
            return toDto(save(b));
        }

        b.log(enemy.name + " вырвался из покебола!");
        reward(b.getUserId(), xpOnly(0, true));
        engine.enemyOnlyRound(b, enemyAi.chooseAbility(b));
        afterRound(b);
        return toDto(save(b));
    }

    @Transactional(readOnly = true)
    public BattleStateDto get(Long battleId, Long authUserId) {
        return toDto(load(battleId, authUserId));
    }

    // ==================== ИТОГИ РАУНДА ====================

    private void afterRound(BattleSession b) {
        Combatant player = b.player();
        Combatant enemy = b.enemy();

        if (!enemy.alive()) {
            b.finish(BattleStatus.WON_BY_KO);
            b.log("Дикий " + enemy.name + " повержен! +" + XP_WIN + " XP.");
            deactivateSpawn(b.getSpawnId());
            reward(b.getUserId(), xpOnly(XP_WIN, false));
        } else if (!player.alive()) {
            b.finish(BattleStatus.LOST);
            b.log(player.name + " без сил. +" + XP_LOSS + " XP за опыт.");
            reward(b.getUserId(), xpOnly(XP_LOSS, false));
        } else {
            b.nextTurn(deadlineFromNow());
            if (player.skipsTurn()) {
                b.log(player.name + " не может действовать в этом ходу.");
            }
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНОЕ ====================

    private Instant deadlineFromNow() {
        return Instant.now().plus(turnTime);
    }

    private BattleSession save(BattleSession b) {
        b.syncState();
        return repository.save(b);
    }

    private BattleSession load(Long battleId, Long authUserId) {
        BattleSession b = repository.findById(battleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Бой не найден: " + battleId));
        if (authUserId != null && !b.getUserId().equals(authUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Это чужой бой.");
        }
        return b;
    }

    private OwnedPokemonDto chooseFighter(UserProfileDto user, Integer slot) {
        List<OwnedPokemonDto> party = user.party();
        if (party == null || party.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "В отряде нет покемонов. Назначьте бойца в профиле.");
        }
        return party.stream()
                .filter(p -> slot != null && slot.equals(p.partySlot()))
                .findFirst()
                .orElse(party.get(0));
    }

    private UserProfileDto fetchUser(Long userId) {
        try {
            return userClient.getUserProfile(userId);
        } catch (Exception e) {
            log.warn("user-service недоступен: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Сервис профилей недоступен, попробуйте позже.");
        }
    }

    private WorldSpawnDto fetchSpawn(Long spawnId) {
        try {
            return worldClient.getSpawnById(spawnId);
        } catch (Exception e) {
            log.warn("world-service недоступен: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Сервис карты недоступен, попробуйте позже.");
        }
    }

    private void deactivateSpawn(Long spawnId) {
        try {
            worldClient.deactivateSpawn(spawnId);
        } catch (Exception e) {
            log.warn("Не удалось убрать спавн {}: {}", spawnId, e.getMessage());
        }
    }

    private static BattleRewardRequest xpOnly(int xp, boolean pokeballUsed) {
        return new BattleRewardRequest(xp, pokeballUsed, false, null, null, null, null, 0, 0, 0, 0);
    }

    private void reward(Long userId, BattleRewardRequest reward) {
        try {
            userClient.applyBattleReward(userId, reward);
        } catch (Exception e) {
            log.warn("Не удалось начислить награду игроку {}: {}", userId, e.getMessage());
        }
    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371000.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private BattleStateDto toDto(BattleSession b) {
        Combatant p = b.player();
        Combatant e = b.enemy();

        List<AbilityDto> playerAbilities = catalog.getAbilitiesForPokemon(p.speciesId, p.name).stream()
                .map(a -> a.withCurrentCooldown(p.cooldown(a.slot())))
                .toList();
        List<AbilityDto> enemyAbilities = catalog.getAbilitiesForPokemon(e.speciesId, e.name).stream()
                .map(a -> a.withCurrentCooldown(e.cooldown(a.slot())))
                .toList();

        long deadlineMs = b.getTurnDeadline() == null ? 0 : b.getTurnDeadline().toEpochMilli();
        int secondsLeft = b.getTurnDeadline() == null ? 0
                : (int) Math.max(0, Duration.between(Instant.now(), b.getTurnDeadline()).toSeconds());

        return new BattleStateDto(
                b.getId(), b.getUserId(), b.getSpawnId(), b.getTurnNumber(), b.getStatus(),
                p.name, p.element, p.hp, p.maxHp, p.attack, p.defense, p.speed,
                e.name, e.element, e.archetype, e.hp, e.maxHp, e.attack, e.defense, e.speed,
                b.logLines(),
                p.effectiveSpeed(), e.effectiveSpeed(),
                p.statusBadges(), e.statusBadges(),
                playerAbilities, enemyAbilities,
                deadlineMs, secondsLeft, (int) turnTime.toSeconds(),
                b.inProgress() && p.skipsTurn(),
                p.speciesId, e.speciesId
        );
    }
}
