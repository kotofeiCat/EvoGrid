package com.EvoGrid.user.model;

import jakarta.persistence.*;

@Entity
@Table(name = "owned_pokemons")
public class OwnedPokemon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    private Long speciesId;
    private String name;
    private String element;
    private String archetype;

    private int maxHp;
    private int attack;
    private int defense;
    private int speed;

    private Integer partySlot;

    public OwnedPokemon() {}

    public OwnedPokemon(UserAccount user, Long speciesId, String name, String element, String archetype,
                        int maxHp, int attack, int defense, int speed, Integer partySlot) {
        this.user = user;
        this.speciesId = speciesId;
        this.name = name;
        this.element = element;
        this.archetype = archetype;
        this.maxHp = maxHp;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.partySlot = partySlot;
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public Long getSpeciesId() { return speciesId; }
    public String getName() { return name; }
    public String getElement() { return element; }
    public String getArchetype() { return archetype; }
    public int getMaxHp() { return maxHp; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getSpeed() { return speed; }
    public Integer getPartySlot() { return partySlot; }
    public void setPartySlot(Integer partySlot) { this.partySlot = partySlot; }
}