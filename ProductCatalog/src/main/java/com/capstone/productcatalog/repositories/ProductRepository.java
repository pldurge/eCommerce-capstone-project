package com.capstone.productcatalog.repositories;

import com.capstone.productcatalog.models.Product;
import com.capstone.productcatalog.models.State;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);
    Page<Product> findByState(State state, Pageable pageable);
    Page<Product> findByPriceBetween(BigDecimal lowPrice, BigDecimal highPrice, Pageable pageable);

}
