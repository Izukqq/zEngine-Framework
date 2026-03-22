package com.zFrameWork.zEngine.engine.dto;

/**
 * Record inmutable de Java 21+ que encápsula el resultado final (metadata)
 * de haber corrido una prueba de fuerza bruta.
 * Mantiene la capa "engine" aislada de la capa "web".
 */
public record ExecutionSummary(
    String jobId, 
    int totalCombinationsEvaluated, 
    long durationSeconds,
    String completedStrategyName
) {}
