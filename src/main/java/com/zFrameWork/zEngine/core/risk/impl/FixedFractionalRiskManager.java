package com.zFrameWork.zEngine.core.risk.impl;

import com.zFrameWork.zEngine.core.risk.RiskManager;
import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradeDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FixedFractionalRiskManager implements RiskManager {
    
    private final BigDecimal riskPercentage = new BigDecimal("0.015"); // 1.5% del capital total
    private final BigDecimal stopLossPercentage = new BigDecimal("0.02"); // 2% de Stop Loss relativo al precio

    @Override
    public void reset(BigDecimal initialCapital) {
        // En este modelo sencillo no guardamos estado de drawdown acumulado en el RiskManager,
        // el motor de backtest lleva el control del balance actual.
    }

    @Override
    public BigDecimal calculatePositionSize(BigDecimal entryPrice, BigDecimal currentCapital) {
        BigDecimal capitalAtRisk = currentCapital.multiply(riskPercentage);
        BigDecimal priceAtRisk = entryPrice.multiply(stopLossPercentage);
        if (priceAtRisk.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        
        // Cantidad de unidades a comprar
        return capitalAtRisk.divide(priceAtRisk, 8, RoundingMode.HALF_UP);
    }

    @Override
    public boolean hitStopLoss(MarketTick tick, TradeDirection currentDirection, BigDecimal entryPrice) {
        if (currentDirection == TradeDirection.LONG) {
            BigDecimal stopPrice = entryPrice.multiply(BigDecimal.ONE.subtract(stopLossPercentage));
            return tick.getLow().compareTo(stopPrice) <= 0;
        } else if (currentDirection == TradeDirection.SHORT) {
            BigDecimal stopPrice = entryPrice.multiply(BigDecimal.ONE.add(stopLossPercentage));
            return tick.getHigh().compareTo(stopPrice) >= 0;
        }
        return false;
    }

    @Override
    public BigDecimal calculateProfitLoss(TradeDirection currentDirection, BigDecimal entryPrice, BigDecimal exitPrice, BigDecimal positionSize) {
        BigDecimal diff = (currentDirection == TradeDirection.LONG) 
                ? exitPrice.subtract(entryPrice) 
                : entryPrice.subtract(exitPrice);
        return diff.multiply(positionSize);
    }
}
