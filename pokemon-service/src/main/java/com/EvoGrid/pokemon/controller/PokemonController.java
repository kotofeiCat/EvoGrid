package com.EvoGrid.pokemon.controller;

import com.EvoGrid.pokemon.dto.PokemonSpeciesDto;
import com.EvoGrid.pokemon.service.PokemonCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pokemons")
@Tag(name = "Pokemon Pokedex API", description = "Каталог видов петомцев EvoGrid и их базовых характеристик")
public class PokemonController {

    private final PokemonCatalogService catalogService;

    public PokemonController(PokemonCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "Получить все виды покемонов")
    public List<PokemonSpeciesDto> getAll() {
        return catalogService.getAllSpecies();
    }

    @GetMapping("/starters")
    @Operation(summary = "Получить стартовых покемонов для выбора при регистрации")
    public List<PokemonSpeciesDto> getStarters() {
        return catalogService.getStarters();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить вид покемона по ID")
    public PokemonSpeciesDto getById(@PathVariable Long id) {
        return catalogService.getSpeciesById(id);
    }
}