package com.zFrameWork.zEngine.examples;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.zFrameWork.zEngine.core.indicator.Indicator;
import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradeDirection;
import com.zFrameWork.zEngine.core.strategy.TradingStrategy;

public class TripleIndicatorStrategy implements TradingStrategy {

    private Map<String, BigDecimal> currentParameters = new HashMap<>();

    // Indicadores independientes inyectados por el constructor (Arquitectura Lego)
    private final String strategyName;
    private final Indicator gatillo;
    private final Indicator fast;
    private final Indicator slow;

    private BigDecimal previousGatillo = null;

    public TripleIndicatorStrategy(String name, Indicator gatillo, Indicator fast, Indicator slow) {
        this.strategyName = name;
        this.gatillo = gatillo;
        this.fast = fast;
        this.slow = slow;
    }

    @Override
    public String getStrategyName() {
        return this.strategyName;
    }

    @Override
    public java.util.List<com.zFrameWork.zEngine.engine.optimizer.ParameterRange> getParameterDefinitions() {
        // Rangos quemados dentro de la estrategia para probar combinaciones
        return java.util.Arrays.asList(
            new com.zFrameWork.zEngine.engine.optimizer.ParameterRange("periodGatillo", new BigDecimal("5"), new BigDecimal("15"), new BigDecimal("5")),
            new com.zFrameWork.zEngine.engine.optimizer.ParameterRange("periodFast", new BigDecimal("20"), new BigDecimal("60"), new BigDecimal("20")),
            new com.zFrameWork.zEngine.engine.optimizer.ParameterRange("periodSlow", new BigDecimal("80"), new BigDecimal("200"), new BigDecimal("40"))
        );
    }

    @Override
    public void setParameters(Map<String, BigDecimal> parameters) {
        this.currentParameters = new HashMap<>(parameters);
        gatillo.setParameters(parameters);
        fast.setParameters(parameters);
        slow.setParameters(parameters);
    }

    @Override
    public Map<String, BigDecimal> getCurrentParameters() {
        return this.currentParameters;
    }

    @Override
    public void resetState() {
        gatillo.reset();
        fast.reset();
        slow.reset();
        this.previousGatillo = null;
    }

    @Override
    public TradeDirection evaluateEntry(MarketTick tick) {
        previousGatillo = gatillo.getValue();

        gatillo.update(tick.getClose());
        fast.update(tick.getClose());
        slow.update(tick.getClose());

        BigDecimal currentGatillo = gatillo.getValue();
        BigDecimal currentFast = fast.getValue();
        BigDecimal currentSlow = slow.getValue();

        if (currentSlow == null || previousGatillo == null || currentFast == null) {
            return TradeDirection.NEUTRAL;
        }

        // 1. FILTRO DE TENDENCIA: Rápida vs Lenta
        boolean isUptrend = currentFast.compareTo(currentSlow) > 0;
        boolean isDowntrend = currentFast.compareTo(currentSlow) < 0;

        // 2. EL GATILLO: Cruce exacto en esta vela
        boolean crossUp = previousGatillo.compareTo(currentFast) <= 0 && currentGatillo.compareTo(currentFast) > 0;
        boolean crossDown = previousGatillo.compareTo(currentFast) >= 0 && currentGatillo.compareTo(currentFast) < 0;

        if (isUptrend && crossUp) {
            return TradeDirection.LONG;
        }
        if (isDowntrend && crossDown) {
            return TradeDirection.SHORT;
        }

        return TradeDirection.NEUTRAL;
    }

    @Override
    public boolean evaluateExit(MarketTick tick, TradeDirection currentPosition) {
        gatillo.update(tick.getClose());
        fast.update(tick.getClose());
        slow.update(tick.getClose());

        BigDecimal currentGatillo = gatillo.getValue();
        BigDecimal currentFast = fast.getValue();
        BigDecimal currentSlow = slow.getValue();

        if (currentSlow == null || currentGatillo == null || currentFast == null) return false;

        // SALIDA TÁCTICA: Cerramos si el Gatillo cruza la M.A Rápida en contra
        if (currentPosition == TradeDirection.LONG && currentGatillo.compareTo(currentFast) < 0) {
            return true; 
        }
        if (currentPosition == TradeDirection.SHORT && currentGatillo.compareTo(currentFast) > 0) {
            return true; 
        }

        return false;
    }
}
