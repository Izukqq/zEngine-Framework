package com.zFrameWork.zEngine.examples;

import com.zFrameWork.zEngine.core.indicator.Indicator;
import com.zFrameWork.zEngine.core.indicator.impl.RsiIndicator;
import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradeDirection;
import com.zFrameWork.zEngine.core.strategy.TradingStrategy;
import com.zFrameWork.zEngine.engine.optimizer.ParameterRange;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RsiStrategy implements TradingStrategy {

    private Map<String, BigDecimal> currentParameters = new HashMap<>();

    private final Indicator rsi = new RsiIndicator("periodRsi");
    private BigDecimal previousRsi = null;

    @Override
    public String getStrategyName() {
        return "RSI_Strategy";
    }

    @Override
    public List<ParameterRange> getParameterDefinitions() {
        // Rangos para probar:
        // Periodo RSI: de 10 a 20 de 2 en 2 (6 variaciones)
        // Banda de Sobreventa (Oversold): de 25 a 40 de 5 en 5 (4 variaciones)
        // Banda de Sobrecompra (Overbought): de 60 a 75 de 5 en 5 (4 variaciones)
        // Total = 96 simulaciones únicas
        return Arrays.asList(
            new ParameterRange("periodRsi", new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("2")),
            new ParameterRange("oversold", new BigDecimal("25"), new BigDecimal("40"), new BigDecimal("5")),
            new ParameterRange("overbought", new BigDecimal("60"), new BigDecimal("75"), new BigDecimal("5"))
        );
    }

    @Override
    public void setParameters(Map<String, BigDecimal> parameters) {
        this.currentParameters = new HashMap<>(parameters);
        rsi.setParameters(parameters);
    }

    @Override
    public Map<String, BigDecimal> getCurrentParameters() {
        return currentParameters;
    }

    @Override
    public void resetState() {
        rsi.reset();
        previousRsi = null;
    }

    @Override
    public TradeDirection evaluateEntry(MarketTick tick) {
        previousRsi = rsi.getValue();
        rsi.update(tick.getClose());
        
        BigDecimal currentRsi = rsi.getValue();
        if (previousRsi == null || currentRsi == null) return TradeDirection.NEUTRAL;

        BigDecimal oversold = currentParameters.getOrDefault("oversold", new BigDecimal("30"));
        BigDecimal overbought = currentParameters.getOrDefault("overbought", new BigDecimal("70"));

        // LÓGICA DE ENTRADA: 
        // LONG: Cuando el RSI cruza hacia ARRIBA de la línea de Sobreventa (Rebote alcista)
        if (previousRsi.compareTo(oversold) <= 0 && currentRsi.compareTo(oversold) > 0) {
            return TradeDirection.LONG;
        }

        // SHORT: Cuando el RSI cruza hacia ABAJO de la línea de Sobrecompra (Rechazo bajista)
        if (previousRsi.compareTo(overbought) >= 0 && currentRsi.compareTo(overbought) < 0) {
            return TradeDirection.SHORT;
        }

        return TradeDirection.NEUTRAL;
    }

    @Override
    public boolean evaluateExit(MarketTick tick, TradeDirection currentDirection) {
        rsi.update(tick.getClose());
        BigDecimal currentRsi = rsi.getValue();
        
        if (currentRsi == null) return false;

        BigDecimal oversold = currentParameters.getOrDefault("oversold", new BigDecimal("30"));
        BigDecimal overbought = currentParameters.getOrDefault("overbought", new BigDecimal("70"));

        // LÓGICA DE SALIDA:
        // Si estamos Long, cerramos si llega a la zona de Sobrecompra y se frena, o si colapsa bajo 50
        if (currentDirection == TradeDirection.LONG) {
            // Take Profit si entra a sobrecompra
            if (currentRsi.compareTo(overbought) >= 0) return true;
        }

        // Si estamos Short, cerramos si llega a zona de Sobreventa, o si cruza arriba de 50
        if (currentDirection == TradeDirection.SHORT) {
            // Take Profit si entra a sobreventa
            if (currentRsi.compareTo(oversold) <= 0) return true;
        }

        return false;
    }
}
