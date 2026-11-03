package com.EvoGrid.battle.controller;

import com.EvoGrid.battle.dto.AbilityDto;
import com.EvoGrid.battle.dto.BattleStateDto;
import com.EvoGrid.battle.dto.StartBattleRequest;
import com.EvoGrid.battle.service.AbilityCatalog;
import com.EvoGrid.battle.service.BattleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/battles")
@Tag(name = "Battle & Catch API")
public class BattleController {

    private final BattleService battleService;
    private final AbilityCatalog abilityCatalog;

    public BattleController(BattleService battleService, AbilityCatalog abilityCatalog) {
        this.battleService = battleService;
        this.abilityCatalog = abilityCatalog;
    }

    @GetMapping("/abilities")
    @Operation(summary = "Получить справочник способностей всех покемонов")
    public Map<Long, List<AbilityDto>> getAllAbilities() {
        return abilityCatalog.getAllAbilitiesMap();
    }

    @GetMapping("/abilities/{speciesId}")
    @Operation(summary = "Получить способности конкретного вида покемона")
    public List<AbilityDto> getPokemonAbilities(
            @PathVariable Long speciesId,
            @RequestParam(required = false) String name) {
        return abilityCatalog.getAbilitiesForPokemon(speciesId, name);
    }

    @PostMapping("/start")
    @Operation(summary = "Начать пошаговый бой с диким покемоном")
    public BattleStateDto startBattle(
            @RequestBody StartBattleRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long authUserId) {
        return battleService.start(request, authUserId);
    }

    @PostMapping("/{battleId}/attack")
    @Operation(summary = "Выполнить ход выбранной способностью (слот 1..4)")
    public BattleStateDto attack(
            @PathVariable Long battleId,
            @RequestParam(name = "slot", required = false, defaultValue = "1") Integer slot,
            @RequestHeader(name = "X-User-Id", required = false) Long authUserId) {
        return battleService.useAbility(battleId, authUserId, slot);
    }

    @PostMapping("/{battleId}/catch")
    @Operation(summary = "Бросить Покебол в дикого покемона")
    public BattleStateDto catchPokemon(
            @PathVariable Long battleId,
            @RequestHeader(name = "X-User-Id", required = false) Long authUserId) {
        return battleService.throwPokeball(battleId, authUserId);
    }

    @PostMapping("/{battleId}/timeout")
    @Operation(summary = "Время хода вышло или боец спит/оглушён — разыграть ход автоматически")
    public BattleStateDto timeout(
            @PathVariable Long battleId,
            @RequestHeader(name = "X-User-Id", required = false) Long authUserId) {
        return battleService.timeout(battleId, authUserId);
    }

    @GetMapping("/{battleId}")
    @Operation(summary = "Получить текущее состояние боя")
    public BattleStateDto getBattle(
            @PathVariable Long battleId,
            @RequestHeader(name = "X-User-Id", required = false) Long authUserId) {
        return battleService.get(battleId, authUserId);
    }
}