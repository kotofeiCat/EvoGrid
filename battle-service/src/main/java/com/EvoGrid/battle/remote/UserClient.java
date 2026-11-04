package com.EvoGrid.battle.remote;

import com.EvoGrid.common.dto.BattleRewardRequest;
import com.EvoGrid.common.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserClient {

    @GetMapping("/{userId}")
    UserProfileDto getProfile(@PathVariable("userId") Long userId);

    @PostMapping("/{userId}/rewards")
    UserProfileDto applyReward(@PathVariable("userId") Long userId, @RequestBody BattleRewardRequest reward);
}
