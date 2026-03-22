package com.zFrameWork.zEngine.web.dto;

/**
 * Data Transfer Object inmutable (Java 17 Record) diseñado para
 * transferir la identidad de las estrategias al Frontend React
 * de una manera segura y plana.
 */
public record StrategyDto(String id, String name) {
}
