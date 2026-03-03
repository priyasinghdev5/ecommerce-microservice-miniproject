package com.ecom.batch.config;

import com.ecom.batch.dto.ProductCsvRow;
import com.ecom.batch.listener.JobCompletionListener;
import com.ecom.batch.processor.ProductItemProcessor;
import com.ecom.batch.writer.KafkaProductItemWriter;
import com.ecom.common.events.ProductImportEvent;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.FileReader;
import java.io.Reader;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchJobConfig {

    private final JobCompletionListener jobCompletionListener;
    private final ProductItemProcessor processor;
    private final KafkaProductItemWriter writer;

    @Bean
    public Job productImportJob(JobRepository jobRepository, Step productImportStep) {
        return new JobBuilder("productImportJob", jobRepository)
                .listener(jobCompletionListener)
                .start(productImportStep)
                .build();
    }

    @Bean
    public Step productImportStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   ItemReader<ProductCsvRow> csvReader) {
        return new StepBuilder("productImportStep", jobRepository)
                .<ProductCsvRow, ProductImportEvent>chunk(100, transactionManager)
                .reader(csvReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)   // skip all bad rows, log errors
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<ProductCsvRow> csvReader(
            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{jobParameters['jobId']}") String jobId) throws Exception {

        processor.setImportJobId(UUID.fromString(jobId));
        Reader reader = new FileReader(filePath);
        CsvToBean<ProductCsvRow> csvToBean = new CsvToBeanBuilder<ProductCsvRow>(reader)
                .withType(ProductCsvRow.class)
                .withIgnoreLeadingWhiteSpace(true)
                .withIgnoreEmptyLine(true)
                .build();
        return new IteratorItemReader<>(csvToBean.iterator());
    }
}
