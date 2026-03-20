package com.zFrameWork.zEngine.engine.optimizer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

/**
 * Define los límites y el tamaño del paso para un parámetro específico durante la fuerza bruta.
 */
@Getter
@AllArgsConstructor
public class ParameterRange {
    private final String name;          // Ej: "emaFast"
    private final BigDecimal start;     // Ej: 10
    private final BigDecimal end;       // Ej: 50
    private final BigDecimal step;      // Ej: 5 (Saltará 10, 15, 20... hasta 50)
}