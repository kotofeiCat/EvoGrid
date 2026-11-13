package com.EvoGrid.battle.service;

import com.EvoGrid.battle.client.Bag;
import com.EvoGrid.battle.client.Players;
import com.EvoGrid.battle.config.BattleSettings;
import com.EvoGrid.battle.model.BattleSession;
import com.EvoGrid.battle.model.Combatant;
import com.EvoGrid.battle.repository.BattleSessionRepository;
import com.EvoGrid.common.dto.BattleRewardRequest;
import com.EvoGrid.common.game.Leveling;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BattleRewards {

    private final Players players;
    private final Bag bag;
    private final BattleSessionRepository repository;
    private final BattleSettings settings;

    public void settle(BattleSession battle) {
        int xp;
        int coins;
        int pokemonXp;
        BattleRewardRequest.CaughtPokemon caught = null;
        Combatant enemy = battle.enemy();

        switch (battle.getStatus()) {
            case WON_BY_KO -> {
                xp = settings.xpForWin();
                coins = settings.coinsForWin();
                pokemonXp = Leveling.xpForVictory(enemy.level);
                battle.log("Дикий " + enemy.name + " повержен! +" + xp + " XP, +" + coins + " монет.");
            }
            case CAPTURED -> {
                xp = settings.xpForCatch();
                coins = settings.coinsForCatch();
                pokemonXp = Leveling.xpForVictory(enemy.level);
                caught = new BattleRewardRequest.CaughtPokemon(enemy.speciesId, enemy.name, enemy.element,
                        enemy.archetype, Leveling.unscale(enemy.maxHp, enemy.level), enemy.hp,
                        Leveling.unscale(enemy.attack, enemy.level), Leveling.unscale(enemy.defense, enemy.level),
                        Leveling.unscale(enemy.speed, enemy.level), enemy.level);
                battle.log(enemy.name + " пойман! +" + xp + " XP, +" + coins + " монет.");
            }
            case LOST -> {
                xp = settings.xpForLoss();
                coins = settings.coinsForLoss();
                pokemonXp = Leveling.xpForLoss();
                battle.log("Весь отряд без сил. +" + xp + " XP и +" + coins + " монет за опыт.");
            }
            default -> throw new IllegalStateException("Бой ещё идёт");
        }

        logLevelUps(battle, pokemonXp);
        players.reward(battle.getUserId(), new BattleRewardRequest(xp, partyHp(battle), caught, pokemonXp));
        if (!bag.grantCoins(battle.getUserId(), coinsOperation(battle), coins)) {
            battle.setCoinsPending(coins);
        }
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    @Transactional
    public void retryPendingCoins() {
        for (BattleSession battle : repository.findTop50ByCoinsPendingGreaterThan(0)) {
            if (!bag.grantCoins(battle.getUserId(), coinsOperation(battle), battle.getCoinsPending())) {
                return;
            }
            log.info("Бой {}: игроку {} дозачислено {} монет", battle.getId(), battle.getUserId(), battle.getCoinsPending());
            battle.setCoinsPending(0);
        }
    }

    private static void logLevelUps(BattleSession battle, int pokemonXp) {
        battle.log("Отряд получает по " + pokemonXp + " опыта.");
        for (Combatant fighter : battle.team()) {
            if (fighter.ownedPokemonId == null) continue;
            int newLevel = Leveling.addExperience(fighter.level, fighter.experience, pokemonXp)[0];
            if (newLevel > Leveling.clamp(fighter.level)) {
                battle.log(fighter.name + " достиг " + newLevel + " уровня! Характеристики выросли.");
            }
        }
    }

    private static List<BattleRewardRequest.FighterHp> partyHp(BattleSession battle) {
        return battle.team().stream()
                .filter(fighter -> fighter.ownedPokemonId != null)
                .map(fighter -> new BattleRewardRequest.FighterHp(fighter.ownedPokemonId, fighter.hp))
                .toList();
    }

    private static String coinsOperation(BattleSession battle) {
        return "battle-" + battle.getId() + "-coins";
    }
}
