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

    // Базовый крип: Слепой крысеныш / Крысюк (ID = 1)
    private static final List<AbilityDto> RAT_CREEP = List.of(
            attack(1, "RAT_BITE", "Укус крысы", "Быстрый нейтральный укус без перезарядки.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "RAT_LUNGE", "Яростный наскок", "Усиленный прыжок на врага.",
                    AbilityCategory.SPECIAL, "NEUTRAL", 18, 95, 2, false, StatusEffectType.NONE, 0, 0),
            defense(3, "RAT_DODGE", "Крысиный перекат", "Уворот: 65% шанс избежать следующей атаки.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "RAT_SNACK", "Полевой перекус", "Восстанавливает 20% от макс. HP.",
                    3, 20, false, 0, StatusEffectType.NONE, 0, 0, StatusEffectType.NONE, 0)
    );

    // 1. Красная панда (ID = 2, FIRE)
    private static final List<AbilityDto> RED_PANDA = List.of(
            attack(1, "HOT_CLAW", "Горячий коготь", "Обычная атака когтями.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "FIRE_ARC", "Огненная дуга", "Огненная STAB-атака. Накладывает Горение (2 хода).",
                    AbilityCategory.SPECIAL, "FIRE", 16, 100, 2, true, StatusEffectType.BURN, 100, 2),
            defense(3, "SPARK_DASH", "Искрящийся рывок", "Уворот: 65% шанс промаха врага на этом ходу.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "VOLCANO_WARMTH", "Тепло вулкана", "Бафф атаки (+30%) и огненные шипы (10% урона обратно) на 2 хода.",
                    3, 0, false, 0, StatusEffectType.ATK_UP, 100, 2, StatusEffectType.THORNS, 2)
    );

    // 2. Пихач (ID = 3, FIRE)
    private static final List<AbilityDto> PIHACH = List.of(
            attack(1, "DIRECT_JAB", "Прямой джеб", "Тяжелый прямой удар кулаком.",
                    AbilityCategory.BASIC, "NEUTRAL", 12, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "FOUR_ARM_BARRAGE", "Четырехрукий шквал", "Мощная серия огненных ударов (STAB).",
                    AbilityCategory.SPECIAL, "FIRE", 22, 100, 2, true, StatusEffectType.NONE, 0, 0),
            defense(3, "CROSS_BLOCK", "Крестовой блок", "Блок четырех рук: снижает входящий урон на 70%.",
                    3, 70, 0, StatusEffectType.BLOCK, 1),
            utility(4, "VOLCANIC_RAGE", "Вулканический раж", "Жертвует 10% HP: удваивает скорость (x2) и пробивает блок 2 хода.",
                    3, 0, false, 10, StatusEffectType.RAGE_SPEED_PIERCE, 100, 2, StatusEffectType.NONE, 0)
    );

    // 3. Синдерхут (ID = 4, FIRE)
    private static final List<AbilityDto> CINDERHOOT = List.of(
            attack(1, "TALON_GRIP", "Когтистый хват", "Стремительный удар когтями.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "SMOLDER_SWEEP", "Тлеющий взмах", "Огненная STAB-атака. Ослепляет врага (-35% точности на 2 хода).",
                    AbilityCategory.SPECIAL, "FIRE", 16, 100, 2, true, StatusEffectType.BLIND, 100, 2),
            defense(3, "SMOKE_SCREEN", "Дымовая завеса", "Уворот в дыму: 65% шанс промаха врага.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "ASH_SECOND_WIND", "Второе дыхание из пепла", "Лечит 22% HP (удвоенное лечение 44%, если HP ниже 25%).",
                    4, 22, true, 0, StatusEffectType.NONE, 0, 0, StatusEffectType.NONE, 0)
    );

    // 4. Шаломандра (ID = 5, FIRE)
    private static final List<AbilityDto> SHALOMANDRA = List.of(
            attack(1, "WHIP_TAIL", "Хлесткий хвост", "Базовый удар тяжелым хвостом.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "MAGMA_SPLASH", "Магматический всплеск", "Огненная STAB-атака. Замедляет цель (-35% скорости на 2 хода).",
                    AbilityCategory.SPECIAL, "FIRE", 16, 100, 2, true, StatusEffectType.SLOW, 100, 2),
            defense(3, "BASALT_SCALES", "Базальтовая чешуя", "Каменный панцирь: уменьшает урон на 60%.",
                    3, 60, 0, StatusEffectType.BLOCK, 1),
            utility(4, "LAVA_BATH", "Лавовое купание", "Лечит 22% HP и заряжает следующий удар гарантированным Горением.",
                    3, 22, false, 0, StatusEffectType.NEXT_HIT_BURN, 100, 2, StatusEffectType.NONE, 0)
    );

    // 5. Выдрочка (ID = 6, WATER)
    private static final List<AbilityDto> VYDROCHKA = List.of(
            attack(1, "WATER_SLAP", "Водный шлепок", "Базовая водная атака с бонусом STAB.",
                    AbilityCategory.BASIC, "WATER", 10, 100, 0, true, StatusEffectType.NONE, 0, 0),
            attack(2, "WAVE_WHIP", "Волновой хлыст", "Водная STAB-атака. Сбивает с ног (Оглушение на 1 ход).",
                    AbilityCategory.SPECIAL, "WATER", 16, 100, 3, true, StatusEffectType.STUN, 100, 1),
            defense(3, "WATER_SURFACE", "Водная гладь", "Скольжение по воде: 65% шанс уворота.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "SHELL_BARRIER", "Ракушечный барьер", "Полностью поглощает урон от следующего удара противника.",
                    4, 0, false, 0, StatusEffectType.SHELL_BARRIER, 100, 1, StatusEffectType.NONE, 0)
    );

    // 6. Сэр Сомуэль (ID = 7, WATER)
    private static final List<AbilityDto> SIR_SOMUEL = List.of(
            attack(1, "CANE_STRIKE", "Удар тростью", "Точный фехтовальный выпад тростью.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "PIPE_GEYSER", "Гейзер из трубки", "Мощный поток кипятка под давлением (STAB).",
                    AbilityCategory.SPECIAL, "WATER", 18, 100, 2, true, StatusEffectType.NONE, 0, 0),
            defense(3, "HAT_WARD", "Шляпный заслон", "Блок 45% урона + иммунитет к критическим ударам на 2 хода.",
                    3, 45, 0, StatusEffectType.CRIT_IMMUNE, 2),
            utility(4, "AROMATIC_TEA", "Ароматный чай", "Снимает все дебаффы и восстанавливает по 10% HP в течение 3 ходов.",
                    4, 10, false, 0, StatusEffectType.CLEANSE, 100, 1, StatusEffectType.REGEN, 3)
    );

    // 7. Сплэшикан (ID = 8, WATER)
    private static final List<AbilityDto> SPLASHICAN = List.of(
            attack(1, "WING_CLAP", "Хлопок крылом", "Размашистый удар крылом.",
                    AbilityCategory.BASIC, "NEUTRAL", 11, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "WATER_VOLLEY", "Водяной залп", "Залп морской водой из клюва (STAB).",
                    AbilityCategory.SPECIAL, "WATER", 20, 100, 2, true, StatusEffectType.NONE, 0, 0),
            defense(3, "AIR_SOMERSAULT", "Воздушный кульбит", "Уворот в воздухе: 65% шанс промаха врага.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "LIVING_RESERVOIR", "Живой резервуар", "Лечит 20% HP и усиливает следующую водную атаку на +35%.",
                    3, 20, false, 0, StatusEffectType.WATER_BOOST, 100, 2, StatusEffectType.NONE, 0)
    );

    // 8. Касторакс (ID = 9, WATER)
    private static final List<AbilityDto> CASTORAX = List.of(
            attack(1, "INCISOR_STRIKE", "Удар резцами", "Мощный укус крепкими резцами.",
                    AbilityCategory.BASIC, "NEUTRAL", 12, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "DAM_SMASH", "Удар плотиной", "Сокрушительный водный удар хвостом-веслом (STAB).",
                    AbilityCategory.SPECIAL, "WATER", 21, 100, 2, true, StatusEffectType.NONE, 0, 0),
            defense(3, "LOG_SHIELD", "Бревенчатый щит", "Блок бревном: снижает входящий урон на 55%.",
                    3, 55, 0, StatusEffectType.BLOCK, 1),
            utility(4, "EXPRESS_REPAIR", "Экспресс-ремонт", "Лечит 25% HP и дает иммунитет к контролю и дебаффам на 2 хода.",
                    3, 25, false, 0, StatusEffectType.CC_IMMUNE, 100, 2, StatusEffectType.NONE, 0)
    );

    // 9. Хранитель Леса / Рогатый Куколд (ID = 10, FOREST)
    private static final List<AbilityDto> FOREST_GUARDIAN = List.of(
            attack(1, "HOOF_STRIKE", "Удар копытцем", "Стремительный наскок копытами.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "FERN_WHIRLWIND", "Папоротниковый вихрь", "Режущий лесной шквал (STAB).",
                    AbilityCategory.SPECIAL, "FOREST", 19, 100, 2, true, StatusEffectType.NONE, 0, 0),
            defense(3, "FOREST_STEP", "Лесной шаг", "Уворот среди листвы: 65% шанс промаха врага.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "ANTLER_BLOOM", "Цветение рогов", "Лечит 18% HP, снимает дебаффы и с шансом 55% усыпляет врага на 1 ход.",
                    4, 18, false, 0, StatusEffectType.SLEEP, 55, 1, StatusEffectType.CLEANSE, 1)
    );

    // 10. Свинохрюк (ID = 11, FOREST)
    private static final List<AbilityDto> SVINOHRYUK = List.of(
            attack(1, "TUSK_RAM", "Таран бивнями", "Прямой таран массивными клыками.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "SPORE_VOLLEY", "Споровый залп", "Лесная STAB-атака. Отравляет врага Ядом на 3 хода.",
                    AbilityCategory.SPECIAL, "FOREST", 15, 100, 2, true, StatusEffectType.POISON, 100, 3),
            defense(3, "MOSS_ARMOR", "Мшистая броня", "Плотный мох: снижает входящий урон на 50%.",
                    3, 50, 0, StatusEffectType.BLOCK, 1),
            utility(4, "TRUFFLE_SNACK", "Трюфельный перекус", "Сильное исцеление: восстанавливает 35% от макс. HP.",
                    4, 35, false, 0, StatusEffectType.NONE, 0, 0, StatusEffectType.NONE, 0)
    );

    // 11. Арсен Воронян (ID = 12, FOREST)
    private static final List<AbilityDto> ARSEN_VORONYAN = List.of(
            attack(1, "DIVE_PECK", "Клевок с пике", "Молниеносный удар клювом сверху.",
                    AbilityCategory.BASIC, "NEUTRAL", 10, 100, 0, false, StatusEffectType.NONE, 0, 0),
            attack(2, "BRAMBLE_TRAP", "Ежевичный капкан", "Лесная STAB-атака. Замедляет врага (-35% скорости на 2 хода).",
                    AbilityCategory.SPECIAL, "FOREST", 16, 100, 2, true, StatusEffectType.SLOW, 100, 2),
            defense(3, "SHADOW_FLAP", "Теневой взмах", "Уход в тень: 65% шанс уворота от следующей атаки.",
                    3, 0, 65, StatusEffectType.EVASION, 1),
            utility(4, "MONOCLE_ANALYSIS", "Анализ монокля", "Вычисляет уязвимость: следующий удар нанесет гарантированный крит (x1.5).",
                    3, 0, false, 0, StatusEffectType.GUARANTEED_CRIT, 100, 2, StatusEffectType.NONE, 0)
    );

    // 12. Культурный медведь (ID = 13, FOREST)
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
            Map.entry(1L, RAT_CREEP),
            Map.entry(2L, RED_PANDA),
            Map.entry(3L, PIHACH),
            Map.entry(4L, CINDERHOOT),
            Map.entry(5L, SHALOMANDRA),
            Map.entry(6L, VYDROCHKA),
            Map.entry(7L, SIR_SOMUEL),
            Map.entry(8L, SPLASHICAN),
            Map.entry(9L, CASTORAX),
            Map.entry(10L, FOREST_GUARDIAN),
            Map.entry(11L, SVINOHRYUK),
            Map.entry(12L, ARSEN_VORONYAN),
            Map.entry(13L, CULTURED_BEAR)
    );

    public List<AbilityDto> getAbilitiesForPokemon(Long speciesId, String pokemonName) {
        if (speciesId != null && BY_SPECIES_ID.containsKey(speciesId)) {
            return BY_SPECIES_ID.get(speciesId);
        }
        if (pokemonName != null) {
            String norm = pokemonName.trim().toLowerCase(Locale.ROOT);
            if (norm.contains("панда") || norm.contains("пиролис") || norm.contains("лис")) return RED_PANDA;
            if (norm.contains("пихач")) return PIHACH;
            if (norm.contains("синдерхут")) return CINDERHOOT;
            if (norm.contains("шаломандра")) return SHALOMANDRA;
            if (norm.contains("выдрочка")) return VYDROCHKA;
            if (norm.contains("сомуэль")) return SIR_SOMUEL;
            if (norm.contains("сплэшикан")) return SPLASHICAN;
            if (norm.contains("касторакс")) return CASTORAX;
            if (norm.contains("хранитель") || norm.contains("куколд")) return FOREST_GUARDIAN;
            if (norm.contains("свинохрюк")) return SVINOHRYUK;
            if (norm.contains("воронян") || norm.contains("арсен")) return ARSEN_VORONYAN;
            if (norm.contains("медведь")) return CULTURED_BEAR;
        }
        return RAT_CREEP;
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