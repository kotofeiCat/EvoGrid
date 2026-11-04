package com.EvoGrid.pokemon.web;

import com.EvoGrid.common.dto.AbilityDto;
import com.EvoGrid.common.dto.PokemonSpeciesDto;
import com.EvoGrid.pokemon.catalog.PokemonCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pokemons")
@RequiredArgsConstructor
@Tag(name = "Эводекс", description = "Виды эвонов, их характеристики и способности")
public class PokemonController {

    private final PokemonCatalogService catalog;

    @GetMapping
    @Operation(summary = "Все виды")
    public List<PokemonSpeciesDto> getAll() {
        return catalog.getAllSpecies();
    }

    @GetMapping("/starters")
    @Operation(summary = "Стартовые виды для регистрации")
    public List<PokemonSpeciesDto> getStarters() {
        return catalog.getStarters();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Вид по ID")
    public PokemonSpeciesDto getById(@PathVariable long id) {
        return catalog.getSpecies(id);
    }

    @GetMapping("/{id}/abilities")
    @Operation(summary = "Четыре способности вида")
    public List<AbilityDto> getAbilities(@PathVariable long id) {
        return catalog.getAbilities(id);
    }
}
