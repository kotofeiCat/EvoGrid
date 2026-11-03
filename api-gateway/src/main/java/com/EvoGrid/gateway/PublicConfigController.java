package com.EvoGrid.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class PublicConfigController {

    private final String yandexMapsApiKey;

    public PublicConfigController(
            @Value("${yandex.maps.api-key}") String yandexMapsApiKey) {
        this.yandexMapsApiKey = yandexMapsApiKey;
    }

    @GetMapping("/public")
    public Map<String, String> getPublicConfig() {
        return Map.of("yandexMapsApiKey", yandexMapsApiKey);
    }
}