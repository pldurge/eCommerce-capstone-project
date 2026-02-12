package com.capstone.productcatalog.repositories;

import com.capstone.productcatalog.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findById(Long id);

    List<Product> findAll();

    Product save(Product product);


    List<Product> findByPriceBetween(Double low, Double high);

    List<Product> findAllByOrderByPrice();

    @Query("SELECT p.description FROM Product p WHERE p.id = :id") //HQL
    String findDescriptionWhereIdIs(@Param("id") Long id);

}
