package com.zFrameWork.zEngine.repository;

import com.zFrameWork.zEngine.model.entity.OptimizationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OptimizationJobRepository extends JpaRepository<OptimizationJob, String> {
}
