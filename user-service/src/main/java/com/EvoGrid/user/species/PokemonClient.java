package com.EvoGrid.user.species;

import com.EvoGrid.common.dto.PokemonSpeciesDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "pokemon-service")
public interface PokemonClient {

    @GetMapping("/api/pokemons")
    List<PokemonSpeciesDto> getAll();

    @GetMapping("/api/pokemons/{id}")
    PokemonSpeciesDto getById(@PathVariable("id") Long id);
}
