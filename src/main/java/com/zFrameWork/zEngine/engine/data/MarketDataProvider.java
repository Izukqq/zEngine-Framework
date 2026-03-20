package com.zFrameWork.zEngine.engine.data;

import java.util.List;

import com.zFrameWork.zEngine.core.strategy.MarketTick;

public interface MarketDataProvider {

    List<MarketTick> loadData(String source);
    
}
