package com.EvoGrid.world.species;

import com.EvoGrid.common.dto.PokemonSpeciesDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "pokemon-service")
public interface PokemonClient {

    @GetMapping("/api/pokemons")
    List<PokemonSpeciesDto> getAll();
}
