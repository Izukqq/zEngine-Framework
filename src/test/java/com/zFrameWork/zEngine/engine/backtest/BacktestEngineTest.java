package com.zFrameWork.zEngine.engine.backtest;

import com.zFrameWork.zEngine.core.risk.RiskManager;
import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradeDirection;
import com.zFrameWork.zEngine.core.strategy.TradingStrategy;
import com.zFrameWork.zEngine.model.entity.OptimizationResult;
import com.zFrameWork.zEngine.model.entity.OptimizationJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BacktestEngineTest {

        @Mock
        private RiskManager riskManagerMock;

        @Mock
        private TradingStrategy strategyMock;

        @InjectMocks
        private BacktestEngine backtestEngine;

        private List<MarketTick> simulatedMarket;

        @BeforeEach
        void setUp() {
                LocalDateTime now = LocalDateTime.now();
                // Vela 1: Señal de Compra (LONG) a precio 100
                MarketTick tick1 = MarketTick.builder().symbol("BTCUSDT").time(now).close(new BigDecimal("100"))
                                .build();

                // Vela 2: Salida por Ganancia a precio 120
                MarketTick tick2 = MarketTick.builder().symbol("BTCUSDT").time(now.plusMinutes(15))
                                .close(new BigDecimal("120")).build();

                // Vela 3: Señal de Compra (LONG) a precio 100
                MarketTick tick3 = MarketTick.builder().symbol("BTCUSDT").time(now.plusMinutes(30))
                                .close(new BigDecimal("100")).build();

                // Vela 4: Caída fuerte, Salida por StopLoss
                MarketTick tick4 = MarketTick.builder().symbol("BTCUSDT").time(now.plusMinutes(45))
                                .close(new BigDecimal("50")).build();

                simulatedMarket = Arrays.asList(tick1, tick2, tick3, tick4);
        }

    @Test
    void testDrawdownAndProfitCalculation_WithStopLossHit() {
        // --- 1. MOCK STRATEGY BEHAVIOR ---
        when(strategyMock.getStrategyName()).thenReturn("TestMockStrategy");
        when(strategyMock.getCurrentParameters()).thenReturn(new HashMap<>());

        // Para Vela 1: Emitir LONG
        when(strategyMock.evaluateEntry(simulatedMarket.get(0))).thenReturn(TradeDirection.LONG);
        when(strategyMock.evaluateExit(simulatedMarket.get(0), TradeDirection.LONG)).thenReturn(false);

        // Para Vela 2: Salir de la operación LONG
        when(strategyMock.evaluateExit(simulatedMarket.get(1), TradeDirection.LONG)).thenReturn(true);

        // Para Vela 3: Emitir LONG nuevamente
        when(strategyMock.evaluateEntry(simulatedMarket.get(2))).thenReturn(TradeDirection.LONG);
        when(strategyMock.evaluateExit(simulatedMarket.get(2), TradeDirection.LONG)).thenReturn(false);

        // Para Vela 4: No logramos salir por estrategia, salimos por el RiskManager (StopLoss)
        when(strategyMock.evaluateExit(simulatedMarket.get(3), TradeDirection.LONG)).thenReturn(false);

        // --- 2. MOCK RISK MANAGER BEHAVIOR ---
        // Simular que el tamaño de posición es de 2 monedas
        when(riskManagerMock.calculatePositionSize(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("2"));

        // En la vela 2 (Salida natural exitosa), reportamos la ganancia
        when(riskManagerMock.calculateProfitLoss(eq(TradeDirection.LONG), eq(new BigDecimal("100")), eq(new BigDecimal("120")), eq(new BigDecimal("2"))))
                .thenReturn(new BigDecimal("40")); // Profit = (120-100)*2 = 40 usd

        // En la vela 4 (Stop Loss), el motor asume precio de salida penalizado.
        // Si compramos en 100 y es LONG, el StopLoss forzado en BacktestEngine.java es 98.
        when(riskManagerMock.hitStopLoss(simulatedMarket.get(3), TradeDirection.LONG, new BigDecimal("100")))
                .thenReturn(true);

        when(riskManagerMock.calculateProfitLoss(eq(TradeDirection.LONG), eq(new BigDecimal("100")), eq(new BigDecimal("98.00")), eq(new BigDecimal("2"))))
                .thenReturn(new BigDecimal("-4.00")); // Loss = (98-100)*2 = -4 usd

        // --- 3. EXECUTE ENGINE ---
        OptimizationJob dummyJob = new OptimizationJob();
        dummyJob.setId("Test-Job-123");
        OptimizationResult result = backtestEngine.run(strategyMock, simulatedMarket, dummyJob);

        // --- 4. ASSERTIONS ---
        // Capital Inicial Hardcodeado en el Engine: 1300
        // Operación 1 (Ganadora): 1300 + 40 = 1340 (Max Capital)
        // Operación 2 (Perdedora por SL): 1340 - 4 = 1336
        // Total Profit = 1336 - 1300 = 36
        
        assertEquals(2, result.getTotalTrades(), "Deberían haberse ejecutado 2 trades");
        assertEquals(new BigDecimal("50.0000"), result.getWinRatePct().setScale(4, RoundingMode.HALF_UP), "WinRate de 50% (1 de 2)");
        assertEquals(new BigDecimal("36.00"), result.getNetProfitUsd().setScale(2, RoundingMode.HALF_UP), "El beneficio neto debe ser $36");

        // Drawdown = (Max Capital - Current Capital) / Max Capital * 100
        // (1340 - 1336) / 1340 * 100 = 4 / 1340 * 100 = 0.29850746%
        BigDecimal expectedDrawdown = new BigDecimal("4").divide(new BigDecimal("1340"), 8, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        assertEquals(expectedDrawdown.setScale(8, RoundingMode.HALF_UP), result.getMaxDrawdown().setScale(8, RoundingMode.HALF_UP), "El Drawdown matemático no coincide");

        verify(riskManagerMock, times(1)).reset(new BigDecimal("1300"));
        verify(strategyMock, times(1)).resetState();
    }
}
