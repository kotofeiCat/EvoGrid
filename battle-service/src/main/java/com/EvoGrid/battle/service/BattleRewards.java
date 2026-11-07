package com.EvoGrid.battle.service;

import com.EvoGrid.battle.engine.Combatant;
import com.EvoGrid.battle.remote.Bag;
import com.EvoGrid.battle.remote.Players;
import com.EvoGrid.battle.session.BattleSession;
import com.EvoGrid.battle.session.BattleSessionRepository;
import com.EvoGrid.common.dto.BattleRewardRequest;
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
        BattleRewardRequest.CaughtPokemon caught = null;
        Combatant enemy = battle.enemy();

        switch (battle.getStatus()) {
            case WON_BY_KO -> {
                xp = settings.xpForWin();
                coins = settings.coinsForWin();
                battle.log("Дикий " + enemy.name + " повержен! +" + xp + " XP, +" + coins + " монет.");
            }
            case CAPTURED -> {
                xp = settings.xpForCatch();
                coins = settings.coinsForCatch();
                caught = new BattleRewardRequest.CaughtPokemon(enemy.speciesId, enemy.name, enemy.element,
                        enemy.archetype, enemy.maxHp, enemy.hp, enemy.attack, enemy.defense, enemy.speed);
                battle.log(enemy.name + " пойман! +" + xp + " XP, +" + coins + " монет.");
            }
            case LOST -> {
                xp = settings.xpForLoss();
                coins = settings.coinsForLoss();
                battle.log("Весь отряд без сил. +" + xp + " XP и +" + coins + " монет за опыт.");
            }
            default -> throw new IllegalStateException("Бой ещё идёт");
        }

        players.reward(battle.getUserId(), new BattleRewardRequest(xp, partyHp(battle), caught));
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
