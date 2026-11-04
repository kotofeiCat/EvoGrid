package com.EvoGrid.user.account;

import com.EvoGrid.common.dto.PokemonSpeciesDto;
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

/**
 * Эвон в коллекции игрока. Характеристики сохраняются на момент поимки,
 * чтобы будущие изменения баланса не меняли уже пойманных.
 */
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

    /** 1–3, если эвон в отряде; null — просто в коллекции. */
    private Integer partySlot;

    /** Здоровье на момент hpUpdatedAt; null — здоров полностью (так у записей из старой версии). */
    private Integer currentHp;

    private Instant hpUpdatedAt;

    public OwnedPokemon(UserAccount user, Long speciesId, String name, String element, String archetype,
                        int maxHp, int attack, int defense, int speed, Integer partySlot) {
        this(user, speciesId, name, element, archetype, maxHp, maxHp, attack, defense, speed, partySlot);
    }

    public OwnedPokemon(UserAccount user, Long speciesId, String name, String element, String archetype,
                        int maxHp, int currentHp, int attack, int defense, int speed, Integer partySlot) {
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
        setHp(currentHp, Instant.now());
    }

    public static OwnedPokemon starter(UserAccount user, PokemonSpeciesDto species) {
        return new OwnedPokemon(user, species.id(), species.name(), species.element(), species.archetype(),
                species.baseHp(), species.baseAttack(), species.baseDefense(), species.baseSpeed(), 1);
    }

    // ---------- здоровье ----------

    /**
     * Здоровье сейчас. Вне боя эвоны понемногу восстанавливаются (даже те, кто без сил):
     * от нуля до полного здоровья за fullRegenTime. Считается «на лету», без фоновых задач.
     */
    public int hpAt(Instant now, Duration fullRegenTime) {
        if (currentHp == null) return maxHp;
        if (hpUpdatedAt == null || fullRegenTime.isZero()) return currentHp;
        long seconds = Math.max(0, Duration.between(hpUpdatedAt, now).toSeconds());
        long regenerated = seconds * maxHp / Math.max(1, fullRegenTime.toSeconds());
        return (int) Math.min(maxHp, currentHp + regenerated);
    }

    public void setHp(int hp, Instant now) {
        this.currentHp = Math.max(0, Math.min(maxHp, hp));
        this.hpUpdatedAt = now;
    }

    public void assignToSlot(Integer slot) {
        this.partySlot = slot;
    }

    /** Подтягивает название и стихию из каталога (старые записи могли называться иначе). */
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
