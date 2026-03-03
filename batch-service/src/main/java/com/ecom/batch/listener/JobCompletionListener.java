package com.ecom.batch.listener;

import com.ecom.batch.entity.ImportJob;
import com.ecom.batch.entity.ImportStatus;
import com.ecom.batch.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobCompletionListener implements JobExecutionListener {

    private final ImportJobRepository jobRepository;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobId = jobExecution.getJobParameters().getString("jobId");
        if (jobId != null) {
            jobRepository.findById(UUID.fromString(jobId)).ifPresent(job -> {
                job.setStatus(ImportStatus.RUNNING);
                job.setStartedAt(Instant.now());
                jobRepository.save(job);
            });
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobId = jobExecution.getJobParameters().getString("jobId");
        if (jobId == null) return;

        jobRepository.findById(UUID.fromString(jobId)).ifPresent(job -> {
            long readCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(s -> s.getReadCount()).sum();
            long writeCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(s -> s.getWriteCount()).sum();
            long skipCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(s -> s.getSkipCount()).sum();

            job.setTotalRecords((int) readCount);
            job.setProcessedCount((int) writeCount);
            job.setErrorCount((int) skipCount);
            job.setCompletedAt(Instant.now());
            job.setStatus(jobExecution.getStatus() == BatchStatus.COMPLETED
                    ? (skipCount > 0 ? ImportStatus.PARTIAL : ImportStatus.COMPLETED)
                    : ImportStatus.FAILED);
            jobRepository.save(job);

            log.info("Batch job complete: id={} status={} read={} written={} skipped={}",
                    jobId, job.getStatus(), readCount, writeCount, skipCount);
        });
    }
}
