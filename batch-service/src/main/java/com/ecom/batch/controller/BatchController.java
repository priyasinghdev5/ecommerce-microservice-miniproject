package com.ecom.batch.controller;

import com.ecom.batch.entity.ImportJob;
import com.ecom.batch.service.ImportJobService;
import com.ecom.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final ImportJobService importJobService;

    /**
     * Upload a CSV file to trigger a batch import job.
     * Returns immediately with job ID — processing is async.
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<ImportJob>> startImport(
            @RequestParam("file") MultipartFile file) throws Exception {
        ImportJob job = importJobService.startImport(file);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Import job started", job));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<ImportJob>>> getAllJobs() {
        return ResponseEntity.ok(ApiResponse.ok(importJobService.getAllJobs()));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<ImportJob>> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(importJobService.getJob(jobId)));
    }
}
