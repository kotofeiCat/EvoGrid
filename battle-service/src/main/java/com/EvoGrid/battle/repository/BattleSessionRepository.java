package com.EvoGrid.battle.repository;

import com.EvoGrid.battle.model.BattleSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleSessionRepository extends JpaRepository<BattleSession, Long> {
}