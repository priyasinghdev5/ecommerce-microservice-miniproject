package com.ecom.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Published by batch-service to topic: product.import
 * Consumed by: product-service — upserts the product into MongoDB and Elasticsearch
 *
 * batch-service reads CSV from SFTP, validates each row, then publishes
 * one ProductImportEvent per row (or per chunk of rows).
 */
public record ProductImportEvent(
        UUID eventId,
        UUID importJobId,        // links back to import_jobs table
        int rowNumber,
        String sku,              // business key for upsert
        String name,
        String description,
        String brand,
        List<String> categories,
        BigDecimal price,
        BigDecimal compareAtPrice,
        Map<String, Object> attributes,  // dynamic: color, size, material, etc.
        boolean active,
        Instant importedAt
) {}
