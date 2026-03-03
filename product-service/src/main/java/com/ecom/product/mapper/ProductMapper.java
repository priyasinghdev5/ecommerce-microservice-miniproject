package com.ecom.product.mapper;

import com.ecom.product.document.ProductDocument;
import com.ecom.product.dto.*;
import com.ecom.product.search.ProductSearchDocument;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(ProductDocument doc);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "variants", ignore = true)
    ProductDocument toDocument(ProductRequest request);

    @Mapping(target = "reviewCount", source = "reviewCount")
    ProductSearchDocument toSearchDocument(ProductDocument doc);
}
