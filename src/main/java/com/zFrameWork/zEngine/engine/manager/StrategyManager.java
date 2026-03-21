package com.zFrameWork.zEngine.engine.manager;

import com.zFrameWork.zEngine.core.strategy.TradingStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StrategyManager {

    private final Map<String, TradingStrategy> strategyRegistry;

    // Spring inyecta automáticamente todas las clases que implementen TradingStrategy
    public StrategyManager(List<TradingStrategy> availableStrategies) {
        this.strategyRegistry = availableStrategies.stream()
                .collect(Collectors.toMap(TradingStrategy::getStrategyName, strategy -> strategy));
    }

    public List<String> getAvailableStrategyNames() {
        return strategyRegistry.keySet().stream().collect(Collectors.toList());
    }

    public TradingStrategy getStrategyByName(String name) {
        TradingStrategy strategy = strategyRegistry.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy no encontrada: " + name);
        }
        return strategy;
    }
}
