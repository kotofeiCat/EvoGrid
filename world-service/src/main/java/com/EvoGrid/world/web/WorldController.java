package com.EvoGrid.world.web;

import com.EvoGrid.common.dto.WorldSpawnDto;
import com.EvoGrid.common.web.CurrentUser;
import com.EvoGrid.world.spawn.SpawnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/world")
@RequiredArgsConstructor
@Tag(name = "Карта", description = "Дикие эвоны вокруг игрока")
public class WorldController {

    private final SpawnService spawns;

    @GetMapping("/spawns")
    @Operation(summary = "Эвоны вокруг точки; если их мало — появятся новые")
    public List<WorldSpawnDto> around(@Parameter(hidden = true) @CurrentUser Long userId,
                                      @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
                                      @RequestParam @DecimalMin("-180") @DecimalMax("180") double lon) {
        return spawns.around(lat, lon, userId);
    }
}
