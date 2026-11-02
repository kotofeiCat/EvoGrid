package com.EvoGrid.world.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "world_spawns")
public class WorldSpawn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long speciesId;
    private String name;
    private String element;
    private String archetype;

    private int currentHp;
    private int maxHp;
    private int attack;
    private int defense;
    private int speed;

    private double latitude;
    private double longitude;
    private boolean active;
    private LocalDateTime spawnedAt;

    public WorldSpawn() {}

    public WorldSpawn(Long speciesId, String name, String element, String archetype,
                      int maxHp, int attack, int defense, int speed,
                      double latitude, double longitude) {
        this.speciesId = speciesId;
        this.name = name;
        this.element = element;
        this.archetype = archetype;
        this.currentHp = maxHp;
        this.maxHp = maxHp;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = true;
        this.spawnedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getSpeciesId() { return speciesId; }
    public String getName() { return name; }
    public String getElement() { return element; }
    public String getArchetype() { return archetype; }
    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
    public int getMaxHp() { return maxHp; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getSpeed() { return speed; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getSpawnedAt() { return spawnedAt; }
}