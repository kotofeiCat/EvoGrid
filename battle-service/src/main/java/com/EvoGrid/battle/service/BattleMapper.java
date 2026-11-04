package com.EvoGrid.battle.service;

import com.EvoGrid.battle.engine.Combatant;
import com.EvoGrid.battle.session.BattleSession;
import com.EvoGrid.battle.web.BattleStateDto;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

final class BattleMapper {

    private BattleMapper() {
    }

    static BattleStateDto toDto(BattleSession b, Duration turnTime) {
        Combatant p = b.player();
        Combatant e = b.enemy();
        Instant deadline = b.getTurnDeadline();
        int secondsLeft = deadline == null ? 0 : (int) Math.max(0, Duration.between(Instant.now(), deadline).toSeconds());

        return new BattleStateDto(
                b.getId(), b.getUserId(), b.getSpawnId(), b.getTurnNumber(), b.getStatus(),
                p.name, p.element, p.hp, p.maxHp, p.attack, p.defense, p.speed,
                e.name, e.element, e.archetype, e.hp, e.maxHp, e.attack, e.defense, e.speed,
                b.logLines(),
                p.effectiveSpeed(), e.effectiveSpeed(),
                p.statusBadges(), e.statusBadges(),
                p.abilitiesWithCooldowns(), e.abilitiesWithCooldowns(),
                deadline == null ? 0 : deadline.toEpochMilli(), secondsLeft, (int) turnTime.toSeconds(),
                b.inProgress() && p.skipsTurn(),
                p.speciesId, e.speciesId,
                team(b), b.getActiveIndex());
    }

    private static List<BattleStateDto.Fighter> team(BattleSession b) {
        List<Combatant> team = b.team();
        return IntStream.range(0, team.size())
                .mapToObj(i -> {
                    Combatant c = team.get(i);
                    return new BattleStateDto.Fighter(i, c.ownedPokemonId, c.speciesId, c.name, c.element,
                            c.hp, c.maxHp, i == b.getActiveIndex(), !c.alive(), c.statusBadges());
                })
                .toList();
    }
}
