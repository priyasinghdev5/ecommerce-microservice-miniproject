package com.ecom.batch.service;

import com.ecom.batch.entity.ImportJob;
import com.ecom.batch.entity.ImportStatus;
import com.ecom.batch.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportJobService {

    private final JobLauncher jobLauncher;
    private final Job productImportJob;
    private final ImportJobRepository jobRepository;

    private static final String UPLOAD_DIR = "uploads/";

    /**
     * Accepts a CSV file upload, saves it, creates an ImportJob record,
     * and launches the Spring Batch job asynchronously.
     */
    public ImportJob startImport(MultipartFile file) throws Exception {
        // Save file to local disk
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        String filename = file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR + UUID.randomUUID() + "_" + filename);
        file.transferTo(filePath.toFile());

        // Create job record
        ImportJob job = ImportJob.builder()
                .filename(filename)
                .sourcePath(filePath.toString())
                .status(ImportStatus.PENDING)
                .build();
        job = jobRepository.save(job);

        // Launch batch job
        JobParameters params = new JobParametersBuilder()
                .addString("jobId", job.getId().toString())
                .addString("filePath", filePath.toAbsolutePath().toString())
                .addLong("timestamp", Instant.now().toEpochMilli())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(productImportJob, params);
        log.info("Batch job launched: jobId={} executionId={}", job.getId(), execution.getId());
        return job;
    }

    public List<ImportJob> getAllJobs() {
        return jobRepository.findAll();
    }

    public ImportJob getJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Import job not found: " + jobId));
    }
}
