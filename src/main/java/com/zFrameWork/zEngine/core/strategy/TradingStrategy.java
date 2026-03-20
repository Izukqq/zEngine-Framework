package com.zFrameWork.zEngine.core.strategy;

import java.math.BigDecimal;
import java.util.Map;

public interface TradingStrategy {

    String getStrategyName();

    void setParameters(Map<String, BigDecimal> parameters);

    Map<String, BigDecimal> getCurrentParameters();

    /**
     * Devuelve la dirección de la operación (LONG, SHORT, NEUTRAL)
     */
    TradeDirection evaluateEntry(MarketTick tick);

    /**
     * Evalúa la salida pasándole la posición actual
     */
    boolean evaluateExit(MarketTick tick, TradeDirection currentPosition);
    
    void resetState();
}