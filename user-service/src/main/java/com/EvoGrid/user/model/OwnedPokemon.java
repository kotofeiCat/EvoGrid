package com.EvoGrid.user.model;

import com.EvoGrid.common.dto.PokemonSpeciesDto;
import com.EvoGrid.common.game.Leveling;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "owned_pokemons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    private Integer currentHp;

    private Instant hpUpdatedAt;

    private Integer level;

    private Integer experience;

    public OwnedPokemon(UserAccount user, Long speciesId, String name, String element, String archetype,
                        int maxHp, int attack, int defense, int speed, Integer partySlot) {
        this(user, speciesId, name, element, archetype, maxHp, maxHp, attack, defense, speed, partySlot);
    }

    public OwnedPokemon(UserAccount user, Long speciesId, String name, String element, String archetype,
                        int maxHp, int currentHp, int attack, int defense, int speed, Integer partySlot) {
        this(user, speciesId, name, element, archetype, maxHp, currentHp, attack, defense, speed, partySlot, 1);
    }

    public OwnedPokemon(UserAccount user, Long speciesId, String name, String element, String archetype,
                        int maxHp, int currentHp, int attack, int defense, int speed, Integer partySlot, int level) {
        this.user = user;
        this.level = Leveling.clamp(level);
        this.experience = 0;
        this.speciesId = speciesId;
        this.name = name;
        this.element = element;
        this.archetype = archetype;
        this.maxHp = maxHp;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.partySlot = partySlot;
        setHp(currentHp, Instant.now());
    }

    public static OwnedPokemon starter(UserAccount user, PokemonSpeciesDto species) {
        return new OwnedPokemon(user, species.id(), species.name(), species.element(), species.archetype(),
                species.baseHp(), species.baseAttack(), species.baseDefense(), species.baseSpeed(), 1);
    }

    public int getLevel() {
        return Leveling.clamp(level);
    }

    public int getExperience() {
        return experience == null ? 0 : experience;
    }

    public int effectiveMaxHp() {
        return Leveling.scale(maxHp, getLevel());
    }

    public int effectiveAttack() {
        return Leveling.scale(attack, getLevel());
    }

    public int effectiveDefense() {
        return Leveling.scale(defense, getLevel());
    }

    public int effectiveSpeed() {
        return Leveling.scale(speed, getLevel());
    }

    public int hpAt(Instant now, Duration fullRegenTime) {
        int max = effectiveMaxHp();
        if (currentHp == null) return max;
        if (hpUpdatedAt == null || fullRegenTime.isZero()) return Math.min(max, currentHp);
        long seconds = Math.max(0, Duration.between(hpUpdatedAt, now).toSeconds());
        long regenerated = seconds * max / Math.max(1, fullRegenTime.toSeconds());
        return (int) Math.min(max, currentHp + regenerated);
    }

    public void setHp(int hp, Instant now) {
        this.currentHp = Math.max(0, Math.min(effectiveMaxHp(), hp));
        this.hpUpdatedAt = now;
    }

    public int gainExperience(int gained, Instant now, Duration fullRegenTime) {
        int before = getLevel();
        int hp = hpAt(now, fullRegenTime);
        int oldMax = effectiveMaxHp();
        int[] result = Leveling.addExperience(before, getExperience(), gained);
        this.level = result[0];
        this.experience = result[1];
        if (this.level > before) {
            setHp(hp + effectiveMaxHp() - oldMax, now);
        }
        return this.level - before;
    }

    public void assignToSlot(Integer slot) {
        this.partySlot = slot;
    }

    public boolean renameFrom(PokemonSpeciesDto species) {
        if (species.name().equals(name) && species.element().equals(element) && species.archetype().equals(archetype)) {
            return false;
        }
        this.name = species.name();
        this.element = species.element();
        this.archetype = species.archetype();
        return true;
    }
}
