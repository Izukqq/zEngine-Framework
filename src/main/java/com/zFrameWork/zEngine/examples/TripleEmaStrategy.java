package com.zFrameWork.zEngine.examples;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradeDirection;
import com.zFrameWork.zEngine.core.strategy.TradingStrategy;

public class TripleEmaStrategy implements TradingStrategy {

    private Map<String, BigDecimal> currentParameters = new HashMap<>();

    // Variables de estado O(1) para no recalcular todo el historial
    private BigDecimal currentEmaGatillo = null;
    private BigDecimal currentEmaFast = null;
    private BigDecimal currentEmaSlow = null;

    // Memoria de la vela anterior para detectar el cruce exacto
    private BigDecimal previousEmaGatillo = null; 

    @Override
    public String getStrategyName() {
        return "Triple_EMA_Gatillo";
    }

    @Override
    public void setParameters(Map<String, BigDecimal> parameters) {
        this.currentParameters = new HashMap<>(parameters);
    }

    @Override
    public Map<String, BigDecimal> getCurrentParameters() {
        return this.currentParameters;
    }

    @Override
    public void resetState() {
        // Vital para limpiar la memoria antes de probar una nueva combinación
        this.currentEmaGatillo = null;
        this.currentEmaFast = null;
        this.currentEmaSlow = null;
        this.previousEmaGatillo = null;
    }

    @Override
    public TradeDirection evaluateEntry(MarketTick tick) {
        updateEmas(tick.getClose());

        // Si aún no tenemos datos suficientes para arrancar, esperamos
        if (currentEmaSlow == null || previousEmaGatillo == null) {
            return TradeDirection.NEUTRAL;
        }

        // Lógica de 3 EMAs Institucional:
        // 1. FILTRO DE TENDENCIA: La Rápida debe estar alineada con la Lenta
        boolean isUptrend = currentEmaFast.compareTo(currentEmaSlow) > 0;
        boolean isDowntrend = currentEmaFast.compareTo(currentEmaSlow) < 0;

        // 2. EL GATILLO: La EMA Gatillo cruza la EMA Rápida en esta vela exacta
        boolean crossUp = previousEmaGatillo.compareTo(currentEmaFast) <= 0 && currentEmaGatillo.compareTo(currentEmaFast) > 0;
        boolean crossDown = previousEmaGatillo.compareTo(currentEmaFast) >= 0 && currentEmaGatillo.compareTo(currentEmaFast) < 0;

        // Si la tendencia general es alcista Y el gatillo cruza hacia arriba -> Compramos
        if (isUptrend && crossUp) {
            return TradeDirection.LONG;
        }
        // Si la tendencia general es bajista Y el gatillo cruza hacia abajo -> Vendemos
        if (isDowntrend && crossDown) {
            return TradeDirection.SHORT;
        }

        return TradeDirection.NEUTRAL;
    }

    @Override
    public boolean evaluateExit(MarketTick tick, TradeDirection currentPosition) {
        updateEmas(tick.getClose());

        if (currentEmaSlow == null) return false;

        // SALIDA TÁCTICA: Cerramos si el Gatillo se devuelve y cruza en contra de nuestra posición
        if (currentPosition == TradeDirection.LONG && currentEmaGatillo.compareTo(currentEmaFast) < 0) {
            return true; 
        }
        if (currentPosition == TradeDirection.SHORT && currentEmaGatillo.compareTo(currentEmaFast) > 0) {
            return true; 
        }

        return false;
    }

    // --- MOTOR MATEMÁTICO DE EMA (Ultrarrápido) ---
    private void updateEmas(BigDecimal closePrice) {
        int pGatillo = currentParameters.get("emaGatillo").intValue();
        int pFast = currentParameters.get("emaFast").intValue();
        int pSlow = currentParameters.get("emaSlow").intValue();

        // Guardamos el gatillo actual como "anterior" antes de sobreescribirlo
        previousEmaGatillo = currentEmaGatillo;

        currentEmaGatillo = calculateEma(closePrice, currentEmaGatillo, pGatillo);
        currentEmaFast = calculateEma(closePrice, currentEmaFast, pFast);
        currentEmaSlow = calculateEma(closePrice, currentEmaSlow, pSlow);
    }

    private BigDecimal calculateEma(BigDecimal close, BigDecimal previousEma, int period) {
        if (previousEma == null) {
            return close; // La primera vela es la base
        }
        // Multiplicador EMA = 2 / (Periodo + 1)
        BigDecimal multiplier = new BigDecimal("2").divide(new BigDecimal(period + 1), 8, RoundingMode.HALF_UP);
        
        // Fórmula EMA = (Close - EMA_Prev) * Multiplier + EMA_Prev
        BigDecimal diff = close.subtract(previousEma);
        return diff.multiply(multiplier).add(previousEma).setScale(8, RoundingMode.HALF_UP);
    }
}