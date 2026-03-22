package com.zFrameWork.zEngine.engine.backtest;

import com.zFrameWork.zEngine.core.risk.RiskManager;
import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradeDirection;
import com.zFrameWork.zEngine.core.strategy.TradingStrategy;
import com.zFrameWork.zEngine.model.entity.OptimizationResult;
import com.zFrameWork.zEngine.model.entity.OptimizationJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BacktestEngine {

    private final RiskManager riskManager;

    @Value("${zengine.backtest.initial-capital:1300}")
    private String initialCapitalConfig;

    public BacktestEngine(RiskManager riskManager) {
        this.riskManager = riskManager;
    }

    public OptimizationResult run(TradingStrategy strategy, List<MarketTick> historicalData, OptimizationJob job) {
        BigDecimal initialCapital = new BigDecimal(initialCapitalConfig);
        BigDecimal currentCapital = initialCapital;
        BigDecimal maxCapital = initialCapital;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        int totalTrades = 0;
        int winningTrades = 0;

        TradeDirection currentPosition = TradeDirection.NEUTRAL;
        BigDecimal entryPrice = BigDecimal.ZERO;
        BigDecimal positionSize = BigDecimal.ZERO;

        strategy.resetState();
        riskManager.reset(initialCapital);

        for (MarketTick tick : historicalData) {
            if (currentPosition == TradeDirection.NEUTRAL) {
                TradeDirection signal = strategy.evaluateEntry(tick);
                if (signal != TradeDirection.NEUTRAL) {
                    currentPosition = signal;
                    entryPrice = tick.getClose(); // Suponiendo ejecución a precio de cierre de la vela gatillo
                    positionSize = riskManager.calculatePositionSize(entryPrice, currentCapital);
                }
            } else {
                boolean hitSL = riskManager.hitStopLoss(tick, currentPosition, entryPrice);
                boolean strategyExit = strategy.evaluateExit(tick, currentPosition);

                if (strategyExit || hitSL) {
                    // Si toca SL priorizamos cerrar al precio del SL como penalización (simulación
                    // conservadora)
                    BigDecimal exitPrice = hitSL
                            ? (currentPosition == TradeDirection.LONG ? entryPrice.multiply(new BigDecimal("0.98")) : // 2%
                                                                                                                      // SL
                                                                                                                      // aprox
                                    entryPrice.multiply(new BigDecimal("1.02")))
                            : tick.getClose();

                    BigDecimal profitLoss = riskManager.calculateProfitLoss(currentPosition, entryPrice, exitPrice,
                            positionSize);
                    currentCapital = currentCapital.add(profitLoss);

                    totalTrades++;
                    if (profitLoss.compareTo(BigDecimal.ZERO) > 0) {
                        winningTrades++;
                    }

                    if (currentCapital.compareTo(maxCapital) > 0) {
                        maxCapital = currentCapital;
                    } else {
                        // Calcular Drawdown (MaxCap - CurrCap) / MaxCap
                        BigDecimal drawdown = maxCapital.subtract(currentCapital)
                                .divide(maxCapital, 8, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                        if (drawdown.compareTo(maxDrawdown) > 0) {
                            maxDrawdown = drawdown;
                        }
                    }

                    currentPosition = TradeDirection.NEUTRAL;
                }
            }
        }

        BigDecimal netProfit = currentCapital.subtract(initialCapital);
        BigDecimal winRate = totalTrades > 0
                ? new BigDecimal(winningTrades).divide(new BigDecimal(totalTrades), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        return buildResult(job, strategy, totalTrades, winRate, netProfit, maxDrawdown);
    }

    private OptimizationResult buildResult(OptimizationJob job, TradingStrategy strategy, int totalTrades, BigDecimal winRate,
            BigDecimal netProfit, BigDecimal maxDrawdown) {
        OptimizationResult result = new OptimizationResult();
        result.setOptimizationJob(job);
        result.setStrategyName(strategy.getStrategyName());
        result.setTotalTrades(totalTrades);
        result.setWinRatePct(winRate);
        result.setNetProfitUsd(netProfit);
        result.setMaxDrawdown(maxDrawdown);
        // --- SERIALIZANDO PARÁMETROS GENÉRICOS A JSON ---
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(strategy.getCurrentParameters());
            result.setParametersJson(json);
        } catch (Exception e) {
            System.err.println("Error serializando parámetros a JSON: " + e.getMessage());
        }

        return result;
    }
}