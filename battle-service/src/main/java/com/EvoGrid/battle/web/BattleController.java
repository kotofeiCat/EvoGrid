package com.EvoGrid.battle.web;

import com.EvoGrid.battle.service.BattleService;
import com.EvoGrid.common.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/battles")
@RequiredArgsConstructor
@Tag(name = "Бои", description = "Пошаговый бой с диким эвоном: способности, смена бойца, предметы и поимка")
public class BattleController {

    private final BattleService battles;

    @PostMapping("/start")
    @Operation(summary = "Начать бой с эвоном на карте")
    public BattleStateDto start(@Parameter(hidden = true) @CurrentUser Long userId,
                                @Valid @RequestBody StartBattleRequest request) {
        return battles.start(userId, request.spawnId(), request.partySlot());
    }

    @PostMapping("/{battleId}/attack")
    @Operation(summary = "Сходить способностью (слот 1–4)")
    public BattleStateDto attack(@Parameter(hidden = true) @CurrentUser Long userId,
                                 @PathVariable Long battleId,
                                 @RequestParam(defaultValue = "1") @Min(1) @Max(4) int slot) {
        return battles.useAbility(battleId, userId, slot);
    }

    @PostMapping("/{battleId}/timeout")
    @Operation(summary = "Время хода вышло или боец спит — разыграть ход автоматически")
    public BattleStateDto timeout(@Parameter(hidden = true) @CurrentUser Long userId, @PathVariable Long battleId) {
        return battles.timeout(battleId, userId);
    }

    @PostMapping("/{battleId}/catch")
    @Operation(summary = "Бросить сферу (по умолчанию — простую)")
    public BattleStateDto throwSphere(@Parameter(hidden = true) @CurrentUser Long userId, @PathVariable Long battleId,
                                      @RequestParam(required = false) String sphere) {
        return battles.throwSphere(battleId, userId, sphere);
    }

    @PostMapping("/{battleId}/switch")
    @Operation(summary = "Сменить бойца на поле (номер в отряде 0–2). Тратит ход")
    public BattleStateDto switchFighter(@Parameter(hidden = true) @CurrentUser Long userId, @PathVariable Long battleId,
                                        @RequestParam @Min(value = 0, message = "Номер бойца 0–2")
                                        @Max(value = 2, message = "Номер бойца 0–2") int index) {
        return battles.switchFighter(battleId, userId, index);
    }

    @PostMapping("/{battleId}/item")
    @Operation(summary = "Применить лечилку или «Искру жизни» на бойца отряда. Тратит ход")
    public BattleStateDto useItem(@Parameter(hidden = true) @CurrentUser Long userId, @PathVariable Long battleId,
                                  @Valid @RequestBody UseItemRequest request) {
        return battles.useItem(battleId, userId, request.itemCode(), request.targetIndex());
    }

    @GetMapping("/{battleId}")
    @Operation(summary = "Текущее состояние боя")
    public BattleStateDto get(@Parameter(hidden = true) @CurrentUser Long userId, @PathVariable Long battleId) {
        return battles.get(battleId, userId);
    }

    public record UseItemRequest(
            @NotBlank(message = "Выберите предмет") String itemCode,
            @Min(value = 0, message = "Номер бойца 0–2") @Max(value = 2, message = "Номер бойца 0–2") Integer targetIndex
    ) {}

    public record StartBattleRequest(
            @NotNull(message = "Не выбран эвон на карте") Long spawnId,
            @Min(1) @Max(3) Integer partySlot
    ) {}
}
