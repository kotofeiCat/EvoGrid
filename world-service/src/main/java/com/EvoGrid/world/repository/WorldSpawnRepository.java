package com.EvoGrid.world.repository;

import com.EvoGrid.world.model.WorldSpawn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorldSpawnRepository extends JpaRepository<WorldSpawn, Long> {
    List<WorldSpawn> findByActiveTrueAndLatitudeBetweenAndLongitudeBetween(
            double minLat, double maxLat, double minLon, double maxLon
    );
}