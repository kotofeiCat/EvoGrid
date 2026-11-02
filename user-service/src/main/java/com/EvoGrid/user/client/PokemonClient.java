package com.EvoGrid.user.client;

import com.EvoGrid.user.dto.PokemonSpeciesDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "pokemon-service")
public interface PokemonClient {

    @GetMapping("/api/pokemons/{id}")
    PokemonSpeciesDto getSpeciesById(@PathVariable("id") Long id);
}