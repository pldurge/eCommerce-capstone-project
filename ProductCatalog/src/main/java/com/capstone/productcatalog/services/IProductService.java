package com.capstone.productcatalog.services;

import com.capstone.productcatalog.dtos.CategoryDto;
import com.capstone.productcatalog.dtos.ProductDto;
import com.capstone.productcatalog.models.Category;
import com.capstone.productcatalog.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IProductService {
    Page<ProductDto> getAllProducts(Pageable pageable);
    ProductDto getProductByIdToClient(UUID id);
    Product getProductById(UUID id);
    Page<ProductDto> getProductsByCategory(UUID categoryId, Pageable pageable);
    ProductDto createProduct(Product product);
    ProductDto replaceProduct(UUID id, Product product);
    ProductDto updateProduct(UUID id, Product updatedProduct);
    void deleteProduct(UUID id);
    Page<CategoryDto> getAllCategories(Pageable pageable);
    CategoryDto createCategory(Category category);
}
