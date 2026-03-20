package com.zFrameWork.zEngine.engine;

import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradingStrategy;
import com.zFrameWork.zEngine.engine.backtest.BacktestEngine;
import com.zFrameWork.zEngine.engine.data.MarketDataProvider;
import com.zFrameWork.zEngine.engine.optimizer.ParameterCombinator;
import com.zFrameWork.zEngine.engine.optimizer.ParameterRange;
import com.zFrameWork.zEngine.model.entity.OptimizationResult;
import com.zFrameWork.zEngine.repository.OptimizationResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OptimizationOrchestrator {

    private final MarketDataProvider dataProvider;
    private final ParameterCombinator combinator;
    private final BacktestEngine backtestEngine;
    private final OptimizationResultRepository repository;

    // Inyección de dependencias por constructor (Sin @Autowired, Spring lo hace automático)
    public OptimizationOrchestrator(MarketDataProvider dataProvider, 
                                    ParameterCombinator combinator, 
                                    BacktestEngine backtestEngine, 
                                    OptimizationResultRepository repository) {
        this.dataProvider = dataProvider;
        this.combinator = combinator;
        this.backtestEngine = backtestEngine;
        this.repository = repository;
    }

    /**
     * El punto de entrada principal para iniciar un trabajo de Fuerza Bruta.
     */
    public void runOptimization(String dataPath, TradingStrategy strategy, List<ParameterRange> ranges) {
        
        System.out.println("==================================================");
        System.out.println("🚀 INICIANDO LABORATORIO DE FUERZA BRUTA");
        System.out.println("Estrategia a evaluar: " + strategy.getStrategyName());
        System.out.println("Buscando la Meseta de Robustez...");
        System.out.println("==================================================");

        // 1. Cargar datos a la RAM (Solo ocurre una vez, el Provider maneja carpetas o archivos)
        System.out.println("Cargando histórico en memoria RAM...");
        List<MarketTick> historicalData = dataProvider.loadData(dataPath);

        // Generamos un ID único para agrupar todas las iteraciones de este experimento en la Base de Datos
        String jobExecutionId = UUID.randomUUID().toString();
        System.out.println("ID de Ejecución (Job ID): " + jobExecutionId);
        System.out.println("Iniciando permutaciones matemáticas...");

        // 2. Ejecutar la Fuerza Bruta
        long startTime = System.currentTimeMillis();
        
        // Usamos un array de 1 elemento para poder modificar el contador dentro de la función Lambda
        final int[] iterationCount = {0}; 

        combinator.generateAndExecute(ranges, currentParameters -> {
            // A. Inyectar la combinación exacta de parámetros a la estrategia
            strategy.setParameters(currentParameters);
            
            // B. Correr simulación (El motor ahora devuelve el resumen completo)
            OptimizationResult result = backtestEngine.run(strategy, historicalData, jobExecutionId);
            
            // C. Guardar la iteración en PostgreSQL para armar el mapa topográfico
            repository.save(result);
            
            iterationCount[0]++;
            // Imprimir progreso cada 100 iteraciones para no saturar la consola
            if (iterationCount[0] % 100 == 0) {
                System.out.println("Iteraciones completadas: " + iterationCount[0]);
            }
        });

        long endTime = System.currentTimeMillis();
        long durationSeconds = (endTime - startTime) / 1000;

        System.out.println("==================================================");
        System.out.println("✅ FUERZA BRUTA FINALIZADA");
        System.out.println("Total de combinaciones evaluadas: " + iterationCount[0]);
        System.out.println("Tiempo total de cálculo: " + durationSeconds + " segundos.");
        System.out.println("Resultados exportables guardados en PostgreSQL.");
        System.out.println("==================================================");
    }
}