package com.capstone.productcatalog.services;

import com.capstone.productcatalog.models.Product;

import java.util.List;

public interface IProductService {
    Product getProductById(Long id);
    List<Product> getAllProducts();
    Product createProduct(Product input);
    Product replaceProduct(Product input, Long productId);
}
