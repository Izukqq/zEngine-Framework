package com.zFrameWork.zEngine;

import com.zFrameWork.zEngine.engine.OptimizationOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ZEngineApplication implements CommandLineRunner {

    @Autowired
    private OptimizationOrchestrator orchestrator;

    @Autowired
    private com.zFrameWork.zEngine.engine.manager.StrategyManager strategyManager;

    public static void main(String[] args) {
        SpringApplication.run(ZEngineApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        
        // La ruta de tu carpeta histórica
        String rutaDatos = "src/main/resources/historical_data/"; 

        // EL USUARIO ELIGE UNA SOLA ESTRATEGIA (Listaremos las que existen primero)
        System.out.println(">>> ESTRATEGIAS DETECTADAS EN EL FRAMEWORK <<<");
        strategyManager.getAvailableStrategyNames().forEach(name -> System.out.println("- " + name));

        // Por ahora, tomaremos la primera que se registró dinámicamente o puedes forzar un nombre:
        // "RSI_Strategy" o usar getAvailableStrategyNames().get(0)
        String strategyToRun = "RSI_Strategy"; 
        
        System.out.println("=================================================");
        System.out.println("Seleccionando Estrategia: " + strategyToRun);
        
        com.zFrameWork.zEngine.core.strategy.TradingStrategy chosenStrategy = strategyManager.getStrategyByName(strategyToRun);

        orchestrator.runOptimization(rutaDatos, chosenStrategy);
    }
}