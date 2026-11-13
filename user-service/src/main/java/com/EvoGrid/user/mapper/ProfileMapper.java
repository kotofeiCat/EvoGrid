package com.EvoGrid.user.mapper;

import com.EvoGrid.common.dto.OwnedPokemonDto;
import com.EvoGrid.common.dto.UserProfileDto;
import com.EvoGrid.common.game.Leveling;
import com.EvoGrid.user.model.OwnedPokemon;
import com.EvoGrid.user.model.UserAccount;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class ProfileMapper {

    private ProfileMapper() {
    }

    public static UserProfileDto toProfile(UserAccount user, List<OwnedPokemon> collection, Duration fullRegenTime) {
        Instant now = Instant.now();
        List<OwnedPokemonDto> all = collection.stream().map(p -> toDto(p, now, fullRegenTime)).toList();
        List<OwnedPokemonDto> party = all.stream()
                .filter(p -> p.partySlot() != null)
                .sorted(Comparator.comparingInt(OwnedPokemonDto::partySlot))
                .toList();
        return new UserProfileDto(
                user.getId(), user.getEmail(), user.getLevel(), user.getExperience(),
                user.getActivePartySlot(), user.getLatitude(), user.getLongitude(), party, all);
    }

    private static OwnedPokemonDto toDto(OwnedPokemon p, Instant now, Duration fullRegenTime) {
        return new OwnedPokemonDto(p.getId(), p.getSpeciesId(), p.getName(), p.getElement(), p.getArchetype(),
                p.effectiveMaxHp(), p.hpAt(now, fullRegenTime), p.effectiveAttack(), p.effectiveDefense(),
                p.effectiveSpeed(), p.getPartySlot(),
                p.getLevel(), p.getExperience(), Leveling.xpToNext(p.getLevel()));
    }
}
