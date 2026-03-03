package com.ecom.batch.processor;

import com.ecom.batch.dto.ProductCsvRow;
import com.ecom.common.events.ProductImportEvent;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Validates and transforms a CSV row into a ProductImportEvent.
 * Returns null to SKIP invalid rows (Spring Batch skips null items).
 * Invalid rows are counted and logged as ImportErrors.
 */
@Slf4j
@Component
public class ProductItemProcessor implements ItemProcessor<ProductCsvRow, ProductImportEvent> {

    @Setter
    private UUID importJobId;

    @Override
    public ProductImportEvent process(ProductCsvRow row) {
        // Validate required fields
        if (row.getSku() == null || row.getSku().isBlank()) {
            log.warn("Skipping row — missing SKU");
            return null;
        }
        if (row.getName() == null || row.getName().isBlank()) {
            log.warn("Skipping row sku={} — missing name", row.getSku());
            return null;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(row.getPrice().trim());
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Skipping row sku={} — price must be > 0", row.getSku());
                return null;
            }
        } catch (NumberFormatException e) {
            log.warn("Skipping row sku={} — invalid price: {}", row.getSku(), row.getPrice());
            return null;
        }

        BigDecimal compareAtPrice = null;
        if (row.getCompareAtPrice() != null && !row.getCompareAtPrice().isBlank()) {
            try {
                compareAtPrice = new BigDecimal(row.getCompareAtPrice().trim());
            } catch (NumberFormatException ignored) {}
        }

        List<String> categories = row.getCategories() != null
                ? Arrays.asList(row.getCategories().split(","))
                : List.of();

        return new ProductImportEvent(
                UUID.randomUUID(),
                importJobId,
                0,                          // row number set by listener
                row.getSku().trim(),
                row.getName().trim(),
                row.getDescription(),
                row.getBrand(),
                categories,
                price,
                compareAtPrice,
                null,                       // attributes not in CSV
                "true".equalsIgnoreCase(row.getActive()),
                Instant.now()
        );
    }
}
