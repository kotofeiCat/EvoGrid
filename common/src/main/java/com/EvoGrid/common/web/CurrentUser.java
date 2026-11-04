package com.EvoGrid.common.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ID текущего игрока. Шлюз проверяет JWT и кладёт ID в заголовок {@code X-User-Id};
 * в контроллере достаточно написать {@code @CurrentUser Long userId}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
