package com.EvoGrid.pokemon.catalog;

import com.EvoGrid.common.dto.AbilityDto;
import com.EvoGrid.common.dto.PokemonSpeciesDto;
import com.EvoGrid.common.game.StatusEffectType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PokemonCatalogService {

    private final PokemonSpeciesRepository repository;
    private final SpeciesData speciesData;

    public List<PokemonSpeciesDto> getAllSpecies() {
        return repository.findAllByOrderByIdAsc().stream().map(PokemonCatalogService::toDto).toList();
    }

    public List<PokemonSpeciesDto> getStarters() {
        return repository.findByStarterTrueOrderByIdAsc().stream().map(PokemonCatalogService::toDto).toList();
    }

    public PokemonSpeciesDto getSpecies(long id) {
        return repository.findById(id)
                .map(PokemonCatalogService::toDto)
                .orElseThrow(() -> notFound(id));
    }

    public List<AbilityDto> getAbilities(long speciesId) {
        SpeciesDefinition species = speciesData.get(speciesId);
        if (species == null) {
            throw notFound(speciesId);
        }
        return species.abilities().stream()
                .sorted(Comparator.comparingInt(SpeciesDefinition.AbilityDefinition::slot))
                .map(PokemonCatalogService::toDto)
                .toList();
    }

    private static ResponseStatusException notFound(long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Вид не найден: " + id);
    }

    private static PokemonSpeciesDto toDto(PokemonSpecies s) {
        return new PokemonSpeciesDto(
                s.getId(), s.getName(), s.getElement().name(), s.getArchetype(), s.getDescription(),
                s.getBaseHp(), s.getBaseAttack(), s.getBaseDefense(), s.getBaseSpeed(),
                s.isStarter(), s.isSpawnable());
    }

    private static AbilityDto toDto(SpeciesDefinition.AbilityDefinition a) {
        StatusEffectType effect = a.effect() == null ? StatusEffectType.NONE : a.effect();
        StatusEffectType secondEffect = a.secondEffect() == null ? StatusEffectType.NONE : a.secondEffect();
        return new AbilityDto(
                a.slot(), a.code(), a.name(), a.description(), a.category(), a.element(), a.target(),
                a.power(), a.accuracy() == null ? 100 : a.accuracy(), a.cooldown(), 0, a.stab(),
                a.blockPercent(), a.evasionChancePercent(), a.healPercentMaxHp(), a.doubleHealLowHp(),
                a.selfDamagePercentMaxHp(),
                effect, a.effectChance() == null ? 100 : a.effectChance(), a.effectTurns(),
                secondEffect, a.secondEffectTurns());
    }
}
