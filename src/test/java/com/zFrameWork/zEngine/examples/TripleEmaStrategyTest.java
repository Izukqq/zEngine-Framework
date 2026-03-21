package com.zFrameWork.zEngine.examples;

import com.zFrameWork.zEngine.core.indicator.Indicator;
import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradeDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TripleEmaStrategyTest {

    @InjectMocks
    private TripleEmaStrategy strategy;

    @Mock
    private Indicator emaGatilloMock;

    @Mock
    private Indicator emaFastMock;

    @Mock
    private Indicator emaSlowMock;

    @BeforeEach
    void setUp() throws Exception {
        // Usamos Reflection nativa de Java para sobreescribir los "Legos" internos 
        // y reemplazarlos por Mocks controlados, testeando PURAMENTE la lógica de la estrategia.
        Field gatilloField = TripleEmaStrategy.class.getDeclaredField("emaGatillo");
        gatilloField.setAccessible(true);
        gatilloField.set(strategy, emaGatilloMock);

        Field fastField = TripleEmaStrategy.class.getDeclaredField("emaFast");
        fastField.setAccessible(true);
        fastField.set(strategy, emaFastMock);

        Field slowField = TripleEmaStrategy.class.getDeclaredField("emaSlow");
        slowField.setAccessible(true);
        slowField.set(strategy, emaSlowMock);
        
        // Inicializar el estado de la estrategia para asegurar que previousEmaGatillo sea null al inicio
        strategy.resetState();
    }

    @Test
    void testEvaluateEntry_SignalsLongOnCrossover() {
        MarketTick tick1 = MarketTick.builder().symbol("TEST").time(LocalDateTime.now()).close(new BigDecimal("100")).build();
        MarketTick tick2 = MarketTick.builder().symbol("TEST").time(LocalDateTime.now().plusMinutes(1)).close(new BigDecimal("105")).build();

        // -------------------------------------------------------------
        // VELA 1: Preparación ANTES del cruce
        // Tendencia alcista (Fast > Slow), pero el Gatillo aún está por DEBAJO de la Rápida.
        // Se llama getValue() 2 veces por ejecución de vela (antes de actualizar y después).
        // -------------------------------------------------------------
        when(emaGatilloMock.getValue()).thenReturn(
            new BigDecimal("48.0"), // Para previousEmaGatillo en tick1
            new BigDecimal("48.0"), // Para currentEmaGatillo en tick1
            new BigDecimal("48.0"), // Para previousEmaGatillo en tick2 (recuerda lo que era antes)
            new BigDecimal("52.0")  // Para currentEmaGatillo en tick2 (¡cruzó hacia arriba!)
        );
        when(emaFastMock.getValue()).thenReturn(new BigDecimal("50.0"));    // Rápida se queda estable
        when(emaSlowMock.getValue()).thenReturn(new BigDecimal("40.0"));    // Lenta se queda estable

        TradeDirection signal1 = strategy.evaluateEntry(tick1);
        
        // La estrategia debe mantenerse Neutral porque no ha habido cruce hacia arriba
        assertEquals(TradeDirection.NEUTRAL, signal1, "La estrategia debe mantenerse NEUTRAL antes del cruce.");

        // -------------------------------------------------------------
        // VELA 2: EL CRUCE (Golden Cross del Gatillo)
        // El Gatillo "rompe" hacia arriba cruzando la Rápida. La tendencia alcista se mantiene (Fast > Slow).
        // -------------------------------------------------------------
        TradeDirection signal2 = strategy.evaluateEntry(tick2);

        // La estrategia debe detectar el cruce alcista y emitir LONG
        assertEquals(TradeDirection.LONG, signal2, "La estrategia debe disparar LONG exactamente en la vela del cruce alcista.");

        verify(emaGatilloMock, times(2)).update(any(BigDecimal.class));
    }
}
