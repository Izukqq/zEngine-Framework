package com.zFrameWork.zEngine.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "optimization_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private OptimizationJob optimizationJob;

    @Column(name = "strategy_name")
    private String strategyName;

    // --- PARÁMETROS GENÉRICOS ---
    // Persistimos el mapa completo como un String JSON (Soporta estrategias con 3, 5, 20 parámetros)
    @Column(name = "parameters_json", columnDefinition = "TEXT")
    private String parametersJson;

    // --- RESULTADOS AGREGADOS (Para la Meseta de Robustez) ---
    @Column(name = "total_trades")
    private int totalTrades;

    @Column(name = "win_rate_pct", precision = 5, scale = 2)
    private BigDecimal winRatePct;

    @Column(name = "net_profit_usd", precision = 19, scale = 8)
    private BigDecimal netProfitUsd;

    @Column(name = "max_drawdown", precision = 19, scale = 8)
    private BigDecimal maxDrawdown;
}