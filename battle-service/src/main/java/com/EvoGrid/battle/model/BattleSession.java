package com.EvoGrid.battle.model;

import jakarta.persistence.*;

@Entity
@Table(name = "battle_sessions")
public class BattleSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long spawnId;
    private int turnNumber;

    @Enumerated(EnumType.STRING)
    private BattleStatus status;

    private String playerPokemonName;
    private String playerElement;
    private int playerCurrentHp;
    private int playerMaxHp;
    private int playerAttack;
    private int playerDefense;
    private int playerSpeed;

    private Long enemySpeciesId;
    private String enemyPokemonName;
    private String enemyElement;
    private String enemyArchetype;
    private int enemyCurrentHp;
    private int enemyMaxHp;
    private int enemyAttack;
    private int enemyDefense;
    private int enemySpeed;

    @Column(columnDefinition = "TEXT")
    private String combatLog;

    public BattleSession() {}

    public BattleSession(Long userId, Long spawnId,
                         String playerPokemonName, String playerElement, int playerMaxHp, int playerAttack, int playerDefense, int playerSpeed,
                         Long enemySpeciesId, String enemyPokemonName, String enemyElement, String enemyArchetype,
                         int enemyMaxHp, int enemyAttack, int enemyDefense, int enemySpeed, String initialLog) {
        this.userId = userId;
        this.spawnId = spawnId;
        this.turnNumber = 1;
        this.status = BattleStatus.IN_PROGRESS;
        this.playerPokemonName = playerPokemonName;
        this.playerElement = playerElement;
        this.playerCurrentHp = playerMaxHp;
        this.playerMaxHp = playerMaxHp;
        this.playerAttack = playerAttack;
        this.playerDefense = playerDefense;
        this.playerSpeed = playerSpeed;
        this.enemySpeciesId = enemySpeciesId;
        this.enemyPokemonName = enemyPokemonName;
        this.enemyElement = enemyElement;
        this.enemyArchetype = enemyArchetype;
        this.enemyCurrentHp = enemyMaxHp;
        this.enemyMaxHp = enemyMaxHp;
        this.enemyAttack = enemyAttack;
        this.enemyDefense = enemyDefense;
        this.enemySpeed = enemySpeed;
        this.combatLog = initialLog;
    }

    public void appendLog(String line) {
        this.combatLog = (this.combatLog == null || this.combatLog.isEmpty())
                ? line
                : this.combatLog + "\n" + line;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getSpawnId() { return spawnId; }
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public BattleStatus getStatus() { return status; }
    public void setStatus(BattleStatus status) { this.status = status; }
    public String getPlayerPokemonName() { return playerPokemonName; }
    public String getPlayerElement() { return playerElement; }
    public int getPlayerCurrentHp() { return playerCurrentHp; }
    public void setPlayerCurrentHp(int playerCurrentHp) { this.playerCurrentHp = playerCurrentHp; }
    public int getPlayerMaxHp() { return playerMaxHp; }
    public int getPlayerAttack() { return playerAttack; }
    public int getPlayerDefense() { return playerDefense; }
    public int getPlayerSpeed() { return playerSpeed; }
    public Long getEnemySpeciesId() { return enemySpeciesId; }
    public String getEnemyPokemonName() { return enemyPokemonName; }
    public String getEnemyElement() { return enemyElement; }
    public String getEnemyArchetype() { return enemyArchetype; }
    public int getEnemyCurrentHp() { return enemyCurrentHp; }
    public void setEnemyCurrentHp(int enemyCurrentHp) { this.enemyCurrentHp = enemyCurrentHp; }
    public int getEnemyMaxHp() { return enemyMaxHp; }
    public int getEnemyAttack() { return enemyAttack; }
    public int getEnemyDefense() { return enemyDefense; }
    public int getEnemySpeed() { return enemySpeed; }
    public String getCombatLog() { return combatLog; }
}