package com.EvoGrid.battle.model;

public enum AbilityCategory {
    BASIC,      // Слот 1: Обычная атака (без перезарядки)
    SPECIAL,    // Слот 2: Особая стихийная атака (STAB + дебафф/контроль)
    DEFENSE,    // Слот 3: Защитное действие (Блок или Уворот 65%)
    UTILITY     // Слот 4: Спец-прием (Лечение, Бафф, Контр-механика)
}