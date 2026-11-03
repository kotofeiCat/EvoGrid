package com.EvoGrid.battle.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Бой с диким покемоном.
 * Состояние бойцов хранится JSON-ом в двух колонках вместо ~80 отдельных колонок.
 * Таблица новая («battles»), старая «battle_sessions» больше не используется.
 */
@Entity
@Table(name = "battles")
public class BattleSession {

    private static final ObjectMapper JSON = new ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY)
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

    @Column(columnDefinition = "TEXT")
    private String playerJson;

    @Column(columnDefinition = "TEXT")
    private String enemyJson;

    @Column(columnDefinition = "TEXT")
    private String combatLog;

    /** До какого момента игрок должен выбрать действие. */
    private Instant turnDeadline;

    private Instant createdAt;

    @Transient
    private Combatant player;

    @Transient
    private Combatant enemy;

    protected BattleSession() {
        // для JPA
    }

    public BattleSession(Long userId, Long spawnId, Combatant player, Combatant enemy,
                         String firstLogLine, Instant turnDeadline) {
        this.userId = userId;
        this.spawnId = spawnId;
        this.player = player;
        this.enemy = enemy;
        this.turnNumber = 1;
        this.status = BattleStatus.IN_PROGRESS;
        this.combatLog = firstLogLine;
        this.turnDeadline = turnDeadline;
        this.createdAt = Instant.now();
        syncState();
    }

    // ---------- бойцы ----------

    public Combatant player() {
        if (player == null) player = read(playerJson);
        return player;
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
        if (player != null) playerJson = write(player);
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

    // ---------- JSON ----------

    private static Combatant read(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return JSON.readValue(json, Combatant.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Повреждённое состояние бойца в БД", e);
        }
    }

    private static String write(Combatant c) {
        try {
            return JSON.writeValueAsString(c);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сохранить состояние бойца", e);
        }
    }
}
