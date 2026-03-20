package com.zFrameWork.zEngine.engine.backtest;

import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradeDirection;
import com.zFrameWork.zEngine.core.strategy.TradingStrategy;
import com.zFrameWork.zEngine.model.entity.OptimizationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class BacktestEngine {

    public OptimizationResult run(TradingStrategy strategy, List<MarketTick> historicalData, String jobExecutionId) {
        
        TradeDirection currentPosition = TradeDirection.NEUTRAL;
        BigDecimal entryPrice = BigDecimal.ZERO;
        
        BigDecimal initialCapital = new BigDecimal("1300.00");
        BigDecimal currentCapital = initialCapital;
        
        // Gestión de Riesgo: 1.5% del capital actual
        BigDecimal riskPct = new BigDecimal("1.5"); 
        BigDecimal stopLossPrice = BigDecimal.ZERO;
        BigDecimal currentPositionSize = BigDecimal.ZERO;

        // Contadores
        int totalTrades = 0;
        int winningTrades = 0;
        BigDecimal peakCapital = initialCapital; 
        BigDecimal maxDrawdownUsd = BigDecimal.ZERO;

        strategy.resetState();

        for (MarketTick tick : historicalData) {
            
            // 1. LÓGICA DE ENTRADA (Cruce de EMAs en 1h, 4h, 12h)
            if (currentPosition == TradeDirection.NEUTRAL) {
                TradeDirection signal = strategy.evaluateEntry(tick);
                if (signal != TradeDirection.NEUTRAL) {
                    currentPosition = signal;
                    entryPrice = tick.getClose(); 
                    
                    // Calcular Riesgo en USD (Ej: 1300 * 0.015 = $19.50)
                    BigDecimal riskUsdAmount = currentCapital.multiply(riskPct.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
                    
                    // Fijamos un Stop Loss técnico inicial (1.5% de distancia del precio)
                    BigDecimal slDistancePct = riskPct.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
                    
                    if (currentPosition == TradeDirection.LONG) {
                        stopLossPrice = entryPrice.subtract(entryPrice.multiply(slDistancePct));
                    } else {
                        stopLossPrice = entryPrice.add(entryPrice.multiply(slDistancePct));
                    }

                    // Calcular Tamaño de Posición Dinámico
                    BigDecimal priceDistance = entryPrice.subtract(stopLossPrice).abs();
                    currentPositionSize = riskUsdAmount.divide(priceDistance, 8, RoundingMode.HALF_UP);
                }
            } 
            // 2. LÓGICA DE SALIDA DINÁMICA (Trend Following Puro)
            else {
                boolean hitSl = false;
                
                // A. Verificar si el precio tocó el Stop Loss de protección (Catástrofe)
                if (currentPosition == TradeDirection.LONG && tick.getLow().compareTo(stopLossPrice) <= 0) hitSl = true;
                else if (currentPosition == TradeDirection.SHORT && tick.getHigh().compareTo(stopLossPrice) >= 0) hitSl = true;

                // B. Verificar si el indicador (EMA Gatillo) ordena salir (Cambio de tendencia)
                boolean signalExit = strategy.evaluateExit(tick, currentPosition);

                if (hitSl || signalExit) {
                    BigDecimal exitPrice = hitSl ? stopLossPrice : tick.getClose();

                    BigDecimal tradeProfitUsd = calculateRealUsdProfit(currentPosition, entryPrice, exitPrice, currentPositionSize);
                    
                    currentCapital = currentCapital.add(tradeProfitUsd);
                    totalTrades++;
                    
                    if (tradeProfitUsd.compareTo(BigDecimal.ZERO) > 0) winningTrades++;

                    // Tracking de Drawdown
                    if (currentCapital.compareTo(peakCapital) > 0) peakCapital = currentCapital;
                    BigDecimal currentDrawdown = peakCapital.subtract(currentCapital);
                    if (currentDrawdown.compareTo(maxDrawdownUsd) > 0) maxDrawdownUsd = currentDrawdown;

                    currentPosition = TradeDirection.NEUTRAL;

                    if (currentCapital.compareTo(BigDecimal.ZERO) <= 0) break;
                }
            }
        }

        BigDecimal netProfit = currentCapital.subtract(initialCapital);
        Map<String, BigDecimal> params = strategy.getCurrentParameters();

        return OptimizationResult.builder()
                .jobExecutionId(jobExecutionId)
                .emaRapida(params.getOrDefault("emaFast", BigDecimal.ZERO))
                .emaLenta(params.getOrDefault("emaSlow", BigDecimal.ZERO))
                .emaGatillo(params.getOrDefault("emaGatillo", BigDecimal.ZERO))
                .totalTrades(totalTrades)
                .winRatePct(calculateWinRate(winningTrades, totalTrades))
                .netProfitUsd(netProfit)
                .maxDrawdown(maxDrawdownUsd)
                .build();
    }

    private BigDecimal calculateRealUsdProfit(TradeDirection dir, BigDecimal entry, BigDecimal exit, BigDecimal size) {
        BigDecimal diff = (dir == TradeDirection.LONG) ? exit.subtract(entry) : entry.subtract(exit);
        return diff.multiply(size).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWinRate(int win, int total) {
        if (total == 0) return BigDecimal.ZERO;
        return new BigDecimal(win).divide(new BigDecimal(total), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }
}