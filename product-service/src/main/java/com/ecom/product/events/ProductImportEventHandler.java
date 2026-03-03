package com.ecom.product.events;

import com.ecom.common.events.ProductImportEvent;
import com.ecom.common.kafka.KafkaTopics;
import com.ecom.product.document.ProductDocument;
import com.ecom.product.repository.ProductRepository;
import com.ecom.product.repository.ProductSearchRepository;
import com.ecom.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Consumes ProductImportEvent from batch-service.
 * Upserts product into MongoDB and Elasticsearch.
 * Evicts Redis cache for the affected product.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductImportEventHandler {

    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @KafkaListener(topics = KafkaTopics.PRODUCT_IMPORT, groupId = "product-service")
    public void handleProductImport(ProductImportEvent event) {
        log.info("Received ProductImportEvent: sku={} job={}", event.sku(), event.importJobId());

        Optional<ProductDocument> existing = productRepository.findBySku(event.sku());

        ProductDocument doc;
        if (existing.isPresent()) {
            // Update existing
            doc = existing.get();
            doc.setName(event.name());
            doc.setDescription(event.description());
            doc.setBrand(event.brand());
            doc.setCategories(event.categories());
            doc.setPrice(event.price());
            doc.setCompareAtPrice(event.compareAtPrice());
            doc.setAttributes(event.attributes());
            doc.setActive(event.active());
        } else {
            // Create new
            doc = ProductDocument.builder()
                    .sku(event.sku())
                    .name(event.name())
                    .description(event.description())
                    .brand(event.brand())
                    .categories(event.categories())
                    .price(event.price())
                    .compareAtPrice(event.compareAtPrice())
                    .attributes(event.attributes())
                    .active(event.active())
                    .build();
        }

        ProductDocument saved = productRepository.save(doc);

        // Sync to Elasticsearch
        searchRepository.save(productMapper.toSearchDocument(saved));

        // Evict Redis cache
        redisTemplate.delete("product:" + saved.getId());
        redisTemplate.delete("product:sku:" + saved.getSku());

        log.info("Product upserted: sku={} id={}", saved.getSku(), saved.getId());
    }
}
