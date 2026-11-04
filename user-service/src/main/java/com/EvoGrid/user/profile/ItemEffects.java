package com.EvoGrid.user.profile;

import com.EvoGrid.common.dto.ItemDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Что предмет делает со здоровьем эвона вне боя. */
final class ItemEffects {

    private ItemEffects() {
    }

    static int hpAfter(ItemDto item, String name, int hp, int maxHp) {
        return switch (item.kind()) {
            case HEAL -> {
                if (hp <= 0) throw badRequest(name + " без сил — сначала нужна «Искра жизни»");
                if (hp >= maxHp) throw badRequest(name + " и так полностью здоров");
                yield item.fullHeal() ? maxHp : Math.min(maxHp, hp + item.healHp());
            }
            case REVIVE -> {
                if (hp > 0) throw badRequest(name + " не нуждается в оживлении");
                yield Math.max(1, maxHp * item.revivePercent() / 100);
            }
            case CAPTURE -> throw badRequest("Сферы используются только в бою");
        };
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
