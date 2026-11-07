package com.EvoGrid.common.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class OpenApiAutoConfiguration {

    @Bean
    OpenAPI evoGridOpenApi(@Value("${spring.application.name}") String serviceName) {
        return new OpenAPI().info(new Info()
                .title("EvoGrid · " + serviceName)
                .version("1.0")
                .description("Эндпоинты /api/** доступны через шлюз, /internal/** вызываются только другими сервисами."));
    }
}
