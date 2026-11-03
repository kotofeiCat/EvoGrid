package com.EvoGrid.battle.service;

import com.EvoGrid.battle.dto.AbilityDto;
import com.EvoGrid.battle.model.AbilityCategory;
import com.EvoGrid.battle.model.StatusEffectType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AbilityCatalog {

    // ID = 1 (NEUTRAL) — Серый грызун
    private static final List<AbilityDto> GRAY_RODENT = List.of(
            attack(1, "QUICK_BITE", "Быстрый укус", "Резкий выпад острыми резцами.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "SWIFT_LUNGE", "Стремительный рывок", "Разгоняется и таранит цель (точность 95%).",
                    AbilityCategory.SPECIAL, "NEUTRAL", 18, 95, 2, false, StatusEffectType.NONE, 0, 0),
            defense(3, "NIMBLE_DODGE", "Юркий кувырок", "Уклонение: 65% шанс полностью избежать следующей атаки.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "FIELD_SNACK", "Запас зёрен", "Восстанавливает 20% от макс. HP.",
                    3, 20, false, 0, StatusEffectType.NONE, 0, 0, StatusEffectType.NONE, 0)
    );

    // ID = 2 (FIRE) — Красная панда
    private static final List<AbilityDto> RED_PANDA = List.of(
            attack(1, "HOT_CLAW", "Раскалённый коготь", "Быстрый удар горящей лапой.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "FIRE_ARC", "Пламенная дуга", "Огненный STAB-удар, накладывающий Ожог (2 хода).",
                    AbilityCategory.SPECIAL, "FIRE", 16, 100, 2, true, StatusEffectType.BURN, 100, 2),
            defense(3, "SPARK_DASH", "Искровой рывок", "Уклонение: 65% шанс избежать атаки.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "VOLCANO_WARMTH", "Тепло вулкана", "Повышает Атаку (+30%) и даёт Огненные шипы (10% возврата урона) на 2 хода.",
                    3, 0, false, 0, StatusEffectType.ATK_UP, 100, 2, StatusEffectType.THORNS, 2)
    );

    // ID = 3 (FIRE) — Огнеборец
    private static final List<AbilityDto> FIRE_BRAWLER = List.of(
            attack(1, "DIRECT_JAB", "Прямой джеб", "Тяжёлый пробивной удар кулаком.",
                    AbilityCategory.BASIC, "NEUTRAL", 12, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "FOUR_ARM_BARRAGE", "Шквал четырёх рук", "Сокрушительная серия огненных ударов (STAB).",
                    AbilityCategory.SPECIAL, "FIRE", 22, 100, 2, true, StatusEffectType.NONE, 0, 0),
            defense(3, "CROSS_BLOCK", "Перекрёстный блок", "Снижает входящий урон на 70%.",
                    3, 70, 0, StatusEffectType.BLOCK, 1),
            utility(4, "VOLCANIC_RAGE", "Вулканический напор", "Жертвует 10% HP: удваивает Скорость (x2) и пробивает блоки (2 хода).",
                    3, 0, false, 10, StatusEffectType.RAGE_SPEED_PIERCE, 100, 2, StatusEffectType.NONE, 0)
    );

    // ID = 4 (FIRE) — Пеплоух
    private static final List<AbilityDto> CINDERHOOT = List.of(
            attack(1, "TALON_GRIP", "Хватка когтей", "Резкий удар когтями с налёту.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "SMOLDER_SWEEP", "Пепельный вихрь", "Огненный STAB-удар, ослепляющий цель (-35% точности на 2 хода).",
                    AbilityCategory.SPECIAL, "FIRE", 16, 100, 2, true, StatusEffectType.BLIND, 100, 2),
            defense(3, "SMOKE_SCREEN", "Дымовая завеса", "Уклонение: 65% шанс избежать следующей атаки.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "ASH_SECOND_WIND", "Второе дыхание пепла", "Восстанавливает 22% HP (или 44%, если HP ниже 25%).",
                    4, 22, true, 0, StatusEffectType.NONE, 0, 0, StatusEffectType.NONE, 0)
    );

    // ID = 5 (FIRE) — Пиромандра
    private static final List<AbilityDto> PYROMANDRA = List.of(
            attack(1, "WHIP_TAIL", "Хлесткий хвост", "Размашистый удар тяжёлым хвостом.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "MAGMA_SPLASH", "Всплеск магмы", "Огненный STAB-удар, замедляющий врага (-35% скорости на 2 хода).",
                    AbilityCategory.SPECIAL, "FIRE", 16, 100, 2, true, StatusEffectType.SLOW, 100, 2),
            defense(3, "BASALT_SCALES", "Базальтовая чешуя", "Каменная броня снижает входящий урон на 60%.",
                    3, 60, 0, StatusEffectType.BLOCK, 1),
            utility(4, "LAVA_BATH", "Лавовый прогрев", "Восстанавливает 22% HP и заряжает следующую атаку гарантированным Ожогом.",
                    3, 22, false, 0, StatusEffectType.NEXT_HIT_BURN, 100, 2, StatusEffectType.NONE, 0)
    );

    // ID = 6 (WATER) — Речная выдра
    private static final List<AbilityDto> RIVER_OTTER = List.of(
            attack(1, "WATER_SLAP", "Водный всплеск", "Быстрый водный удар с бонусом STAB.",
                    AbilityCategory.BASIC, "WATER", 10, 100, 0, true, StatusEffectType.NONE, 0, 0),
            attack(2, "WAVE_WHIP", "Приливная волна", "Мощный водный STAB-удар, оглушающий противника (1 ход).",
                    AbilityCategory.SPECIAL, "WATER", 16, 100, 3, true, StatusEffectType.STUN, 100, 1),
            defense(3, "WATER_SURFACE", "Водная гладь", "Уклонение: 65% шанс уйти под воду от атаки.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "SHELL_BARRIER", "Ракушечный щит", "Полностью блокирует следующую вражескую атаку (0 урона).",
                    4, 0, false, 0, StatusEffectType.SHELL_BARRIER, 100, 1, StatusEffectType.NONE, 0)
    );

    // ID = 7 (WATER) — Сэр Сомуэль
    private static final List<AbilityDto> SIR_SOMUEL = List.of(
            attack(1, "CANE_STRIKE", "Удар тростью", "Точный фехтовальный выпад тростью.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "PIPE_GEYSER", "Напорный гейзер", "Мощный выброс водяной струи под давлением (STAB).",
                    AbilityCategory.SPECIAL, "WATER", 18, 100, 2, true, StatusEffectType.NONE, 0, 0),
            defense(3, "HAT_WARD", "Защита цилиндра", "Снижает урон на 45% и даёт иммунитет к критам (2 хода).",
                    3, 45, 0, StatusEffectType.CRIT_IMMUNE, 2),
            utility(4, "AROMATIC_TEA", "Ароматный чай", "Восстанавливает 10% HP, снимает все дебаффы и даёт Регенерацию (3 хода).",
                    4, 10, false, 0, StatusEffectType.CLEANSE, 100, 1, StatusEffectType.REGEN, 3)
    );

    // ID = 8 (WATER) — Плескан
    private static final List<AbilityDto> SPLASHICAN = List.of(
            attack(1, "WING_CLAP", "Хлопок крылом", "Широкий удар мощными крыльями.",
                    AbilityCategory.BASIC, "NEUTRAL", 11, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "WATER_VOLLEY", "Водяной залп", "Прицельный гидроудар с высоты (STAB).",
                    AbilityCategory.SPECIAL, "WATER", 20, 100, 2, true, StatusEffectType.NONE, 0, 0),
            defense(3, "AIR_SOMERSAULT", "Воздушный вираж", "Уклонение: 65% шанс избежать атаки в воздухе.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "LIVING_RESERVOIR", "Водный резервуар", "Восстанавливает 20% HP и усиливает следующую водную атаку на +35%.",
                    3, 20, false, 0, StatusEffectType.WATER_BOOST, 100, 2, StatusEffectType.NONE, 0)
    );

    // ID = 9 (WATER) — Касторакс
    private static final List<AbilityDto> CASTORAX = List.of(
            attack(1, "INCISOR_STRIKE", "Удар резцами", "Крепкий укус, способный перегрызть ствол дерева.",
                    AbilityCategory.BASIC, "NEUTRAL", 12, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "DAM_SMASH", "Прорыв плотины", "Сокрушительная волна водной стихии (STAB).",
                    AbilityCategory.SPECIAL, "WATER", 21, 100, 2, true, StatusEffectType.NONE, 0, 0),
            defense(3, "LOG_SHIELD", "Бревенчатый щит", "Плотина снижает входящий урон на 55%.",
                    3, 55, 0, StatusEffectType.BLOCK, 1),
            utility(4, "EXPRESS_REPAIR", "Срочный ремонт", "Восстанавливает 25% HP и даёт иммунитет к контролю (2 хода).",
                    3, 25, false, 0, StatusEffectType.CC_IMMUNE, 100, 2, StatusEffectType.NONE, 0)
    );

    // ID = 10 (FOREST) — Лесной страж
    private static final List<AbilityDto> FOREST_GUARDIAN = List.of(
            attack(1, "HOOF_STRIKE", "Удар копытом", "Быстрый и точный выпад передними копытами.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "FERN_WHIRLWIND", "Листопадный вихрь", "Шквал острых листьев и ветвей (STAB).",
                    AbilityCategory.SPECIAL, "FOREST", 19, 100, 2, true, StatusEffectType.NONE, 0, 0),
            defense(3, "FOREST_STEP", "Лесная поступь", "Уклонение: 65% шанс раствориться среди листвы.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "ANTLER_BLOOM", "Цветение кроны", "Восстанавливает 18% HP, очищает дебаффы и с шансом 55% усыпляет врага.",
                    4, 18, false, 0, StatusEffectType.SLEEP, 55, 1, StatusEffectType.CLEANSE, 1)
    );

    // ID = 11 (FOREST) — Лесной вепрь
    private static final List<AbilityDto> FOREST_BOAR = List.of(
            attack(1, "TUSK_RAM", "Таран клыками", "Мощный наскок с разбега.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "SPORE_VOLLEY", "Споровый залп", "Лесной STAB-удар, отравляющий противника Ядом (3 хода).",
                    AbilityCategory.SPECIAL, "FOREST", 15, 100, 2, true, StatusEffectType.POISON, 100, 3),
            defense(3, "MOSS_ARMOR", "Моховая броня", "Плотный слой мха снижает входящий урон на 50%.",
                    3, 50, 0, StatusEffectType.BLOCK, 1),
            utility(4, "TRUFFLE_SNACK", "Лесной трюфель", "Находит редкий корень и восстанавливает 35% от макс. HP.",
                    4, 35, false, 0, StatusEffectType.NONE, 0, 0, StatusEffectType.NONE, 0)
    );

    // ID = 12 (FOREST) — Ворон-сыщик
    private static final List<AbilityDto> DETECTIVE_RAVEN = List.of(
            attack(1, "DIVE_PECK", "Пикирующий клевок", "Стремительная атака клювом с разворота.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "BRAMBLE_TRAP", "Терновая ловушка", "Лесной STAB-удар, опутывающий и замедляющий цель (-35% скорости на 2 хода).",
                    AbilityCategory.SPECIAL, "FOREST", 16, 100, 2, true, StatusEffectType.SLOW, 100, 2),
            defense(3, "SHADOW_FLAP", "Взмах в тени", "Уклонение: 65% шанс скрыться в тени ветвей.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "MONOCLE_ANALYSIS", "Дедукция монокля", "Находит уязвимость: следующая атака нанесёт гарантированный крит (x1.5).",
                    3, 0, false, 0, StatusEffectType.GUARANTEED_CRIT, 100, 2, StatusEffectType.NONE, 0)
    );

    // ID = 13 (FOREST) — Культурный медведь
    private static final List<AbilityDto> CULTURED_BEAR = List.of(
            attack(1, "HEAVY_PAW", "Тяжелая лапа", "Размашистый удар медвежьей лапой.",
                    AbilityCategory.BASIC, "NEUTRAL", 12, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "FERN_SWING", "Папоротниковый размах", "Сокрушительный лесной удар (STAB).",
                    AbilityCategory.SPECIAL, "FOREST", 22, 100, 2, true, StatusEffectType.NONE, 0, 0),
            defense(3, "WOODEN_CARAPACE", "Древесный панцирь", "Древесная кора снижает входящий урон на 60%.",
                    3, 60, 0, StatusEffectType.BLOCK, 1),
            utility(4, "TEA_ETIQUETTE", "Чайный этикет", "Восстанавливает 20% HP и повышает Защиту на +35% (2 хода).",
                    3, 20, false, 0, StatusEffectType.DEF_UP, 100, 2, StatusEffectType.NONE, 0)
    );

    private static final Map<Long, List<AbilityDto>> BY_SPECIES_ID = Map.ofEntries(
            Map.entry(1L, GRAY_RODENT),
            Map.entry(2L, RED_PANDA),
            Map.entry(3L, FIRE_BRAWLER),
            Map.entry(4L, CINDERHOOT),
            Map.entry(5L, PYROMANDRA),
            Map.entry(6L, RIVER_OTTER),
            Map.entry(7L, SIR_SOMUEL),
            Map.entry(8L, SPLASHICAN),
            Map.entry(9L, CASTORAX),
            Map.entry(10L, FOREST_GUARDIAN),
            Map.entry(11L, FOREST_BOAR),
            Map.entry(12L, DETECTIVE_RAVEN),
            Map.entry(13L, CULTURED_BEAR)
    );

    public Map<Long, List<AbilityDto>> getAllAbilitiesMap() {
        return BY_SPECIES_ID;
    }

    public List<AbilityDto> getAbilitiesForPokemon(Long speciesId, String pokemonName) {
        if (speciesId != null && BY_SPECIES_ID.containsKey(speciesId)) {
            return BY_SPECIES_ID.get(speciesId);
        }
        if (pokemonName != null) {
            String norm = pokemonName.trim().toLowerCase(Locale.ROOT);
            if (norm.contains("панда")) return RED_PANDA;
            if (norm.contains("огнеборец")) return FIRE_BRAWLER;
            if (norm.contains("пеплоух")) return CINDERHOOT;
            if (norm.contains("пиромандра")) return PYROMANDRA;
            if (norm.contains("выдра")) return RIVER_OTTER;
            if (norm.contains("сомуэль")) return SIR_SOMUEL;
            if (norm.contains("плескан")) return SPLASHICAN;
            if (norm.contains("касторакс")) return CASTORAX;
            if (norm.contains("страж")) return FOREST_GUARDIAN;
            if (norm.contains("вепрь")) return FOREST_BOAR;
            if (norm.contains("ворон")) return DETECTIVE_RAVEN;
            if (norm.contains("медведь")) return CULTURED_BEAR;
        }
        return GRAY_RODENT;
    }

    public AbilityDto getAbilityBySlot(Long speciesId, String pokemonName, int slot) {
        List<AbilityDto> list = getAbilitiesForPokemon(speciesId, pokemonName);
        return list.stream()
                .filter(a -> a.slot() == slot)
                .findFirst()
                .orElse(list.get(0));
    }

    private static AbilityDto attack(int slot, String code, String name, String desc,
                                     AbilityCategory cat, String element, int power, int accuracy,
                                     int cooldown, boolean stab,
                                     StatusEffectType effect, int effectChance, int duration) {
        return new AbilityDto(
                slot, code, name, desc, cat, element, "ENEMY",
                power, accuracy, cooldown, 0, stab,
                0, 0, 0, false, 0,
                effect, effectChance, duration, StatusEffectType.NONE, 0
        );
    }

    private static AbilityDto defense(int slot, String code, String name, String desc,
                                      int cooldown, int blockPercent, int evasionPercent,
                                      StatusEffectType extraEffect, int duration) {
        return new AbilityDto(
                slot, code, name, desc, AbilityCategory.DEFENSE, "NEUTRAL", "SELF",
                0, 100, cooldown, 0, false,
                blockPercent, evasionPercent, 0, false, 0,
                extraEffect, 100, duration, StatusEffectType.NONE, 0
        );
    }

    private static AbilityDto utility(int slot, String code, String name, String desc,
                                      int cooldown, int healPercent, boolean doubleLowHp, int selfDmgPercent,
                                      StatusEffectType primary, int primaryChance, int primaryDur,
                                      StatusEffectType secondary, int secondaryDur) {
        return new AbilityDto(
                slot, code, name, desc, AbilityCategory.UTILITY, "NEUTRAL", "SELF",
                0, 100, cooldown, 0, false,
                0, 0, healPercent, doubleLowHp, selfDmgPercent,
                primary, primaryChance, primaryDur, secondary, secondaryDur
        );
    }
}