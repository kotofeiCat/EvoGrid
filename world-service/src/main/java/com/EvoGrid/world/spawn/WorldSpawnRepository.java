package com.EvoGrid.world.spawn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface WorldSpawnRepository extends JpaRepository<WorldSpawn, Long> {

    @Query("""
            select s from WorldSpawn s
            where s.active = true and s.spawnedAt > :bornAfter
              and s.latitude between :minLat and :maxLat
              and s.longitude between :minLon and :maxLon
            """)
    List<WorldSpawn> findAliveInArea(double minLat, double maxLat, double minLon, double maxLon, LocalDateTime bornAfter);

    @Modifying
    @Query("delete from WorldSpawn s where s.active = false or s.spawnedAt < :bornBefore")
    int deleteGoneOrOlderThan(LocalDateTime bornBefore);
}
