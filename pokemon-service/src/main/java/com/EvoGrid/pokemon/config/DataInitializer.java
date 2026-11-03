package com.EvoGrid.pokemon.config;

import com.EvoGrid.pokemon.model.ElementType;
import com.EvoGrid.pokemon.model.PokemonSpecies;
import com.EvoGrid.pokemon.repository.PokemonSpeciesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PokemonSpeciesRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(PokemonSpeciesRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Полностью удаляем старых покемонов и сбрасываем счетчик ID (1..13)
        try {
            jdbcTemplate.execute("TRUNCATE TABLE pokemon_species RESTART IDENTITY CASCADE");
        } catch (Exception e) {
            repository.deleteAllInBatch();
        }

        List<PokemonSpecies> speciesList = List.of(
                new PokemonSpecies("Серый грызун", ElementType.NEUTRAL, "Баланс",
                        "Юркий полевой зверёк, обитающий в городских парках и скверах. Отличается высокой живучестью и быстрой реакцией.",
                        75, 25, 25, 25, false),

                new PokemonSpecies("Красная панда", ElementType.FIRE, "Атакующий",
                        "Стремительный огненный боец с раскалёнными когтями. Наносит серию быстрых ударов и оставляет ожоги.",
                        70, 35, 20, 25, true),

                new PokemonSpecies("Огнеборец", ElementType.FIRE, "Воин / Танк",
                        "Могучий четырёхрукий боец огненной стихии. Способен блокировать тяжёлые удары и пробивать любую защиту.",
                        85, 35, 25, 5, false),

                new PokemonSpecies("Пеплоух", ElementType.FIRE, "Убийца / Скорость",
                        "Бесшумная огненная сова, скрывающаяся в клубах горячего дыма и ослепляющая противников пеплом.",
                        65, 30, 20, 35, false),

                new PokemonSpecies("Пиромандра", ElementType.FIRE, "Поддержка / Танк",
                        "Бронированная лавовая саламандра с базальтовой чешуёй. Замедляет врагов магмой и восстанавливает силы в тепле.",
                        80, 20, 30, 20, false),

                new PokemonSpecies("Речная выдра", ElementType.WATER, "Защитник / Контроль",
                        "Ловкий водный хранитель, использующий ракушечный щит для отражения атак и приливные волны для оглушения.",
                        85, 20, 35, 10, true),

                new PokemonSpecies("Сэр Сомуэль", ElementType.WATER, "Поддержка / Танк",
                        "Галантный речной джентльмен в цилиндре. Мастер защитных барьеров, очищения от негативных эффектов и регенерации.",
                        80, 20, 30, 20, false),

                new PokemonSpecies("Плескан", ElementType.WATER, "Снайпер / Баланс",
                        "Морской пеликан, обрушивающий мощные водяные залпы с высоты и ловко уклоняющийся в воздушных потоках.",
                        75, 35, 25, 15, false),

                new PokemonSpecies("Касторакс", ElementType.WATER, "Воин / Щит",
                        "Крепкий речной строитель, возводящий непробиваемые бревенчатые щиты и устойчивый к любому контролю.",
                        75, 35, 25, 15, false),

                new PokemonSpecies("Лесной страж", ElementType.FOREST, "Баланс / Контроль",
                        "Благородный хранитель древней чащи с цветущими рогами, способный погружать противников в глубокий сон.",
                        70, 35, 20, 25, true),

                new PokemonSpecies("Лесной вепрь", ElementType.FOREST, "Танк / Живучесть",
                        "Могучий кабан в плотной моховой броне. Атакует ядовитыми спорами и быстро восстанавливает здоровье.",
                        85, 20, 35, 10, false),

                new PokemonSpecies("Ворон-сыщик", ElementType.FOREST, "Крит / Скорость",
                        "Проницательный лесной ворон с моноклем. Анализирует слабые места врага для гарантированных критических ударов.",
                        65, 30, 20, 35, false),

                new PokemonSpecies("Культурный медведь", ElementType.FOREST, "Брузер / Танк",
                        "Воспитанный хозяин тайги, сочетающий сокрушительную силу лесного зверя с безупречным чайным этикетом.",
                        85, 35, 25, 5, false)
        );

        repository.saveAll(speciesList);
        log.info("База покемонов очищена и инициализирована: загружено {} видов EvoGrid.", speciesList.size());
    }
}