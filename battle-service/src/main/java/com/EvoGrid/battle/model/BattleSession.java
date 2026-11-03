package com.EvoGrid.battle.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

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

    private Long playerSpeciesId;
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

    // ==================== КУЛДАУНЫ СПОСОБНОСТЕЙ (Слоты 2, 3, 4) ====================
    @Column(columnDefinition = "integer default 0")
    private int playerCd2;
    @Column(columnDefinition = "integer default 0")
    private int playerCd3;
    @Column(columnDefinition = "integer default 0")
    private int playerCd4;

    @Column(columnDefinition = "integer default 0")
    private int enemyCd2;
    @Column(columnDefinition = "integer default 0")
    private int enemyCd3;
    @Column(columnDefinition = "integer default 0")
    private int enemyCd4;

    // ==================== СТАТУСЫ И БАФФЫ ИГРОКА ====================
    @Column(columnDefinition = "integer default 0")
    private int playerBurnTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerPoisonTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerSleepTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerStunTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerSlowTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerBlindTurns;

    @Column(columnDefinition = "integer default 0")
    private int playerBlockPercent;
    @Column(columnDefinition = "integer default 0")
    private int playerEvasionTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerShellBarrierTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerCritImmuneTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerCcImmuneTurns;

    @Column(columnDefinition = "integer default 0")
    private int playerRegenTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerAtkUpTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerDefUpTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerThornsTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerRageTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerNextHitBurnTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerWaterBoostTurns;
    @Column(columnDefinition = "integer default 0")
    private int playerGuaranteedCritTurns;

    // ==================== СТАТУСЫ И БАФФЫ ВРАГА (ИИ) ====================
    @Column(columnDefinition = "integer default 0")
    private int enemyBurnTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyPoisonTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemySleepTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyStunTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemySlowTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyBlindTurns;

    @Column(columnDefinition = "integer default 0")
    private int enemyBlockPercent;
    @Column(columnDefinition = "integer default 0")
    private int enemyEvasionTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyShellBarrierTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyCritImmuneTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyCcImmuneTurns;

    @Column(columnDefinition = "integer default 0")
    private int enemyRegenTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyAtkUpTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyDefUpTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyThornsTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyRageTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyNextHitBurnTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyWaterBoostTurns;
    @Column(columnDefinition = "integer default 0")
    private int enemyGuaranteedCritTurns;

    public BattleSession() {}

    // Старый конструктор (сохранен для обратной совместимости)
    public BattleSession(Long userId, Long spawnId,
                         String playerPokemonName, String playerElement, int playerMaxHp, int playerAttack, int playerDefense, int playerSpeed,
                         Long enemySpeciesId, String enemyPokemonName, String enemyElement, String enemyArchetype,
                         int enemyMaxHp, int enemyAttack, int enemyDefense, int enemySpeed, String initialLog) {
        this(userId, spawnId, null,
                playerPokemonName, playerElement, playerMaxHp, playerAttack, playerDefense, playerSpeed,
                enemySpeciesId, enemyPokemonName, enemyElement, enemyArchetype,
                enemyMaxHp, enemyAttack, enemyDefense, enemySpeed, initialLog);
    }

    // Расширенный конструктор с поддержкой playerSpeciesId
    public BattleSession(Long userId, Long spawnId, Long playerSpeciesId,
                         String playerPokemonName, String playerElement, int playerMaxHp, int playerAttack, int playerDefense, int playerSpeed,
                         Long enemySpeciesId, String enemyPokemonName, String enemyElement, String enemyArchetype,
                         int enemyMaxHp, int enemyAttack, int enemyDefense, int enemySpeed, String initialLog) {
        this.userId = userId;
        this.spawnId = spawnId;
        this.turnNumber = 1;
        this.status = BattleStatus.IN_PROGRESS;
        this.playerSpeciesId = playerSpeciesId;
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

    // ==================== РАСЧЕТ ЭФФЕКТИВНОЙ СКОРОСТИ ====================
    public int getEffectivePlayerSpeed() {
        double spd = this.playerSpeed;
        if (this.playerSlowTurns > 0) spd *= 0.65;
        if (this.playerRageTurns > 0) spd *= 2.0;
        return Math.max(1, (int) Math.round(spd));
    }

    public int getEffectiveEnemySpeed() {
        double spd = this.enemySpeed;
        if (this.enemySlowTurns > 0) spd *= 0.65;
        if (this.enemyRageTurns > 0) spd *= 2.0;
        return Math.max(1, (int) Math.round(spd));
    }

    // ==================== УПРАВЛЕНИЕ КУЛДАУНАМИ ====================
    public int getPlayerCooldown(int slot) {
        return switch (slot) {
            case 2 -> playerCd2;
            case 3 -> playerCd3;
            case 4 -> playerCd4;
            default -> 0;
        };
    }

    public void setPlayerCooldown(int slot, int cd) {
        int val = Math.max(0, cd);
        if (slot == 2) playerCd2 = val;
        else if (slot == 3) playerCd3 = val;
        else if (slot == 4) playerCd4 = val;
    }

    public int getEnemyCooldown(int slot) {
        return switch (slot) {
            case 2 -> enemyCd2;
            case 3 -> enemyCd3;
            case 4 -> enemyCd4;
            default -> 0;
        };
    }

    public void setEnemyCooldown(int slot, int cd) {
        int val = Math.max(0, cd);
        if (slot == 2) enemyCd2 = val;
        else if (slot == 3) enemyCd3 = val;
        else if (slot == 4) enemyCd4 = val;
    }

    public void tickAllCooldowns() {
        if (playerCd2 > 0) playerCd2--;
        if (playerCd3 > 0) playerCd3--;
        if (playerCd4 > 0) playerCd4--;
        if (enemyCd2 > 0) enemyCd2--;
        if (enemyCd3 > 0) enemyCd3--;
        if (enemyCd4 > 0) enemyCd4--;
    }

    // ==================== СНЯТИЕ ДЕБАФФОВ (CLEANSE) ====================
    public void cleansePlayerDebuffs() {
        this.playerBurnTurns = 0;
        this.playerPoisonTurns = 0;
        this.playerSleepTurns = 0;
        this.playerStunTurns = 0;
        this.playerSlowTurns = 0;
        this.playerBlindTurns = 0;
    }

    public void cleanseEnemyDebuffs() {
        this.enemyBurnTurns = 0;
        this.enemyPoisonTurns = 0;
        this.enemySleepTurns = 0;
        this.enemyStunTurns = 0;
        this.enemySlowTurns = 0;
        this.enemyBlindTurns = 0;
    }

    // ==================== СПИСОК АКТИВНЫХ СТАТУСОВ ДЛЯ UI ====================
    public List<String> getPlayerStatusBadges() {
        List<String> list = new ArrayList<>();
        if (playerBurnTurns > 0) list.add("Горение (" + playerBurnTurns + ")");
        if (playerPoisonTurns > 0) list.add("Яд (" + playerPoisonTurns + ")");
        if (playerSleepTurns > 0) list.add("Сон (" + playerSleepTurns + ")");
        if (playerStunTurns > 0) list.add("Оглушен (" + playerStunTurns + ")");
        if (playerSlowTurns > 0) list.add("Замедлен (" + playerSlowTurns + ")");
        if (playerBlindTurns > 0) list.add("Ослеплен (" + playerBlindTurns + ")");
        if (playerBlockPercent > 0) list.add("Блок " + playerBlockPercent + "%");
        if (playerEvasionTurns > 0) list.add("Уворот 65%");
        if (playerShellBarrierTurns > 0) list.add("Барьер");
        if (playerCritImmuneTurns > 0) list.add("Анти-крит (" + playerCritImmuneTurns + ")");
        if (playerCcImmuneTurns > 0) list.add("Иммунитет (" + playerCcImmuneTurns + ")");
        if (playerRegenTurns > 0) list.add("Реген (" + playerRegenTurns + ")");
        if (playerAtkUpTurns > 0) list.add("Атака +30% (" + playerAtkUpTurns + ")");
        if (playerDefUpTurns > 0) list.add("Защита +35% (" + playerDefUpTurns + ")");
        if (playerThornsTurns > 0) list.add("Шипы (" + playerThornsTurns + ")");
        if (playerRageTurns > 0) list.add("Раж SPD x2 (" + playerRageTurns + ")");
        if (playerNextHitBurnTurns > 0) list.add("Заряд огня");
        if (playerWaterBoostTurns > 0) list.add("Водный заряд +35%");
        if (playerGuaranteedCritTurns > 0) list.add("Точный крит");
        return list;
    }

    public List<String> getEnemyStatusBadges() {
        List<String> list = new ArrayList<>();
        if (enemyBurnTurns > 0) list.add("Горение (" + enemyBurnTurns + ")");
        if (enemyPoisonTurns > 0) list.add("Яд (" + enemyPoisonTurns + ")");
        if (enemySleepTurns > 0) list.add("Сон (" + enemySleepTurns + ")");
        if (enemyStunTurns > 0) list.add("Оглушен (" + enemyStunTurns + ")");
        if (enemySlowTurns > 0) list.add("Замедлен (" + enemySlowTurns + ")");
        if (enemyBlindTurns > 0) list.add("Ослеплен (" + enemyBlindTurns + ")");
        if (enemyBlockPercent > 0) list.add("Блок " + enemyBlockPercent + "%");
        if (enemyEvasionTurns > 0) list.add("Уворот 65%");
        if (enemyShellBarrierTurns > 0) list.add("Барьер");
        if (enemyCritImmuneTurns > 0) list.add("Анти-крит (" + enemyCritImmuneTurns + ")");
        if (enemyCcImmuneTurns > 0) list.add("Иммунитет (" + enemyCcImmuneTurns + ")");
        if (enemyRegenTurns > 0) list.add("Реген (" + enemyRegenTurns + ")");
        if (enemyAtkUpTurns > 0) list.add("Атака +30% (" + enemyAtkUpTurns + ")");
        if (enemyDefUpTurns > 0) list.add("Защита +35% (" + enemyDefUpTurns + ")");
        if (enemyThornsTurns > 0) list.add("Шипы (" + enemyThornsTurns + ")");
        if (enemyRageTurns > 0) list.add("Раж SPD x2 (" + enemyRageTurns + ")");
        if (enemyNextHitBurnTurns > 0) list.add("Заряд огня");
        if (enemyWaterBoostTurns > 0) list.add("Водный заряд +35%");
        if (enemyGuaranteedCritTurns > 0) list.add("Точный крит");
        return list;
    }

    // ==================== СТАНДАРТНЫЕ ГЕТТЕРЫ И СЕТТЕРЫ ====================
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getSpawnId() { return spawnId; }
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public BattleStatus getStatus() { return status; }
    public void setStatus(BattleStatus status) { this.status = status; }

    public Long getPlayerSpeciesId() { return playerSpeciesId; }
    public void setPlayerSpeciesId(Long playerSpeciesId) { this.playerSpeciesId = playerSpeciesId; }
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

    public int getPlayerBurnTurns() { return playerBurnTurns; }
    public void setPlayerBurnTurns(int playerBurnTurns) { this.playerBurnTurns = Math.max(0, playerBurnTurns); }
    public int getPlayerPoisonTurns() { return playerPoisonTurns; }
    public void setPlayerPoisonTurns(int playerPoisonTurns) { this.playerPoisonTurns = Math.max(0, playerPoisonTurns); }
    public int getPlayerSleepTurns() { return playerSleepTurns; }
    public void setPlayerSleepTurns(int playerSleepTurns) { this.playerSleepTurns = Math.max(0, playerSleepTurns); }
    public int getPlayerStunTurns() { return playerStunTurns; }
    public void setPlayerStunTurns(int playerStunTurns) { this.playerStunTurns = Math.max(0, playerStunTurns); }
    public int getPlayerSlowTurns() { return playerSlowTurns; }
    public void setPlayerSlowTurns(int playerSlowTurns) { this.playerSlowTurns = Math.max(0, playerSlowTurns); }
    public int getPlayerBlindTurns() { return playerBlindTurns; }
    public void setPlayerBlindTurns(int playerBlindTurns) { this.playerBlindTurns = Math.max(0, playerBlindTurns); }
    public int getPlayerBlockPercent() { return playerBlockPercent; }
    public void setPlayerBlockPercent(int playerBlockPercent) { this.playerBlockPercent = Math.max(0, playerBlockPercent); }
    public int getPlayerEvasionTurns() { return playerEvasionTurns; }
    public void setPlayerEvasionTurns(int playerEvasionTurns) { this.playerEvasionTurns = Math.max(0, playerEvasionTurns); }
    public int getPlayerShellBarrierTurns() { return playerShellBarrierTurns; }
    public void setPlayerShellBarrierTurns(int playerShellBarrierTurns) { this.playerShellBarrierTurns = Math.max(0, playerShellBarrierTurns); }
    public int getPlayerCritImmuneTurns() { return playerCritImmuneTurns; }
    public void setPlayerCritImmuneTurns(int playerCritImmuneTurns) { this.playerCritImmuneTurns = Math.max(0, playerCritImmuneTurns); }
    public int getPlayerCcImmuneTurns() { return playerCcImmuneTurns; }
    public void setPlayerCcImmuneTurns(int playerCcImmuneTurns) { this.playerCcImmuneTurns = Math.max(0, playerCcImmuneTurns); }
    public int getPlayerRegenTurns() { return playerRegenTurns; }
    public void setPlayerRegenTurns(int playerRegenTurns) { this.playerRegenTurns = Math.max(0, playerRegenTurns); }
    public int getPlayerAtkUpTurns() { return playerAtkUpTurns; }
    public void setPlayerAtkUpTurns(int playerAtkUpTurns) { this.playerAtkUpTurns = Math.max(0, playerAtkUpTurns); }
    public int getPlayerDefUpTurns() { return playerDefUpTurns; }
    public void setPlayerDefUpTurns(int playerDefUpTurns) { this.playerDefUpTurns = Math.max(0, playerDefUpTurns); }
    public int getPlayerThornsTurns() { return playerThornsTurns; }
    public void setPlayerThornsTurns(int playerThornsTurns) { this.playerThornsTurns = Math.max(0, playerThornsTurns); }
    public int getPlayerRageTurns() { return playerRageTurns; }
    public void setPlayerRageTurns(int playerRageTurns) { this.playerRageTurns = Math.max(0, playerRageTurns); }
    public int getPlayerNextHitBurnTurns() { return playerNextHitBurnTurns; }
    public void setPlayerNextHitBurnTurns(int playerNextHitBurnTurns) { this.playerNextHitBurnTurns = Math.max(0, playerNextHitBurnTurns); }
    public int getPlayerWaterBoostTurns() { return playerWaterBoostTurns; }
    public void setPlayerWaterBoostTurns(int playerWaterBoostTurns) { this.playerWaterBoostTurns = Math.max(0, playerWaterBoostTurns); }
    public int getPlayerGuaranteedCritTurns() { return playerGuaranteedCritTurns; }
    public void setPlayerGuaranteedCritTurns(int playerGuaranteedCritTurns) { this.playerGuaranteedCritTurns = Math.max(0, playerGuaranteedCritTurns); }

    public int getEnemyBurnTurns() { return enemyBurnTurns; }
    public void setEnemyBurnTurns(int enemyBurnTurns) { this.enemyBurnTurns = Math.max(0, enemyBurnTurns); }
    public int getEnemyPoisonTurns() { return enemyPoisonTurns; }
    public void setEnemyPoisonTurns(int enemyPoisonTurns) { this.enemyPoisonTurns = Math.max(0, enemyPoisonTurns); }
    public int getEnemySleepTurns() { return enemySleepTurns; }
    public void setEnemySleepTurns(int enemySleepTurns) { this.enemySleepTurns = Math.max(0, enemySleepTurns); }
    public int getEnemyStunTurns() { return enemyStunTurns; }
    public void setEnemyStunTurns(int enemyStunTurns) { this.enemyStunTurns = Math.max(0, enemyStunTurns); }
    public int getEnemySlowTurns() { return enemySlowTurns; }
    public void setEnemySlowTurns(int enemySlowTurns) { this.enemySlowTurns = Math.max(0, enemySlowTurns); }
    public int getEnemyBlindTurns() { return enemyBlindTurns; }
    public void setEnemyBlindTurns(int enemyBlindTurns) { this.enemyBlindTurns = Math.max(0, enemyBlindTurns); }
    public int getEnemyBlockPercent() { return enemyBlockPercent; }
    public void setEnemyBlockPercent(int enemyBlockPercent) { this.enemyBlockPercent = Math.max(0, enemyBlockPercent); }
    public int getEnemyEvasionTurns() { return enemyEvasionTurns; }
    public void setEnemyEvasionTurns(int enemyEvasionTurns) { this.enemyEvasionTurns = Math.max(0, enemyEvasionTurns); }
    public int getEnemyShellBarrierTurns() { return enemyShellBarrierTurns; }
    public void setEnemyShellBarrierTurns(int enemyShellBarrierTurns) { this.enemyShellBarrierTurns = Math.max(0, enemyShellBarrierTurns); }
    public int getEnemyCritImmuneTurns() { return enemyCritImmuneTurns; }
    public void setEnemyCritImmuneTurns(int enemyCritImmuneTurns) { this.enemyCritImmuneTurns = Math.max(0, enemyCritImmuneTurns); }
    public int getEnemyCcImmuneTurns() { return enemyCcImmuneTurns; }
    public void setEnemyCcImmuneTurns(int enemyCcImmuneTurns) { this.enemyCcImmuneTurns = Math.max(0, enemyCcImmuneTurns); }
    public int getEnemyRegenTurns() { return enemyRegenTurns; }
    public void setEnemyRegenTurns(int enemyRegenTurns) { this.enemyRegenTurns = Math.max(0, enemyRegenTurns); }
    public int getEnemyAtkUpTurns() { return enemyAtkUpTurns; }
    public void setEnemyAtkUpTurns(int enemyAtkUpTurns) { this.enemyAtkUpTurns = Math.max(0, enemyAtkUpTurns); }
    public int getEnemyDefUpTurns() { return enemyDefUpTurns; }
    public void setEnemyDefUpTurns(int enemyDefUpTurns) { this.enemyDefUpTurns = Math.max(0, enemyDefUpTurns); }
    public int getEnemyThornsTurns() { return enemyThornsTurns; }
    public void setEnemyThornsTurns(int enemyThornsTurns) { this.enemyThornsTurns = Math.max(0, enemyThornsTurns); }
    public int getEnemyRageTurns() { return enemyRageTurns; }
    public void setEnemyRageTurns(int enemyRageTurns) { this.enemyRageTurns = Math.max(0, enemyRageTurns); }
    public int getEnemyNextHitBurnTurns() { return enemyNextHitBurnTurns; }
    public void setEnemyNextHitBurnTurns(int enemyNextHitBurnTurns) { this.enemyNextHitBurnTurns = Math.max(0, enemyNextHitBurnTurns); }
    public int getEnemyWaterBoostTurns() { return enemyWaterBoostTurns; }
    public void setEnemyWaterBoostTurns(int enemyWaterBoostTurns) { this.enemyWaterBoostTurns = Math.max(0, enemyWaterBoostTurns); }
    public int getEnemyGuaranteedCritTurns() { return enemyGuaranteedCritTurns; }
    public void setEnemyGuaranteedCritTurns(int enemyGuaranteedCritTurns) { this.enemyGuaranteedCritTurns = Math.max(0, enemyGuaranteedCritTurns); }
}