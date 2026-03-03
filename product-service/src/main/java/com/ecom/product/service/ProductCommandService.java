package com.ecom.product.service;

import com.ecom.product.document.ProductDocument;
import com.ecom.product.dto.*;
import com.ecom.product.mapper.ProductMapper;
import com.ecom.product.repository.ProductRepository;
import com.ecom.product.search.ProductSearchDocument;
import com.ecom.product.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.ecom.common.kafka.KafkaTopics;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;
    private final ProductMapper productMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new RuntimeException("Product with SKU already exists: " + request.sku());
        }
        ProductDocument doc = productMapper.toDocument(request);
        ProductDocument saved = productRepository.save(doc);

        // Sync to Elasticsearch
        searchRepository.save(productMapper.toSearchDocument(saved));
        log.info("Product created: sku={}", saved.getSku());
        return productMapper.toResponse(saved);
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        ProductDocument existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        existing.setSku(request.sku());
        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setBrand(request.brand());
        existing.setCategories(request.categories());
        existing.setPrice(request.price());
        existing.setCompareAtPrice(request.compareAtPrice());
        existing.setImageUrls(request.imageUrls());
        existing.setActive(request.active());
        existing.setAttributes(request.attributes());

        ProductDocument saved = productRepository.save(existing);

        // Evict Redis cache
        redisTemplate.delete("product:" + id);
        redisTemplate.delete("product:sku:" + request.sku());

        // Sync to Elasticsearch
        searchRepository.save(productMapper.toSearchDocument(saved));

        // Notify other services via Kafka
        kafkaTemplate.send(KafkaTopics.PRODUCT_UPDATED, id, saved);
        log.info("Product updated: id={}", id);
        return productMapper.toResponse(saved);
    }

    public void deleteProduct(String id) {
        ProductDocument doc = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        doc.setActive(false);
        productRepository.save(doc);
        searchRepository.deleteById(id);
        redisTemplate.delete("product:" + id);
        log.info("Product deactivated: id={}", id);
    }
}
