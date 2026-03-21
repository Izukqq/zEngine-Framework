package com.zFrameWork.zEngine.core.indicator.impl;

import com.zFrameWork.zEngine.core.indicator.Indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class EmaIndicator implements Indicator {

    private final String periodParamKey;
    private int period;
    
    private BigDecimal currentEma;
    private BigDecimal previousEma;

    public EmaIndicator(String periodParamKey) {
        this.periodParamKey = periodParamKey;
    }

    @Override
    public void setParameters(Map<String, BigDecimal> params) {
        if (!params.containsKey(periodParamKey)) {
            throw new IllegalArgumentException("Parámetro " + periodParamKey + " no encontrado para EmaIndicator");
        }
        this.period = params.get(periodParamKey).intValue();
    }

    @Override
    public void update(BigDecimal price) {
        this.previousEma = this.currentEma;
        
        if (this.currentEma == null) {
            this.currentEma = price;
        } else {
            BigDecimal multiplier = new BigDecimal("2").divide(new BigDecimal(period + 1), 8, RoundingMode.HALF_UP);
            BigDecimal diff = price.subtract(this.previousEma);
            this.currentEma = diff.multiply(multiplier).add(this.previousEma).setScale(8, RoundingMode.HALF_UP);
        }
    }

    @Override
    public BigDecimal getValue() {
        return this.currentEma;
    }
    
    public BigDecimal getPreviousValue() {
        return this.previousEma;
    }

    @Override
    public void reset() {
        this.currentEma = null;
        this.previousEma = null;
    }
}
