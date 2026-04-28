package com.capstone.productcatalog.repositories;

import com.capstone.productcatalog.models.Product;
import com.capstone.productcatalog.models.State;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);
    Page<Product> findByState(State state, Pageable pageable);
    Page<Product> findByPriceBetween(BigDecimal lowPrice, BigDecimal highPrice, Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
            "WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int reduceStock(@Param("productId") UUID productId, @Param("quantity") int quantity);
}