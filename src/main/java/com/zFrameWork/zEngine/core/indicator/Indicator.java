package com.zFrameWork.zEngine.core.indicator;

import java.math.BigDecimal;
import java.util.Map;

public interface Indicator {
    /**
     * Inyecta los parámetros generales de la estrategia para que el indicador se configure (ej. "periodos").
     */
    void setParameters(Map<String, BigDecimal> params);
    
    /**
     * Actualiza el estado del indicador con un nuevo precio de cierre.
     */
    void update(BigDecimal price);
    
    /**
     * Retorna el valor actual evaluado por este indicador.
     */
    BigDecimal getValue();
    
    /**
     * Reinicia el estado interno del indicador (requerido para cada iteración de backtest).
     */
    void reset();
}
