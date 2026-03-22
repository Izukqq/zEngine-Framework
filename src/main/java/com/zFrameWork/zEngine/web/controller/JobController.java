package com.zFrameWork.zEngine.web.controller;

import com.zFrameWork.zEngine.model.entity.OptimizationJob;
import com.zFrameWork.zEngine.model.entity.OptimizationResult;
import com.zFrameWork.zEngine.repository.OptimizationJobRepository;
import com.zFrameWork.zEngine.repository.OptimizationResultRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final OptimizationJobRepository jobRepository;
    private final OptimizationResultRepository resultRepository;

    public JobController(OptimizationJobRepository jobRepository, OptimizationResultRepository resultRepository) {
        this.jobRepository = jobRepository;
        this.resultRepository = resultRepository;
    }

    /**
     * Recupera el historial maestro de todas las sesiones de Fuerza Bruta.
     */
    @GetMapping
    public ResponseEntity<List<OptimizationJob>> getAllJobs() {
        return ResponseEntity.ok(jobRepository.findAll());
    }

    /**
     * Recupera todas las iteraciones relacionales atadas a un Job en particular
     * para proyectar su Meseta de Robustez topográfica.
     */
    @GetMapping("/{id}/results")
    public ResponseEntity<List<OptimizationResult>> getJobResults(@PathVariable("id") String jobId) {
        return ResponseEntity.ok(resultRepository.findByOptimizationJobId(jobId));
    }
}
