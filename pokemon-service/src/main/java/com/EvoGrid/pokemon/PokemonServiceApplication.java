package com.EvoGrid.pokemon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PokemonServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PokemonServiceApplication.class, args);
    }
}