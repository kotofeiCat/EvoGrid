package com.EvoGrid.pokemon.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PokemonSpeciesRepository extends JpaRepository<PokemonSpecies, Long> {

    List<PokemonSpecies> findAllByOrderByIdAsc();

    List<PokemonSpecies> findByStarterTrueOrderByIdAsc();
}
