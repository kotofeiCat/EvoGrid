package com.EvoGrid.battle.session;

import com.EvoGrid.battle.engine.Combatant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Бой с диким эвоном.
 * Состояние бойцов хранится JSON-ом: весь отряд игрока (до трёх эвонов) и дикий противник.
 * На поле в каждый момент один боец отряда — activeIndex.
 */
@Entity
@Table(name = "battles")
public class BattleSession {

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long spawnId;

    private int turnNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BattleStatus status;

    /** Отряд игрока (JSON-массив). */
    @Column(columnDefinition = "TEXT")
    private String teamJson;

    /** Кто из отряда сейчас на поле. */
    @Column(columnDefinition = "integer default 0 not null")
    private int activeIndex;

    /** Боец игрока из версии без отрядов — читается, только если teamJson пуст. */
    @Column(columnDefinition = "TEXT")
    private String playerJson;

    @Column(columnDefinition = "TEXT")
    private String enemyJson;

    @Column(columnDefinition = "TEXT")
    private String combatLog;

    /** До какого момента игрок должен выбрать действие. */
    private Instant turnDeadline;

    /** Защита от двойной обработки хода (два быстрых клика). */
    @Version
    @Column(columnDefinition = "bigint default 0 not null")
    private long version;

    private Instant createdAt;

    /** Монеты за итог боя, которые ещё не удалось начислить (inventory-service был недоступен). */
    @Column(columnDefinition = "integer default 0 not null")
    private int coinsPending;

    @Transient
    private List<Combatant> team;

    @Transient
    private Combatant enemy;

    protected BattleSession() {
        // для JPA
    }

    public BattleSession(Long userId, Long spawnId, List<Combatant> team, int activeIndex, Combatant enemy,
                         String firstLogLine, Instant turnDeadline) {
        this.userId = userId;
        this.spawnId = spawnId;
        this.team = new ArrayList<>(team);
        this.activeIndex = activeIndex;
        this.enemy = enemy;
        this.turnNumber = 1;
        this.status = BattleStatus.IN_PROGRESS;
        this.combatLog = firstLogLine;
        this.turnDeadline = turnDeadline;
        this.createdAt = Instant.now();
        syncState();
    }

    // ---------- бойцы ----------

    /** Весь отряд игрока по порядку слотов. */
    public List<Combatant> team() {
        if (team == null) {
            team = teamJson != null && !teamJson.isBlank()
                    ? readTeam(teamJson)
                    : new ArrayList<>(List.of(read(playerJson)));
        }
        return team;
    }

    /** Боец игрока, который сейчас на поле. */
    public Combatant player() {
        return team().get(activeIndex);
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    /** Вывести на поле другого бойца отряда. */
    public void switchTo(int index) {
        if (index < 0 || index >= team().size()) {
            throw new IllegalArgumentException("Нет бойца с номером " + index);
        }
        this.activeIndex = index;
    }

    /** Первый живой боец отряда, кроме того, что на поле; -1 — живых не осталось. */
    public int nextAliveIndex() {
        for (int i = 0; i < team().size(); i++) {
            if (i != activeIndex && team().get(i).alive()) return i;
        }
        return -1;
    }

    public boolean teamDefeated() {
        return team().stream().noneMatch(Combatant::alive);
    }

    public Combatant enemy() {
        if (enemy == null) enemy = read(enemyJson);
        return enemy;
    }

    /**
     * Записывает изменения бойцов обратно в JSON-колонки.
     * Вызывается сервисом перед сохранением: так Hibernate гарантированно видит изменения.
     */
    @PrePersist
    public void syncState() {
        if (team != null) teamJson = write(team);
        if (enemy != null) enemyJson = write(enemy);
    }

    // ---------- ход боя ----------

    public boolean inProgress() {
        return status == BattleStatus.IN_PROGRESS;
    }

    public void finish(BattleStatus result) {
        this.status = result;
        this.turnDeadline = null;
    }

    public void nextTurn(Instant newDeadline) {
        this.turnNumber++;
        this.turnDeadline = newDeadline;
    }

    public void log(String line) {
        combatLog = (combatLog == null || combatLog.isEmpty()) ? line : combatLog + "\n" + line;
    }

    public List<String> logLines() {
        return combatLog == null || combatLog.isEmpty() ? List.of() : Arrays.asList(combatLog.split("\n"));
    }

    // ---------- геттеры ----------

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getSpawnId() { return spawnId; }
    public int getTurnNumber() { return turnNumber; }
    public BattleStatus getStatus() { return status; }
    public Instant getTurnDeadline() { return turnDeadline; }
    public Instant getCreatedAt() { return createdAt; }
    public int getCoinsPending() { return coinsPending; }

    public void setCoinsPending(int coins) {
        this.coinsPending = Math.max(0, coins);
    }

    // ---------- JSON ----------

    private static Combatant read(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return JSON.readValue(json, Combatant.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Повреждённое состояние бойца в БД", e);
        }
    }

    private static List<Combatant> readTeam(String json) {
        try {
            return new ArrayList<>(JSON.readValue(json, new TypeReference<List<Combatant>>() {}));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Повреждённое состояние отряда в БД", e);
        }
    }

    private static String write(Object c) {
        try {
            return JSON.writeValueAsString(c);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сохранить состояние бойца", e);
        }
    }
}
