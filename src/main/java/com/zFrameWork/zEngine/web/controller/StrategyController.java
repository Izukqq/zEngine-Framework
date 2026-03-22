package com.zFrameWork.zEngine.web.controller;

import com.zFrameWork.zEngine.engine.OptimizationOrchestrator;
import com.zFrameWork.zEngine.engine.dto.ExecutionSummary;
import com.zFrameWork.zEngine.engine.manager.StrategyManager;
import com.zFrameWork.zEngine.engine.optimizer.ParameterRange;
import com.zFrameWork.zEngine.web.dto.StrategyDto;
import com.zFrameWork.zEngine.web.dto.ParameterRangeDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/strategies")
public class StrategyController {

    private final StrategyManager strategyManager;
    private final OptimizationOrchestrator orchestrator;

    // Dependencia inyectada directamente por Spring Boot
    public StrategyController(StrategyManager strategyManager, OptimizationOrchestrator orchestrator) {
        this.strategyManager = strategyManager;
        this.orchestrator = orchestrator;
    }

    /**
     * Expone de manera transparente la lista actual de estrategias
     * auto-descubiertas por el StrategyManager, mapeadas nativamente a DTOs JSON.
     */
    @GetMapping
    public ResponseEntity<List<StrategyDto>> getAvailableStrategies() {
        List<String> strategyNames = strategyManager.getAvailableStrategyNames();
        
        List<StrategyDto> dtoList = strategyNames.stream()
                .map(name -> new StrategyDto(name, name.replace("_", " ")))
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(dtoList);
    }

    /**
     * Endpoint para consultar qué parámetros requiere una estrategia.
     * Retorna una lista de ParameterRangeDto para dibujar formularios dinámicos en React.
     */
    @GetMapping("/{id}/parameters")
    public ResponseEntity<List<ParameterRangeDto>> getStrategyParameters(@PathVariable("id") String strategyId) {
        var strategy = strategyManager.getStrategyByName(strategyId);
        
        List<ParameterRangeDto> parameterDtos = strategy.getParameterDefinitions().stream()
            .map(pr -> new ParameterRangeDto(pr.getName(), pr.getStart(), pr.getEnd(), pr.getStep()))
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(parameterDtos);
    }

    /**
     * Endpoint asíncrono-síncrono para ejecutar el Backtest en Java 
     * basado en los rangos enviados desde la UI de React de forma dinámica.
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<ExecutionSummary> executeStrategy(
            @PathVariable("id") String strategyId,
            @RequestBody Map<String, ParameterRangeDto> userRangesMap) {
        
        // 1. Obtenemos la estrategia del registro Spring
        var strategy = strategyManager.getStrategyByName(strategyId);
        
        // Convertimos el Map<String, ParameterRangeDto> del frontend en List<ParameterRange> para el Motor
        List<ParameterRange> executionRanges = new ArrayList<>();
        for (Map.Entry<String, ParameterRangeDto> entry : userRangesMap.entrySet()) {
            ParameterRangeDto dto = entry.getValue();
            // Asegurar que el nombre coincida (Data Consistency)
            executionRanges.add(new ParameterRange(entry.getKey(), dto.getStart(), dto.getEnd(), dto.getStep()));
        }
        
        // 2. Ejecutar la iteración en el motor (con producto cartesiano dinámico)
        String rutaDatos = "src/main/resources/historical_data/"; 
        ExecutionSummary summary = orchestrator.runOptimization(rutaDatos, strategy, executionRanges);
        
        // 3. Devolvemos el reporte al Frontend
        return ResponseEntity.ok(summary);
    }
}
