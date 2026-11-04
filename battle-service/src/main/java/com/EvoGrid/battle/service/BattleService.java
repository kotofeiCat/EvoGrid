package com.EvoGrid.battle.service;

import com.EvoGrid.battle.engine.BattleEngine;
import com.EvoGrid.battle.engine.CatchRules;
import com.EvoGrid.battle.engine.Combatant;
import com.EvoGrid.battle.engine.EnemyAi;
import com.EvoGrid.battle.remote.AbilityBook;
import com.EvoGrid.battle.remote.Bag;
import com.EvoGrid.battle.remote.Players;
import com.EvoGrid.battle.remote.Spawns;
import com.EvoGrid.battle.session.BattleSession;
import com.EvoGrid.battle.session.BattleSessionRepository;
import com.EvoGrid.battle.session.BattleStatus;
import com.EvoGrid.battle.web.BattleStateDto;
import com.EvoGrid.common.dto.AbilityDto;
import com.EvoGrid.common.dto.ItemDto;
import com.EvoGrid.common.dto.OwnedPokemonDto;
import com.EvoGrid.common.dto.UserProfileDto;
import com.EvoGrid.common.dto.WorldSpawnDto;
import com.EvoGrid.common.game.ItemKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Сценарии боя: начать, сходить способностью, сменить бойца, применить предмет, бросить сферу,
 * пропустить ход по таймеру. Правила — в {@link BattleEngine}, решения врага — в {@link EnemyAi},
 * награды — в {@link BattleRewards}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleService {

    public static final String DEFAULT_SPHERE = "SPHERE_BASIC";

    private final BattleSessionRepository repository;
    private final Players players;
    private final Spawns spawns;
    private final Bag bag;
    private final AbilityBook abilityBook;
    private final BattleEngine engine;
    private final EnemyAi enemyAi;
    private final BattleRewards rewards;
    private final BattleSettings settings;

    @Transactional
    public BattleStateDto start(Long userId, Long spawnId, Integer partySlot) {
        var unfinished = repository.findFirstByUserIdAndSpawnIdAndStatus(userId, spawnId, BattleStatus.IN_PROGRESS)
                .filter(BattleService::isPlayable);
        if (unfinished.isPresent()) {
            // Игрок перезагрузил страницу или нажал «В бой» второй раз — продолжаем тот же бой.
            return toDto(unfinished.get());
        }

        UserProfileDto user = players.profile(userId);
        List<OwnedPokemonDto> party = user.party();
        if (party == null || party.isEmpty()) {
            throw badRequest("В отряде нет эвонов. Назначьте бойцов в профиле.");
        }
        if (party.stream().allMatch(OwnedPokemonDto::fainted)) {
            throw badRequest("Все эвоны отряда без сил. Вылечите их в сумке или подождите — силы восстанавливаются со временем.");
        }

        WorldSpawnDto spawn = spawns.reserve(spawnId, userId);
        double distance = Geo.distanceMeters(user.latitude(), user.longitude(), spawn.latitude(), spawn.longitude());
        if (distance > settings.maxDistanceMeters()) {
            throw badRequest("Эвон слишком далеко (" + Math.round(distance) + " м). Подойдите ближе.");
        }

        List<Combatant> team = party.stream()
                .map(p -> new Combatant(p.id(), p.speciesId(), p.name(), p.element(), p.archetype(),
                        p.maxHp(), p.currentHp(), p.attack(), p.defense(), p.speed(),
                        abilityBook.forSpecies(p.speciesId())))
                .toList();
        int active = firstFighter(party, partySlot != null ? partySlot : user.activePartySlot());
        Combatant enemy = new Combatant(spawn.speciesId(), spawn.name(), spawn.element(), spawn.archetype(),
                spawn.maxHp(), spawn.attack(), spawn.defense(), spawn.speed(),
                abilityBook.forSpecies(spawn.speciesId()));

        BattleSession battle = save(new BattleSession(userId, spawnId, team, active, enemy,
                "Бой начался: " + team.get(active).name + " против дикого " + enemy.name + ".", nextDeadline()));
        log.info("Бой {} начат: игрок {} ({} в отряде) против {} (спавн {})",
                battle.getId(), userId, team.size(), enemy.name, spawnId);
        return toDto(battle);
    }

    /** Игрок выбрал способность 1–4. Если она на перезарядке — используется обычная атака. */
    @Transactional
    public BattleStateDto useAbility(Long battleId, Long userId, int slot) {
        BattleSession battle = load(battleId, userId);
        if (!battle.inProgress()) {
            return toDto(battle);
        }
        Combatant player = battle.player();
        AbilityDto ability = player.ability(slot);
        if (!player.skipsTurn() && player.cooldown(slot) > 0) {
            battle.log("«" + ability.name() + "» ещё перезаряжается — используется обычная атака.");
            ability = player.ability(1);
        }
        engine.playRound(battle, ability, enemyAi.chooseAbility(battle));
        return finishRound(battle);
    }

    /** Время хода вышло, или боец спит/оглушён: ход разыгрывается автоматически. */
    @Transactional
    public BattleStateDto timeout(Long battleId, Long userId) {
        BattleSession battle = load(battleId, userId);
        if (!battle.inProgress()) {
            return toDto(battle);
        }
        Combatant player = battle.player();
        if (!player.skipsTurn()) {
            boolean timeIsUp = battle.getTurnDeadline() == null
                    || !Instant.now().isBefore(battle.getTurnDeadline().minusSeconds(1));
            if (!timeIsUp) {
                return toDto(battle);
            }
            battle.log("Время на выбор вышло — используется обычная атака.");
        }
        engine.playRound(battle, player.ability(1), enemyAi.chooseAbility(battle));
        return finishRound(battle);
    }

    /** Отозвать бойца и выпустить другого из отряда. Тратит ход: противник успевает ударить. */
    @Transactional
    public BattleStateDto switchFighter(Long battleId, Long userId, int index) {
        BattleSession battle = load(battleId, userId);
        if (!battle.inProgress()) {
            return toDto(battle);
        }
        Combatant target = fighter(battle, index);
        if (index == battle.getActiveIndex()) {
            throw badRequest(target.name + " уже на поле");
        }
        if (!target.alive()) {
            throw badRequest(target.name + " без сил и не может сражаться");
        }

        battle.log(String.format("— Ход %d — %s возвращается. В бой выходит %s!",
                battle.getTurnNumber(), battle.player().name, target.name));
        battle.switchTo(index);
        engine.enemyOnlyRound(battle, enemyAi.chooseAbility(battle));
        return finishRound(battle);
    }

    /** Лечилка или оживление на любого бойца отряда. Тратит ход. */
    @Transactional
    public BattleStateDto useItem(Long battleId, Long userId, String itemCode, Integer targetIndex) {
        BattleSession battle = load(battleId, userId);
        if (!battle.inProgress()) {
            return toDto(battle);
        }
        ItemDto item = bag.item(itemCode);
        Combatant target = fighter(battle, targetIndex != null ? targetIndex : battle.getActiveIndex());
        checkItemApplies(item, target);

        bag.consume(userId, operationId(battle), itemCode);
        String effect = switch (item.kind()) {
            case HEAL -> {
                int healed = target.heal(item.fullHeal() ? target.maxHp : item.healHp());
                if (item.fullHeal()) target.cleanseDebuffs();
                yield String.format("+%d HP (%d/%d)%s", healed, target.hp, target.maxHp,
                        item.fullHeal() ? ", статусы сняты" : "");
            }
            case REVIVE -> String.format("снова в строю с %d HP", target.revive(item.revivePercent()));
            case CAPTURE -> throw new IllegalStateException("сферы проверяются выше");
        };
        battle.log(String.format("— Ход %d — «%s» на %s: %s.", battle.getTurnNumber(), item.name(), target.name, effect));

        engine.enemyOnlyRound(battle, enemyAi.chooseAbility(battle));
        return finishRound(battle);
    }

    /** Бросить сферу. Не получилось — противник ходит. */
    @Transactional
    public BattleStateDto throwSphere(Long battleId, Long userId, String sphereCode) {
        BattleSession battle = load(battleId, userId);
        if (!battle.inProgress()) {
            return toDto(battle);
        }
        ItemDto sphere = bag.item(sphereCode != null ? sphereCode : DEFAULT_SPHERE);
        if (sphere.kind() != ItemKind.CAPTURE) {
            throw badRequest("«" + sphere.name() + "» — не сфера");
        }
        bag.consume(userId, operationId(battle), sphere.code());

        Combatant enemy = battle.enemy();
        double chance = CatchRules.chance(enemy, sphere);
        battle.log(String.format("— Ход %d — Бросок: «%s» (шанс %d%%)",
                battle.getTurnNumber(), sphere.name(), Math.round(chance * 100)));

        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            battle.finish(BattleStatus.CAPTURED);
            log.info("Бой {}: игрок {} поймал {} сферой {}", battle.getId(), userId, enemy.name, sphere.code());
            spawns.remove(battle.getSpawnId());
            rewards.settle(battle);
            return toDto(save(battle));
        }

        battle.log(enemy.name + " вырвался из сферы!");
        engine.enemyOnlyRound(battle, enemyAi.chooseAbility(battle));
        return finishRound(battle);
    }

    @Transactional(readOnly = true)
    public BattleStateDto get(Long battleId, Long userId) {
        return toDto(load(battleId, userId));
    }

    // ==================== ВНУТРЕННЕЕ ====================

    /** Итог раунда: победа, замена павшего бойца, поражение или следующий ход. */
    private BattleStateDto finishRound(BattleSession battle) {
        Combatant player = battle.player();
        Combatant enemy = battle.enemy();

        if (!enemy.alive()) {
            battle.finish(BattleStatus.WON_BY_KO);
            log.info("Бой {}: победа игрока {} на ходу {}", battle.getId(), battle.getUserId(), battle.getTurnNumber());
            spawns.remove(battle.getSpawnId());
            rewards.settle(battle);
            return toDto(save(battle));
        }

        if (!player.alive()) {
            int next = battle.nextAliveIndex();
            if (next < 0) {
                battle.finish(BattleStatus.LOST);
                log.info("Бой {}: поражение игрока {} на ходу {}", battle.getId(), battle.getUserId(), battle.getTurnNumber());
                rewards.settle(battle);
                return toDto(save(battle));
            }
            // Павший боец уходит, следующий живой выходит сам — ход на это не тратится
            battle.switchTo(next);
            battle.log(player.name + " без сил! В бой выходит " + battle.player().name + ".");
        }

        battle.nextTurn(nextDeadline());
        if (battle.player().skipsTurn()) {
            battle.log(battle.player().name + " не может атаковать в этом ходу — можно сменить бойца или применить предмет.");
        }
        return toDto(save(battle));
    }

    private void checkItemApplies(ItemDto item, Combatant target) {
        switch (item.kind()) {
            case CAPTURE -> throw badRequest("Сферу бросают кнопкой «Поймать»");
            case HEAL -> {
                if (!target.alive()) {
                    throw badRequest(target.name + " без сил — сначала нужна «Искра жизни»");
                }
                boolean curesSomething = item.fullHeal() && Combatant.DEBUFFS.stream().anyMatch(target::has);
                if (target.hp >= target.maxHp && !curesSomething) {
                    throw badRequest(target.name + " и так полностью здоров");
                }
            }
            case REVIVE -> {
                if (target.alive()) {
                    throw badRequest(target.name + " не нуждается в оживлении");
                }
            }
        }
    }

    /** Активный слот, если его эвон в силах; иначе первый живой. */
    private static int firstFighter(List<OwnedPokemonDto> party, int preferredSlot) {
        for (int i = 0; i < party.size(); i++) {
            OwnedPokemonDto p = party.get(i);
            if (p.partySlot() != null && p.partySlot() == preferredSlot && !p.fainted()) return i;
        }
        for (int i = 0; i < party.size(); i++) {
            if (!party.get(i).fainted()) return i;
        }
        throw new IllegalStateException("в отряде нет живых — проверяется раньше");
    }

    private static Combatant fighter(BattleSession battle, int index) {
        if (index < 0 || index >= battle.team().size()) {
            throw badRequest("В отряде нет бойца №" + (index + 1));
        }
        return battle.team().get(index);
    }

    /** Один предмет за ход: повторный клик в том же ходу не спишет предмет второй раз. */
    private static String operationId(BattleSession battle) {
        return "battle-" + battle.getId() + "-turn-" + battle.getTurnNumber();
    }

    /** Бои, сохранённые до переноса способностей в pokemon-service, не содержат списка способностей. */
    private static boolean isPlayable(BattleSession battle) {
        return battle.team().stream().noneMatch(c -> c == null || c.abilities.isEmpty())
                && !battle.enemy().abilities.isEmpty();
    }

    private BattleSession load(Long battleId, Long userId) {
        BattleSession battle = repository.findById(battleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Бой не найден"));
        if (!battle.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Это чужой бой");
        }
        if (!isPlayable(battle)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Бой из старой версии игры, начните новый");
        }
        return battle;
    }

    private BattleSession save(BattleSession battle) {
        battle.syncState();
        return repository.save(battle);
    }

    private BattleStateDto toDto(BattleSession battle) {
        return BattleMapper.toDto(battle, settings.turnTime());
    }

    private Instant nextDeadline() {
        return Instant.now().plus(settings.turnTime());
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
