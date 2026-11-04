package com.EvoGrid.common.web;

/** Единый формат ошибки во всех сервисах: {"status": 404, "message": "..."} */
public record ApiError(int status, String message) {}
