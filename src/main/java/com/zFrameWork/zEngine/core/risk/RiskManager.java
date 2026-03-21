package com.zFrameWork.zEngine.core.risk;

import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradeDirection;
import java.math.BigDecimal;

public interface RiskManager {
    void reset(BigDecimal initialCapital);
    BigDecimal calculatePositionSize(BigDecimal entryPrice, BigDecimal currentCapital);
    boolean hitStopLoss(MarketTick tick, TradeDirection currentDirection, BigDecimal entryPrice);
    BigDecimal calculateProfitLoss(TradeDirection currentDirection, BigDecimal entryPrice, BigDecimal exitPrice, BigDecimal positionSize);
}
