package com.zFrameWork.zEngine.engine;

import com.zFrameWork.zEngine.core.strategy.MarketTick;
import com.zFrameWork.zEngine.core.strategy.TradingStrategy;
import com.zFrameWork.zEngine.engine.backtest.BacktestEngine;
import com.zFrameWork.zEngine.engine.data.MarketDataProvider;
import com.zFrameWork.zEngine.engine.optimizer.ParameterCombinator;
import com.zFrameWork.zEngine.engine.optimizer.ParameterRange;
import com.zFrameWork.zEngine.engine.dto.ExecutionSummary;
import com.zFrameWork.zEngine.model.entity.OptimizationResult;
import com.zFrameWork.zEngine.model.entity.OptimizationJob;
import com.zFrameWork.zEngine.repository.OptimizationResultRepository;
import com.zFrameWork.zEngine.repository.OptimizationJobRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

import java.util.UUID;

@Service
public class OptimizationOrchestrator {

    private final MarketDataProvider dataProvider;
    private final ParameterCombinator combinator;
    private final BacktestEngine backtestEngine;
    private final OptimizationResultRepository repository;
    private final OptimizationJobRepository jobRepository;

    // Inyección de dependencias por constructor (Sin @Autowired, Spring lo hace
    // automático)
    public OptimizationOrchestrator(MarketDataProvider dataProvider,
            ParameterCombinator combinator,
            BacktestEngine backtestEngine,
            OptimizationResultRepository repository,
            OptimizationJobRepository jobRepository) {
        this.dataProvider = dataProvider;
        this.combinator = combinator;
        this.backtestEngine = backtestEngine;
        this.repository = repository;
        this.jobRepository = jobRepository;
    }

    /**
     * El punto de entrada principal para iniciar un trabajo de Fuerza Bruta
     * interactivo vía Web.
     * Retorna un Summary en lugar de void.
     */
    public ExecutionSummary runOptimization(String dataPath, TradingStrategy strategy,
            List<ParameterRange> userRanges) {

        System.out.println("==================================================");
        System.out.println("🚀 INICIANDO LABORATORIO DE FUERZA BRUTA");
        System.out.println("Estrategia a evaluar: " + strategy.getStrategyName());
        System.out.println("Buscando la Meseta de Robustez...");
        System.out.println("==================================================");

        // USAMOS los rangos dinámicos inyectados desde la API REST enviados por el
        // usuario
        List<ParameterRange> ranges = userRanges;

        // 1. Cargar datos a la RAM (Solo ocurre una vez, el Provider maneja carpetas o
        // archivos)
        System.out.println("Cargando histórico en memoria RAM...");
        List<MarketTick> historicalData = dataProvider.loadData(dataPath);

        // Generamos un ID único para agrupar todas las iteraciones de este experimento
        // en la Base de Datos
        String jobExecutionId = UUID.randomUUID().toString();

        // --- 2. Registrar el Trabajo en Base de Datos como RUNNING ---
        OptimizationJob job = OptimizationJob.builder()
                .id(jobExecutionId)
                .strategyName(strategy.getStrategyName())
                .startTime(LocalDateTime.now())
                .status(OptimizationJob.JobStatus.RUNNING)
                .build();
        jobRepository.save(job);

        System.out.println("ID de Ejecución (Job ID): " + jobExecutionId);
        System.out.println("Iniciando permutaciones matemáticas...");

        // Usamos un array de 1 elemento para poder modificar el contador dentro de la
        // función Lambda
        final int[] iterationCount = { 0 };
        long startTime = System.currentTimeMillis();

        try {
            combinator.generateAndExecute(ranges, currentParameters -> {
                // A. Inyectar la combinación exacta de parámetros a la estrategia
                strategy.setParameters(currentParameters);

                // B. Correr simulación
                OptimizationResult result = backtestEngine.run(strategy, historicalData, job);

                // C. Guardar la iteración en PostgreSQL para armar el mapa topográfico
                repository.save(result);

                iterationCount[0]++;
                if (iterationCount[0] % 100 == 0) {
                    System.out.println("Iteraciones completadas: " + iterationCount[0]);
                }
            });

            // --- 3. Marcar el Trabajo como COMPLETED ---
            job.setStatus(OptimizationJob.JobStatus.COMPLETED);
        } catch (Exception e) {
            System.err.println("Fuerza Bruta interrumpida abruptamente: " + e.getMessage());
            job.setStatus(OptimizationJob.JobStatus.FAILED);
        } finally {
            job.setEndTime(LocalDateTime.now());
            job.setCombinationsCount(iterationCount[0]);
            jobRepository.save(job);
        }

        long endTime = System.currentTimeMillis();
        long durationSeconds = (endTime - startTime) / 1000;

        System.out.println("==================================================");
        System.out.println("✅ FUERZA BRUTA FINALIZADA");
        System.out.println("Total de combinaciones evaluadas: " + iterationCount[0]);
        System.out.println("Tiempo total de cálculo: " + durationSeconds + " segundos.");
        System.out.println("Resultados exportables guardados en PostgreSQL.");
        System.out.println("==================================================");

        return new ExecutionSummary(jobExecutionId, iterationCount[0], durationSeconds, strategy.getStrategyName());
    }
}