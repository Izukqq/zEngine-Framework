package com.zFrameWork.zEngine.engine.optimizer;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class ParameterCombinator {

    /**
     * Inicia la generación de combinaciones por fuerza bruta.
     * * @param ranges Lista de parámetros con sus rangos y saltos (steps).
     * @param backtestExecutor La función (callback) que ejecutará la prueba con la combinación actual.
     */
    public void generateAndExecute(List<ParameterRange> ranges, Consumer<Map<String, BigDecimal>> backtestExecutor) {
        if (ranges == null || ranges.isEmpty()) {
            throw new IllegalArgumentException("La lista de rangos no puede estar vacía.");
        }
        
        // Mapa temporal para ir construyendo la combinación actual
        Map<String, BigDecimal> currentCombination = new HashMap<>();
        
        // Iniciamos la recursión desde el nivel 0
        combineRecursive(ranges, 0, currentCombination, backtestExecutor);
    }

    /**
     * Algoritmo recursivo de Producto Cartesiano para explorar el árbol de posibilidades.
     * Mantiene una complejidad espacial mínima (O(N) donde N es la cantidad de parámetros).
     */
    private void combineRecursive(List<ParameterRange> ranges, int depth, Map<String, BigDecimal> currentCombination, Consumer<Map<String, BigDecimal>> backtestExecutor) {
        // Caso Base: Si la profundidad es igual al número de rangos, hemos completado 1 combinación.
        if (depth == ranges.size()) {
            // Pasamos una COPIA del mapa al ejecutor para evitar mutaciones indeseadas por referencia
            backtestExecutor.accept(new HashMap<>(currentCombination));
            return;
        }

        // Obtener el rango del nivel actual
        ParameterRange currentRange = ranges.get(depth);
        BigDecimal currentValue = currentRange.getStart();

        // Bucle iterativo sobre el parámetro actual
        // currentValue.compareTo(end) <= 0  significa "mientras currentValue sea menor o igual a end"
        while (currentValue.compareTo(currentRange.getEnd()) <= 0) {
            
            // Asignamos el valor actual al parámetro
            currentCombination.put(currentRange.getName(), currentValue);
            
            // Llamada recursiva: avanzamos al siguiente parámetro (profundidad + 1)
            combineRecursive(ranges, depth + 1, currentCombination, backtestExecutor);
            
            // Incrementamos el valor según el 'step' usando precisión absoluta
            currentValue = currentValue.add(currentRange.getStep());
        }
    }
}