package com.EvoGrid.common.web;

/**
 * Префиксы адресов.
 * <ul>
 *   <li>{@code /api/**} — публичное API: шлюз пропускает его наружу и проверяет JWT;</li>
 *   <li>{@code /internal/**} — только для вызовов между сервисами: в шлюзе нет такого маршрута,
 *       а порты сервисов закрыты внутри Docker-сети.</li>
 * </ul>
 */
public final class ApiPaths {

    public static final String PUBLIC = "/api";
    public static final String INTERNAL = "/internal";

    private ApiPaths() {
    }
}
