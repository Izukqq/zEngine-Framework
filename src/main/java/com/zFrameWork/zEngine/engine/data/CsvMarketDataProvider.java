package com.zFrameWork.zEngine.engine.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.zFrameWork.zEngine.core.strategy.MarketTick;

/* Esta clase es responsable de cargar los datos históricos desde un archivo CSV.

   El formato esperado del CSV es: 
   IMPORTANTE EL FORMATO APLICABLE DE ESTA CLASE ESTA CONFIGURADO PARA BINANCE
   timestamp,open,high,low,close,volume
    Ejemplo de línea en el CSV:
    1769904000000,1300.00,1320.00,1280.00,1310.00,1000.00
        
*/
@Service
public class CsvMarketDataProvider implements MarketDataProvider {

    @Override
    public List<MarketTick> loadData(String filePath) {
        List<MarketTick> ticks = new ArrayList<>();

        String symbol = extractSymbolFromFileName(filePath);

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;

            // Eliminamos la lógica de saltar la primera línea, ya que tu CSV no tiene
            // cabeceras.
            while ((line = reader.readNext()) != null) {

                // Columna 0: Timestamp en milisegundos (ej. 1769904000000)
                long epochMillis = Long.parseLong(line[0].trim());
                // Convertimos el Timestamp a LocalDateTime usando la zona horaria UTC (Estándar
                // de Binance)
                LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.of("UTC"));

                MarketTick tick = MarketTick.builder()
                        .symbol(symbol)
                        .time(time)
                        .open(new BigDecimal(line[1].trim()))
                        .high(new BigDecimal(line[2].trim()))
                        .low(new BigDecimal(line[3].trim()))
                        .close(new BigDecimal(line[4].trim()))
                        .volume(new BigDecimal(line[5].trim()))
                        .build();

                ticks.add(tick);
            }

            System.out.println("✅ Datos históricos inyectados: " + ticks.size() + " velas de " + symbol + " listas.");

        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException("Error fatal al leer el archivo CSV en la ruta: " + filePath, e);
        }

        return ticks;
    }

    private String extractSymbolFromFileName(String filePath) {
        try {
            File file = new File(filePath);
            String name = file.getName();
            return name.split("-")[0];
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}