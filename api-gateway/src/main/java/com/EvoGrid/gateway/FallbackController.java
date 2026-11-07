package com.EvoGrid.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class FallbackController {

    private static final Map<String, String> MESSAGES = Map.of(
            "user-service", "Сервис профилей временно недоступен.",
            "pokemon-service", "Каталог эвонов временно недоступен.",
            "world-service", "Сервис карты временно недоступен.",
            "battle-service", "Сервис боёв временно недоступен.",
            "inventory-service", "Магазин и сумка временно недоступны."
    );

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<Map<String, String>> fallback(@PathVariable String service) {
        log.warn("Circuit Breaker: {} недоступен, отдаём fallback-ответ", service);
        String message = MESSAGES.getOrDefault(service, "Сервис " + service + " временно недоступен.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "service", service,
                "message", message + " Попробуйте через несколько секунд."
        ));
    }
}
