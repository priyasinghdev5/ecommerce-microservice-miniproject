package com.ecom.product.repository;

import com.ecom.product.document.ProductDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<ProductDocument, String> {
    Optional<ProductDocument> findBySku(String sku);
    List<ProductDocument> findByActiveTrueAndCategoriesContaining(String category);
    List<ProductDocument> findByActiveTrueOrderByCreatedAtDesc();
    boolean existsBySku(String sku);
}
