package com.zFrameWork.zEngine.examples;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.zFrameWork.zEngine.core.indicator.Indicator;
import com.zFrameWork.zEngine.core.indicator.impl.WildersSmoothingIndicator;
import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradeDirection;
import com.zFrameWork.zEngine.core.strategy.TradingStrategy;

@Component
public class TripleWilderStrategy implements TradingStrategy {

    private Map<String, BigDecimal> currentParameters = new HashMap<>();

    // Indicadores Wilder's independientes (Piezas de Lego)
    private final Indicator gatillo = new WildersSmoothingIndicator("periodGatillo");
    private final Indicator fast = new WildersSmoothingIndicator("periodFast");
    private final Indicator slow = new WildersSmoothingIndicator("periodSlow");

    // Guardaremos el gatillo anterior manualmente para calcular cruces exactos
    private BigDecimal previousGatillo = null;

    @Override
    public String getStrategyName() {
        return "Triple_Wilder_Strategy";
    }

    @Override
    public java.util.List<com.zFrameWork.zEngine.engine.optimizer.ParameterRange> getParameterDefinitions() {
        // (Gatillo: 5 a 15 de 1 en 1) = 11 combinaciones
        // (Rápida: 20 a 50 de 5 en 5) = 7 combinaciones
        // (Lenta: 100 a 200 de 20 en 20) = 6 combinaciones
        // Total = 462 combinaciones únicas
        return java.util.Arrays.asList(
            new com.zFrameWork.zEngine.engine.optimizer.ParameterRange("periodGatillo", new BigDecimal("5"), new BigDecimal("15"), new BigDecimal("1")),
            new com.zFrameWork.zEngine.engine.optimizer.ParameterRange("periodFast", new BigDecimal("20"), new BigDecimal("50"), new BigDecimal("5")),
            new com.zFrameWork.zEngine.engine.optimizer.ParameterRange("periodSlow", new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("20"))
        );
    }

    @Override
    public void setParameters(Map<String, BigDecimal> parameters) {
        this.currentParameters = new HashMap<>(parameters);
        // Propagamos los parámetros a los legos
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
        // Reiniciamos los legos
        gatillo.reset();
        fast.reset();
        slow.reset();
        this.previousGatillo = null;
    }

    @Override
    public TradeDirection evaluateEntry(MarketTick tick) {
        // Guardamos el gatillo anterior antes de actualizar
        previousGatillo = gatillo.getValue();

        gatillo.update(tick.getClose());
        fast.update(tick.getClose());
        slow.update(tick.getClose());

        BigDecimal currentGatillo = gatillo.getValue();
        BigDecimal currentFast = fast.getValue();
        BigDecimal currentSlow = slow.getValue();

        // Si aún no tenemos datos suficientes para arrancar, esperamos
        if (currentSlow == null || previousGatillo == null || currentFast == null) {
            return TradeDirection.NEUTRAL;
        }

        // 1. FILTRO DE TENDENCIA: La Rápida debe estar alineada con la Lenta
        boolean isUptrend = currentFast.compareTo(currentSlow) > 0;
        boolean isDowntrend = currentFast.compareTo(currentSlow) < 0;

        // 2. EL GATILLO: La línea Gatillo cruza la Rápida en esta vela exacta
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

        // SALIDA TÁCTICA: Cerramos si el Gatillo se devuelve y cruza en contra de nuestra posición
        if (currentPosition == TradeDirection.LONG && currentGatillo.compareTo(currentFast) < 0) {
            return true; 
        }
        if (currentPosition == TradeDirection.SHORT && currentGatillo.compareTo(currentFast) > 0) {
            return true; 
        }

        return false;
    }
}
