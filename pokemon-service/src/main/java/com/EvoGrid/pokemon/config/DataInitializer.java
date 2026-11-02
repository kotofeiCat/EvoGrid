package com.EvoGrid.pokemon.config;

import com.EvoGrid.pokemon.model.ElementType;
import com.EvoGrid.pokemon.model.PokemonSpecies;
import com.EvoGrid.pokemon.repository.PokemonSpeciesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final PokemonSpeciesRepository repository;

    public DataInitializer(PokemonSpeciesRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        List<PokemonSpecies> speciesList = List.of(
                new PokemonSpecies("Крысюк", ElementType.NEUTRAL, "Базовый крип",
                        "Слепой крысеныш — базовый моб с усредненными характеристиками, от которого балансятся все виды.",
                        75, 25, 25, 25, false),

                new PokemonSpecies("Красная панда", ElementType.FIRE, "Средний ДПС",
                        "Покемон вулканического происхождения. Один из трех стартовых покемонов.",
                        70, 35, 20, 25, true),
                new PokemonSpecies("Пихач", ElementType.FIRE, "Тяжелый брузер",
                        "Коренастый четверорукий Буйвол. Когда злится, из хохолка валит густой пар, а кулаки раскаляются докрасна.",
                        85, 35, 25, 5, false),
                new PokemonSpecies("Синдерхут", ElementType.FIRE, "Легкий ДПС",
                        "Округлая сова с пепельно-серым телом, воротником из сизого дыма и тлеющими оранжевыми перьями.",
                        65, 30, 20, 35, false),
                new PokemonSpecies("Шаломандра", ElementType.FIRE, "Танк",
                        "Огненная саламандра с крепкой чешуей, выдерживающая экстремальные температуры.",
                        80, 20, 30, 20, false),


                new PokemonSpecies("Выдрочка", ElementType.WATER, "Танк",
                        "Выносливый водный защитник с плотным мехом и высокой защитой. Стартовый покемон воды.",
                        85, 20, 35, 10, true),
                new PokemonSpecies("Сэр Сомуэль", ElementType.WATER, "Поддержка / Танк",
                        "Просчитывает ходы соперника, парирует удары тростью, успокаивает врагов чаем и создает защитные волны.",
                        80, 20, 30, 20, false),
                new PokemonSpecies("Сплэшикан", ElementType.WATER, "Средний ДПС",
                        "Коренастая птица в бело-лазурном оперении с полупрозрачным светящимся клювом, полным морской воды.",
                        75, 35, 25, 15, false),
                new PokemonSpecies("Касторакс", ElementType.WATER, "Тяжелый брузер",
                        "Покемон речных порогов. Крушит броню мощными резцами и сбивает с ног ударами тяжелого хвоста-весла.",
                        75, 35, 25, 15, false),


                new PokemonSpecies("Рогатый Куколд", ElementType.FOREST, "Средний ДПС",
                        "Лесной архитектор со стремительными атаками. Стартовый покемон леса.",
                        70, 35, 20, 25, true),
                new PokemonSpecies("Свинохрюк", ElementType.FOREST, "Танк",
                        "Массивный лесной кабан с непробиваемой шкурой.",
                        85, 20, 35, 10, false),
                new PokemonSpecies("Арсен Воронян", ElementType.FOREST, "Легкий ДПС",
                        "Молниеносный лесной ворон, атакующий первым за счет высокой скорости.",
                        65, 30, 20, 35, false),
                new PokemonSpecies("Культурный медведь", ElementType.FOREST, "Тяжелый брузер",
                        "Могучий хозяин леса с сокрушительным запасом здоровья и силы удара.",
                        85, 35, 25, 5, false)
        );

        repository.saveAll(speciesList);
        log.info("Загружено {} базовых видов покемонов EvoGrid в базу данных.", speciesList.size());
    }
}