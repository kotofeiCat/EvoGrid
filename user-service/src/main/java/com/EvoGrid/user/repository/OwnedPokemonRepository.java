package com.EvoGrid.user.repository;

import com.EvoGrid.user.model.OwnedPokemon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OwnedPokemonRepository extends JpaRepository<OwnedPokemon, Long> {
    List<OwnedPokemon> findByUserIdOrderByIdAsc(Long userId);
    List<OwnedPokemon> findByUserIdAndPartySlotIsNotNullOrderByPartySlotAsc(Long userId);
    Optional<OwnedPokemon> findByUserIdAndPartySlot(Long userId, Integer partySlot);
}