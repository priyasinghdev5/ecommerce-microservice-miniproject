package com.ecom.product.search;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Elasticsearch index document — the search read model.
 * Synced from MongoDB via Kafka events.
 * Optimized for full-text search, filtering and sorting.
 */
@Document(indexName = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductSearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String sku;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "english")
    private String description;

    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Keyword)
    private List<String> categories;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Float)
    private double averageRating;

    @Field(type = FieldType.Integer)
    private int reviewCount;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant updatedAt;
}
