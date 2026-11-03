package com.EvoGrid.battle.controller;

import com.EvoGrid.battle.dto.BattleStateDto;
import com.EvoGrid.battle.dto.StartBattleRequest;
import com.EvoGrid.battle.service.BattleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battles")
@Tag(name = "Battle & Catch API")
public class BattleController {

    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    @PostMapping("/start")
    @Operation(summary = "Начать бой")
    public BattleStateDto startBattle(
            @RequestBody StartBattleRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long authUserId) {
        return battleService.startBattle(request, authUserId);
    }

    @PostMapping("/{battleId}/attack")
    @Operation(summary = "Атаковать")
    public BattleStateDto attack(
            @PathVariable Long battleId,
            @RequestHeader(name = "X-User-Id", required = false) Long authUserId) {
        return battleService.attackTurn(battleId, authUserId);
    }

    @PostMapping("/{battleId}/catch")
    @Operation(summary = "Бросить покебол")
    public BattleStateDto catchPokemon(
            @PathVariable Long battleId,
            @RequestHeader(name = "X-User-Id", required = false) Long authUserId) {
        return battleService.throwPokeball(battleId, authUserId);
    }

    @GetMapping("/{battleId}")
    @Operation(summary = "Состояние боя")
    public BattleStateDto getBattle(
            @PathVariable Long battleId,
            @RequestHeader(name = "X-User-Id", required = false) Long authUserId) {
        return battleService.getBattleState(battleId, authUserId);
    }
}