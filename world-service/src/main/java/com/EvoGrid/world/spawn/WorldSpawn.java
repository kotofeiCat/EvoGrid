package com.EvoGrid.world.spawn;

import com.EvoGrid.common.dto.PokemonSpeciesDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/** Дикий эвон на карте. */
@Entity
@Table(name = "world_spawns", indexes = @Index(name = "idx_spawn_area", columnList = "active, latitude, longitude"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    /** Кто сейчас с ним сражается и до какого момента держится бронь. */
    private Long reservedBy;
    private Instant reservedUntil;

    /** Защита от одновременной брони двумя игроками. */
    @Version
    @Column(columnDefinition = "bigint default 0 not null")
    private long version;

    public WorldSpawn(PokemonSpeciesDto species, double latitude, double longitude) {
        this.speciesId = species.id();
        this.name = species.name();
        this.element = species.element();
        this.archetype = species.archetype();
        this.maxHp = species.baseHp();
        this.currentHp = species.baseHp();
        this.attack = species.baseAttack();
        this.defense = species.baseDefense();
        this.speed = species.baseSpeed();
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = true;
        this.spawnedAt = LocalDateTime.now();
    }

    public boolean isReservedByOther(Long userId, Instant now) {
        return reservedBy != null && !reservedBy.equals(userId) && reservedUntil != null && reservedUntil.isAfter(now);
    }

    public void reserveFor(Long userId, Instant now, Duration duration) {
        this.reservedBy = userId;
        this.reservedUntil = now.plus(duration);
    }

    public void remove() {
        this.active = false;
        this.currentHp = 0;
        this.reservedBy = null;
        this.reservedUntil = null;
    }
}
