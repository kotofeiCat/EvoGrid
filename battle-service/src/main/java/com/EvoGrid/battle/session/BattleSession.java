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

    @Column(columnDefinition = "TEXT")
    private String teamJson;

    @Column(columnDefinition = "integer default 0 not null")
    private int activeIndex;

    @Column(columnDefinition = "TEXT")
    private String playerJson;

    @Column(columnDefinition = "TEXT")
    private String enemyJson;

    @Column(columnDefinition = "TEXT")
    private String combatLog;

    private Instant turnDeadline;

    @Version
    @Column(columnDefinition = "bigint default 0 not null")
    private long version;

    private Instant createdAt;

    @Column(columnDefinition = "integer default 0 not null")
    private int coinsPending;

    @Transient
    private List<Combatant> team;

    @Transient
    private Combatant enemy;

    protected BattleSession() {
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

    public List<Combatant> team() {
        if (team == null) {
            team = teamJson != null && !teamJson.isBlank()
                    ? readTeam(teamJson)
                    : new ArrayList<>(List.of(read(playerJson)));
        }
        return team;
    }

    public Combatant player() {
        return team().get(activeIndex);
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void switchTo(int index) {
        if (index < 0 || index >= team().size()) {
            throw new IllegalArgumentException("Нет бойца с номером " + index);
        }
        this.activeIndex = index;
    }

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

    @PrePersist
    public void syncState() {
        if (team != null) teamJson = write(team);
        if (enemy != null) enemyJson = write(enemy);
    }

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
