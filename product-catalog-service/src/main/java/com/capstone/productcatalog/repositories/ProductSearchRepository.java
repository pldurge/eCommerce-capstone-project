package com.capstone.productcatalog.repositories;

import com.capstone.productcatalog.models.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name^3\", \"description\", \"category\"], \"fuzziness\": \"AUTO\"}}")
    Page<ProductDocument> searchByKeyword(String keyword, Pageable pageable);
    Page<ProductDocument> findByCategory(String category, Pageable pageable);
}

