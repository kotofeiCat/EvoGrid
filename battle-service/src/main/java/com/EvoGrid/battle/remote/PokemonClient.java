package com.EvoGrid.battle.remote;

import com.EvoGrid.common.dto.AbilityDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "pokemon-service", path = "/api/pokemons")
public interface PokemonClient {

    @GetMapping("/{speciesId}/abilities")
    List<AbilityDto> getAbilities(@PathVariable("speciesId") Long speciesId);
}
