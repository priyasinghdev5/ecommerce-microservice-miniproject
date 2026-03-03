package com.ecom.product.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ecom.product.document.ProductDocument;
import com.ecom.product.dto.ProductResponse;
import com.ecom.product.mapper.ProductMapper;
import com.ecom.product.repository.ProductRepository;
import com.ecom.product.search.ProductSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ElasticsearchClient elasticsearchClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getById(String id) {
        return productRepository.findById(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    @Cacheable(value = "products", key = "'sku:' + #sku")
    public ProductResponse getBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Product not found: " + sku));
    }

    public List<ProductResponse> getFeatured() {
        return productRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream().limit(10)
                .map(productMapper::toResponse)
                .toList();
    }

    public List<ProductResponse> getByCategory(String category) {
        return productRepository.findByActiveTrueAndCategoriesContaining(category)
                .stream().map(productMapper::toResponse).toList();
    }

    /**
     * Full-text search via Elasticsearch.
     * Cache miss: query ES → fetch full docs from MongoDB → cache result.
     */
    public List<ProductResponse> search(String query, int page, int size) {
        String cacheKey = "search:" + query.toLowerCase() + ":" + page;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Search cache hit: {}", cacheKey);
            return (List<ProductResponse>) cached;
        }

        try {
            SearchResponse<ProductSearchDocument> response = elasticsearchClient.search(s -> s
                .index("products")
                .from(page * size)
                .size(size)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m
                            .multiMatch(mm -> mm
                                .query(query)
                                .fields("name^3", "description", "brand^2")
                                .fuzziness("AUTO")
                            )
                        )
                        .filter(f -> f.term(t -> t.field("active").value(true)))
                    )
                ),
                ProductSearchDocument.class
            );

            List<String> ids = response.hits().hits().stream()
                    .map(Hit::id).toList();

            List<ProductResponse> results = productRepository.findAllById(ids)
                    .stream().map(productMapper::toResponse).toList();

            // Cache for 2 minutes
            redisTemplate.opsForValue().set(cacheKey, results, 120, TimeUnit.SECONDS);
            return results;

        } catch (IOException e) {
            log.error("Elasticsearch search failed: {}", e.getMessage());
            // Fallback to MongoDB text search
            return productRepository.findByActiveTrueOrderByCreatedAtDesc()
                    .stream().map(productMapper::toResponse).toList();
        }
    }
}
