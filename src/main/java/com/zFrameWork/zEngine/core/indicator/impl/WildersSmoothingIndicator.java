package com.zFrameWork.zEngine.core.indicator.impl;

import com.zFrameWork.zEngine.core.indicator.Indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class WildersSmoothingIndicator implements Indicator {

    private final String periodParamKey;
    private int period;
    
    private BigDecimal currentValue;
    private BigDecimal previousValue;

    public WildersSmoothingIndicator(String periodParamKey) {
        this.periodParamKey = periodParamKey;
    }

    @Override
    public void setParameters(Map<String, BigDecimal> params) {
        if (!params.containsKey(periodParamKey)) {
            throw new IllegalArgumentException("Parámetro " + periodParamKey + " no encontrado para WildersSmoothingIndicator");
        }
        this.period = params.get(periodParamKey).intValue();
    }

    @Override
    public void update(BigDecimal price) {
        this.previousValue = this.currentValue;
        
        if (this.currentValue == null) {
            this.currentValue = price;
        } else {
            BigDecimal divisor = new BigDecimal(period);
            BigDecimal diff = price.subtract(this.previousValue);
            BigDecimal addition = diff.divide(divisor, 8, RoundingMode.HALF_UP);
            this.currentValue = this.previousValue.add(addition).setScale(8, RoundingMode.HALF_UP);
        }
    }

    @Override
    public BigDecimal getValue() {
        return this.currentValue;
    }
    
    public BigDecimal getPreviousValue() {
        return this.previousValue;
    }

    @Override
    public void reset() {
        this.currentValue = null;
        this.previousValue = null;
    }
}
