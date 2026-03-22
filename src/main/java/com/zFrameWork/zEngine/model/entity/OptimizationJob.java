package com.zFrameWork.zEngine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "optimization_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationJob {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "strategy_name", nullable = false)
    private String strategyName;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    @Column(name = "combinations_count")
    private int combinationsCount;
    
    public enum JobStatus {
        RUNNING,
        COMPLETED,
        FAILED
    }
}
