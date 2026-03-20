package com.zFrameWork.zEngine;

import com.zFrameWork.zEngine.engine.OptimizationOrchestrator;
import com.zFrameWork.zEngine.engine.optimizer.ParameterRange;
import com.zFrameWork.zEngine.examples.TripleEmaStrategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class ZEngineApplication implements CommandLineRunner {

    @Autowired
    private OptimizationOrchestrator orchestrator;

    public static void main(String[] args) {
        SpringApplication.run(ZEngineApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        
        // 1. Configuramos el rango masivo de 3 dimensiones
        List<ParameterRange> configuracionFuerzaBruta = Arrays.asList(
            // El Gatillo: Exploramos EMAs super rápidas (Del periodo 5 al 15, de 1 en 1) -> 11 opciones
            new ParameterRange("emaGatillo", new BigDecimal("5"), new BigDecimal("15"), new BigDecimal("1")),
            
            // La Rápida: Exploramos EMAs de medio plazo (Del periodo 20 al 50, de 5 en 5) -> 7 opciones
            new ParameterRange("emaFast", new BigDecimal("20"), new BigDecimal("50"), new BigDecimal("5")),
            
            // La Lenta (El Filtro): Exploramos EMAs de largo plazo (Del periodo 100 al 200, de 20 en 20) -> 6 opciones
            new ParameterRange("emaSlow", new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("20"))
        );
        // Total que generará: 11 * 7 * 6 = 462 backtests diferentes procesados de golpe.

        // 2. Ruta de tu carpeta con los 86 archivos CSV
        String rutaDatos = "TU RUTA AQUÍ/zEngine/src/main/resources/historical_data/";

        // 3. ¡Lanzamos el laboratorio con la NUEVA estrategia!
        orchestrator.runOptimization(rutaDatos, new TripleEmaStrategy(), configuracionFuerzaBruta);
	}
}