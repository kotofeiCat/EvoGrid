package com.EvoGrid.pokemon.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pokemon_species")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PokemonSpecies {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ElementType element;

    @Column(nullable = false)
    private String archetype;

    @Column(length = 500)
    private String description;

    private int baseHp;
    private int baseAttack;
    private int baseDefense;
    private int baseSpeed;

    private boolean starter;

    @Column(columnDefinition = "boolean default true not null")
    private boolean spawnable;

    public PokemonSpecies(SpeciesDefinition definition) {
        this.id = definition.id();
        updateFrom(definition);
    }

    public void updateFrom(SpeciesDefinition definition) {
        this.name = definition.name();
        this.element = ElementType.valueOf(definition.element());
        this.archetype = definition.archetype();
        this.description = definition.description();
        this.baseHp = definition.stats().hp();
        this.baseAttack = definition.stats().attack();
        this.baseDefense = definition.stats().defense();
        this.baseSpeed = definition.stats().speed();
        this.starter = definition.starter();
        this.spawnable = definition.spawnable();
    }
}
