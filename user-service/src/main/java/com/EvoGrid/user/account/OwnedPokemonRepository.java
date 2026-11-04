package com.EvoGrid.user.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OwnedPokemonRepository extends JpaRepository<OwnedPokemon, Long> {

    List<OwnedPokemon> findByUserIdOrderByIdAsc(Long userId);

    Optional<OwnedPokemon> findByUserIdAndPartySlot(Long userId, Integer partySlot);

    List<OwnedPokemon> findBySpeciesId(Long speciesId);
}
