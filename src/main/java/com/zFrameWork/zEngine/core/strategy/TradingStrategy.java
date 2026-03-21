package com.zFrameWork.zEngine.core.strategy;

import java.math.BigDecimal;
import java.util.Map;

public interface TradingStrategy {
    // El motor le pasa un mapa genérico (ej: "periodo" -> 14)
    void setParameters(Map<String, BigDecimal> params);
    
    // La estrategia expone qué parámetros está usando actualmente
    Map<String, BigDecimal> getCurrentParameters();

    TradeDirection evaluateEntry(MarketTick tick);
    boolean evaluateExit(MarketTick tick, TradeDirection currentDirection);
    
    void resetState();
    String getStrategyName();

    // Nueva responsabilidad: La estrategia le dice al motor qué parámetros probar
    java.util.List<com.zFrameWork.zEngine.engine.optimizer.ParameterRange> getParameterDefinitions();
}