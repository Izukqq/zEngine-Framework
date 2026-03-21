package com.zFrameWork.zEngine.core.indicator.impl;

import com.zFrameWork.zEngine.core.indicator.Indicator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class RsiIndicator implements Indicator {

    private BigDecimal previousPrice = null;
    
    // Usamos Wilder's Smoothing para suavizar las ganancias y las pérdidas
    private final WildersSmoothingIndicator avgGainIndicator;
    private final WildersSmoothingIndicator avgLossIndicator;

    private BigDecimal currentValue = null;

    public RsiIndicator(String periodParamName) {
        // Reutilizamos el Lego de Wilder para calcular promedios
        this.avgGainIndicator = new WildersSmoothingIndicator(periodParamName);
        this.avgLossIndicator = new WildersSmoothingIndicator(periodParamName);
    }

    @Override
    public void setParameters(Map<String, BigDecimal> params) {
        // Propagamos los parámetros a los sub-indicadores
        avgGainIndicator.setParameters(params);
        avgLossIndicator.setParameters(params);
    }

    @Override
    public void update(BigDecimal price) {
        if (previousPrice == null) {
            previousPrice = price;
            // Para la primera vela, inicializamos los promedios en 0
            avgGainIndicator.update(BigDecimal.ZERO);
            avgLossIndicator.update(BigDecimal.ZERO);
            return;
        }

        BigDecimal change = price.subtract(previousPrice);
        BigDecimal gain = BigDecimal.ZERO;
        BigDecimal loss = BigDecimal.ZERO;

        if (change.compareTo(BigDecimal.ZERO) > 0) {
            gain = change;
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            loss = change.abs();
        }

        // Suavizamos
        avgGainIndicator.update(gain);
        avgLossIndicator.update(loss);

        previousPrice = price;

        BigDecimal avgGain = avgGainIndicator.getValue();
        BigDecimal avgLoss = avgLossIndicator.getValue();

        if (avgGain == null || avgLoss == null) {
            return; // Esperando suficientes datos
        }

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            currentValue = new BigDecimal("100.00");
        } else {
            BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
            BigDecimal rsPlusOne = rs.add(BigDecimal.ONE);
            BigDecimal hundred = new BigDecimal("100");
            
            // RSI = 100 - (100 / (1 + RS))
            BigDecimal rsi = hundred.subtract(hundred.divide(rsPlusOne, 8, RoundingMode.HALF_UP));
            currentValue = rsi;
        }
    }

    @Override
    public BigDecimal getValue() {
        return currentValue;
    }

    @Override
    public void reset() {
        this.previousPrice = null;
        this.currentValue = null;
        this.avgGainIndicator.reset();
        this.avgLossIndicator.reset();
    }
}
