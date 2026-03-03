package com.ecom.batch.writer;

import com.ecom.common.events.ProductImportEvent;
import com.ecom.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Writes validated ProductImportEvents to Kafka.
 * product-service consumes these and upserts into MongoDB + Elasticsearch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProductItemWriter implements ItemWriter<ProductImportEvent> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void write(Chunk<? extends ProductImportEvent> chunk) {
        for (ProductImportEvent event : chunk.getItems()) {
            kafkaTemplate.send(KafkaTopics.PRODUCT_IMPORT, event.sku(), event);
            log.debug("Published ProductImportEvent: sku={}", event.sku());
        }
        log.info("Batch chunk written: {} products published to Kafka", chunk.size());
    }
}
