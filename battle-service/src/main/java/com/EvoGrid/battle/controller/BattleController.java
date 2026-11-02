package com.EvoGrid.battle.controller;

import com.EvoGrid.battle.dto.BattleStateDto;
import com.EvoGrid.battle.dto.StartBattleRequest;
import com.EvoGrid.battle.service.BattleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battles")
@Tag(name = "Battle & Catch API", description = "Пошаговая боевая система EvoGrid (скорость, стихии 1.5x/0.67x) и поимка покемонов")
public class BattleController {

    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    @PostMapping("/start")
    @Operation(summary = "Начать бой с диким покемоном на карте")
    public BattleStateDto startBattle(@RequestBody StartBattleRequest request) {
        return battleService.startBattle(request);
    }

    @PostMapping("/{battleId}/attack")
    @Operation(summary = "Выполнить ход атаки (быстрый по скорости бьет первым + учет стихий)")
    public BattleStateDto attack(@PathVariable Long battleId) {
        return battleService.attackTurn(battleId);
    }

    @PostMapping("/{battleId}/catch")
    @Operation(summary = "Бросить покебол для поимки дикого покемона")
    public BattleStateDto catchPokemon(@PathVariable Long battleId) {
        return battleService.throwPokeball(battleId);
    }

    @GetMapping("/{battleId}")
    @Operation(summary = "Получить текущее состояние и лог боя")
    public BattleStateDto getBattle(@PathVariable Long battleId) {
        return battleService.getBattleState(battleId);
    }
}