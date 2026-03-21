package com.zFrameWork.zEngine.core.risk;

import com.zFrameWork.zEngine.core.risk.impl.FixedFractionalRiskManager;
import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradeDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

public class FixedFractionalRiskManagerTest {

    private FixedFractionalRiskManager riskManager;

    @BeforeEach
    void setUp() {
        riskManager = new FixedFractionalRiskManager();
    }

    @Test
    void testCalculatePositionSize() {
        // En la implementación actual:
        // riesgo = 1.5% (0.015)
        // stop loss rel = 2% (0.02)
        // Capital = 1000
        // Capital en riesgo = 1000 * 0.015 = 15
        // Precio entrada = 100
        // Distancia a Stop Loss = 100 * 0.02 = 2
        // Position Size = 15 / 2 = 7.5 unidades
        
        BigDecimal entryPrice = new BigDecimal("100");
        BigDecimal currentCapital = new BigDecimal("1000");

        BigDecimal size = riskManager.calculatePositionSize(entryPrice, currentCapital);

        assertEquals(new BigDecimal("7.50000000"), size.setScale(8, RoundingMode.HALF_UP), "El tamaño de la posición debe ser exactamente 7.5 con 1000 usd de capital y riesgo del 1.5%");
    }

    @Test
    void testHitStopLoss_LongPosition() {
        BigDecimal entryPrice = new BigDecimal("100");
        // Stop loss para LONG al 2% significa stop price = 98.0
        
        // Vela que cae hasta 97 (Debe tocar Stop Loss)
        MarketTick tickHit = MarketTick.builder().low(new BigDecimal("97.00")).high(new BigDecimal("101.00")).build();
        assertTrue(riskManager.hitStopLoss(tickHit, TradeDirection.LONG, entryPrice), "La vela llegó a 97, debe tocar el stop loss de 98");

        // Vela que solo cae a 99 (A salvo)
        MarketTick tickSafe = MarketTick.builder().low(new BigDecimal("99.00")).high(new BigDecimal("101.00")).build();
        assertFalse(riskManager.hitStopLoss(tickSafe, TradeDirection.LONG, entryPrice), "La vela se mantuvo en 99, no debió tocar stop loss");
    }

    @Test
    void testHitStopLoss_ShortPosition() {
        BigDecimal entryPrice = new BigDecimal("100");
        // Stop loss para SHORT al 2% significa stop price = 102.0
        
        // Vela que sube hasta 103 (Debe tocar Stop Loss)
        MarketTick tickHit = MarketTick.builder().low(new BigDecimal("99.00")).high(new BigDecimal("103.00")).build();
        assertTrue(riskManager.hitStopLoss(tickHit, TradeDirection.SHORT, entryPrice), "La vela llegó a 103, debe tocar el stop loss de 102");

        // Vela que solo sube a 101 (A salvo)
        MarketTick tickSafe = MarketTick.builder().low(new BigDecimal("99.00")).high(new BigDecimal("101.00")).build();
        assertFalse(riskManager.hitStopLoss(tickSafe, TradeDirection.SHORT, entryPrice), "La vela se mantuvo en 101, no debió tocar stop loss");
    }
}
