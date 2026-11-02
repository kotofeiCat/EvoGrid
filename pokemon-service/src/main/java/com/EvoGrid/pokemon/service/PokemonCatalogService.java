package com.EvoGrid.pokemon.service;

import com.EvoGrid.pokemon.dto.PokemonSpeciesDto;
import com.EvoGrid.pokemon.model.PokemonSpecies;
import com.EvoGrid.pokemon.repository.PokemonSpeciesRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PokemonCatalogService {

    private final PokemonSpeciesRepository repository;

    public PokemonCatalogService(PokemonSpeciesRepository repository) {
        this.repository = repository;
    }

    public List<PokemonSpeciesDto> getAllSpecies() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public List<PokemonSpeciesDto> getStarters() {
        return repository.findByStarterTrue().stream().map(this::toDto).toList();
    }

    public PokemonSpeciesDto getSpeciesById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Вид не найден: " + id));
    }

    private PokemonSpeciesDto toDto(PokemonSpecies s) {
        return new PokemonSpeciesDto(
                s.getId(), s.getName(), s.getElement(), s.getArchetype(),
                s.getDescription(), s.getBaseHp(), s.getBaseAttack(),
                s.getBaseDefense(), s.getBaseSpeed(), s.isStarter()
        );
    }
}