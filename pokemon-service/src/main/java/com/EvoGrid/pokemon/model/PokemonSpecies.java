package com.EvoGrid.pokemon.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pokemon_species")
public class PokemonSpecies{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public PokemonSpecies() {}

    public PokemonSpecies(String name, ElementType element, String archetype, String description,
                          int baseHp, int baseAttack, int baseDefense, int baseSpeed, boolean starter) {
        this.name = name;
        this.element = element;
        this.archetype = archetype;
        this.description = description;
        this.baseHp = baseHp;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.baseSpeed = baseSpeed;
        this.starter = starter;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ElementType getElement() { return element; }
    public String getArchetype() { return archetype; }
    public String getDescription() { return description; }
    public int getBaseHp() { return baseHp; }
    public int getBaseAttack() { return baseAttack; }
    public int getBaseDefense() { return baseDefense; }
    public int getBaseSpeed() { return baseSpeed; }
    public boolean isStarter() { return starter; }
}