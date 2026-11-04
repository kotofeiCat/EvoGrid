package com.EvoGrid.battle.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BattleSessionRepository extends JpaRepository<BattleSession, Long> {

    Optional<BattleSession> findFirstByUserIdAndSpawnIdAndStatus(Long userId, Long spawnId, BattleStatus status);

    List<BattleSession> findTop50ByCoinsPendingGreaterThan(int coins);
}
