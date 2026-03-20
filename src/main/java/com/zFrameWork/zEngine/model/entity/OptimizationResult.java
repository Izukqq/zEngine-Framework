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

    @Column(name = "job_execution_id")
    private String jobExecutionId;

    // --- PARÁMETROS DE LA ESTRATEGIA ---
    @Column(name = "ema_rapida", precision = 10, scale = 2)
    private BigDecimal emaRapida;

    @Column(name = "ema_lenta", precision = 10, scale = 2)
    private BigDecimal emaLenta;

    @Column(name = "ema_gatillo", precision = 10, scale = 2)
    private BigDecimal emaGatillo;

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