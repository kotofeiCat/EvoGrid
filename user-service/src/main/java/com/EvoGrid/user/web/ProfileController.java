package com.EvoGrid.user.web;

import com.EvoGrid.common.dto.UserProfileDto;
import com.EvoGrid.common.web.CurrentUser;
import com.EvoGrid.user.profile.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Профиль текущего игрока. ID берётся из JWT, который проверил шлюз. */
@Validated
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "Профиль", description = "Позиция, отряд, здоровье эвонов и прогресс текущего игрока")
public class ProfileController {

    private final ProfileService profiles;

    @GetMapping
    @Operation(summary = "Мой профиль")
    public UserProfileDto me(@Parameter(hidden = true) @CurrentUser Long userId) {
        return profiles.getProfile(userId);
    }

    @PutMapping("/location")
    @Operation(summary = "Обновить мою позицию на карте")
    public UserProfileDto move(@Parameter(hidden = true) @CurrentUser Long userId,
                               @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
                               @RequestParam @DecimalMin("-180") @DecimalMax("180") double lon) {
        return profiles.moveTo(userId, lat, lon);
    }

    @PutMapping("/active-slot")
    @Operation(summary = "Выбрать бойца для следующих боёв")
    public UserProfileDto selectActiveSlot(@Parameter(hidden = true) @CurrentUser Long userId,
                                           @RequestParam @Min(1) @Max(3) int slot) {
        return profiles.selectActiveSlot(userId, slot);
    }

    @PutMapping("/party")
    @Operation(summary = "Поставить эвона из коллекции в слот отряда")
    public UserProfileDto assignToParty(@Parameter(hidden = true) @CurrentUser Long userId,
                                        @Valid @RequestBody PartySlotRequest request) {
        return profiles.assignToParty(userId, request.ownedPokemonId(), request.slot());
    }

    @PostMapping("/pokemons/{ownedPokemonId}/items")
    @Operation(summary = "Вылечить или оживить эвона предметом из сумки")
    public UserProfileDto useItem(@Parameter(hidden = true) @CurrentUser Long userId,
                                  @PathVariable Long ownedPokemonId,
                                  @Valid @RequestBody UseItemRequest request) {
        return profiles.useItem(userId, ownedPokemonId, request.itemCode());
    }

    public record UseItemRequest(@NotBlank(message = "Выберите предмет") String itemCode) {}

    public record PartySlotRequest(
            @NotNull(message = "Не выбран эвон") Long ownedPokemonId,
            @Min(value = 1, message = "Слот 1–3") @Max(value = 3, message = "Слот 1–3") int slot
    ) {}
}
