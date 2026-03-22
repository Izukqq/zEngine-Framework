package com.zFrameWork.zEngine.repository;
import com.zFrameWork.zEngine.model.entity.OptimizationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptimizationResultRepository extends JpaRepository<OptimizationResult, Long> {
    
    // Spring Data JPA ya nos regala el método .save() por defecto.
    List<OptimizationResult> findByOptimizationJobId(String jobId);
}