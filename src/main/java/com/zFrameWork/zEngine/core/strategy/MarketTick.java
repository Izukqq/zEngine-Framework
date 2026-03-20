package com.zFrameWork.zEngine.core.strategy;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una unidad de datos del mercado (una vela/tick) en un momento específico.
 * Se utiliza @Getter y no @Setter para garantizar la inmutabilidad durante el backtest.
 */

@Builder
@Getter
@ToString

public class MarketTick { 
    private final String symbol;
    private final LocalDateTime time;

    private final BigDecimal open;
    private final BigDecimal high;
    private final BigDecimal low;
    private final BigDecimal close;
    private final BigDecimal volume;
}
