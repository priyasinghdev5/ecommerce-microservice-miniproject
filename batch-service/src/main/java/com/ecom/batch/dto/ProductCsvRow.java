package com.ecom.batch.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.*;

/**
 * Maps CSV columns to Java fields using OpenCSV.
 * Column names must match exactly (case-insensitive).
 *
 * Expected CSV format:
 * sku,name,description,brand,categories,price,compare_at_price,active
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductCsvRow {

    @CsvBindByName(column = "sku", required = true)
    private String sku;

    @CsvBindByName(column = "name", required = true)
    private String name;

    @CsvBindByName(column = "description")
    private String description;

    @CsvBindByName(column = "brand")
    private String brand;

    @CsvBindByName(column = "categories")
    private String categories;   // comma-separated e.g. "footwear,sports"

    @CsvBindByName(column = "price", required = true)
    private String price;        // String to handle validation

    @CsvBindByName(column = "compare_at_price")
    private String compareAtPrice;

    @CsvBindByName(column = "active")
    private String active = "true";
}
