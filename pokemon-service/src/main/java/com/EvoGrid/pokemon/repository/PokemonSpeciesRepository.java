package com.EvoGrid.pokemon.repository;

import com.EvoGrid.pokemon.model.PokemonSpecies;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PokemonSpeciesRepository extends JpaRepository<PokemonSpecies, Long> {
    List<PokemonSpecies> findByStarterTrue();
}