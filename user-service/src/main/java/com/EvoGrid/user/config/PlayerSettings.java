package com.EvoGrid.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("players")
public record PlayerSettings(Duration fullRegenTime) {

    public PlayerSettings {
        if (fullRegenTime == null) fullRegenTime = Duration.ofMinutes(30);
    }
}
