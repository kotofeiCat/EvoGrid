package com.EvoGrid.common.dto;

/**
 * Вид эвона из каталога pokemon-service.
 *
 * @param starter   можно выбрать при регистрации
 * @param spawnable появляется на карте (тестовые виды — нет)
 */
public record PokemonSpeciesDto(
        Long id,
        String name,
        String element,
        String archetype,
        String description,
        int baseHp,
        int baseAttack,
        int baseDefense,
        int baseSpeed,
        boolean starter,
        boolean spawnable
) {}
