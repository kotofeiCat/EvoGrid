package com.EvoGrid.pokemon.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SpeciesData {

    private static final String FILE = "data/species.json";

    private final Map<Long, SpeciesDefinition> byId;

    public SpeciesData(ObjectMapper objectMapper) throws IOException {
        try (InputStream in = new ClassPathResource(FILE).getInputStream()) {
            List<SpeciesDefinition> all = objectMapper.readValue(in, new TypeReference<>() {});
            all.forEach(SpeciesData::validate);
            this.byId = all.stream().collect(Collectors.toUnmodifiableMap(SpeciesDefinition::id, Function.identity()));
        }
    }

    public List<SpeciesDefinition> all() {
        return byId.values().stream()
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }

    public SpeciesDefinition get(long id) {
        return byId.get(id);
    }

    private static void validate(SpeciesDefinition species) {
        Set<Integer> slots = species.abilities().stream()
                .map(SpeciesDefinition.AbilityDefinition::slot)
                .collect(Collectors.toSet());
        if (!slots.equals(Set.of(1, 2, 3, 4))) {
            throw new IllegalStateException(FILE + ": у вида " + species.id() + " должны быть способности в слотах 1–4");
        }
    }
}
