package com.EvoGrid.user.profile;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Настройки игроков (config-server: user-service.yml).
 *
 * @param fullRegenTime за сколько эвон вне боя восстанавливается от нуля до полного здоровья
 */
@ConfigurationProperties("players")
public record PlayerSettings(Duration fullRegenTime) {

    public PlayerSettings {
        if (fullRegenTime == null) fullRegenTime = Duration.ofMinutes(30);
    }
}
