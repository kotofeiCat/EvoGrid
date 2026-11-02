package com.EvoGrid.battle.client;

import com.EvoGrid.battle.dto.BattleRewardRequest;
import com.EvoGrid.battle.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/{userId}")
    UserProfileDto getUserProfile(@PathVariable("userId") Long userId);

    @PostMapping("/api/users/{userId}/rewards")
    UserProfileDto applyBattleReward(
            @PathVariable("userId") Long userId,
            @RequestBody BattleRewardRequest request
    );
}