package com.capstone.productcatalog.services;

import com.capstone.productcatalog.dtos.CategoryDto;
import com.capstone.productcatalog.dtos.ProductDto;
import com.capstone.productcatalog.exceptions.ProductNotFoundException;
import com.capstone.productcatalog.models.Category;
import com.capstone.productcatalog.models.Product;
import com.capstone.productcatalog.models.State;
import com.capstone.productcatalog.repositories.CategoryRepository;
import com.capstone.productcatalog.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageProductService implements IProductService{

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Page<ProductDto> getAllProducts(Pageable pageable) {
        Page<Product> products =  productRepository.findByState(State.ACTIVE, pageable);
        return products.map(Product::toDto);
    }

    @Override
    public ProductDto getProductByIdToClient(UUID id) {
        Product product =  productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        return product.toDto();
    }

    @Override
    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    @Override
    public Page<ProductDto> getProductsByCategory(UUID categoryId, Pageable pageable) {
        Page<Product> products =  productRepository.findByCategoryId(categoryId, pageable);
        return products.map(Product::toDto);
    }

    @Override
    @Transactional
    public ProductDto createProduct(Product product) {
        Product savedProduct = productRepository.save(product);
        return savedProduct.toDto();
    }

    @Override
    @Transactional
    public ProductDto replaceProduct(UUID id, Product newProduct) {
        Product existing = getProductById(id);

        existing.setName(newProduct.getName());
        existing.setDescription(newProduct.getDescription());
        existing.setPrice(newProduct.getPrice());
        existing.setStockQuantity(newProduct.getStockQuantity());
        existing.setImageUrl(newProduct.getImageUrl());
        existing.setCategory(newProduct.getCategory());
        return productRepository.save(existing).toDto();
    }

    @Override
    @Transactional
    public ProductDto updateProduct(UUID id, Product updatedProduct) {
        Product existing = getProductById(id);

        if (updatedProduct.getName() != null) {
            existing.setName(updatedProduct.getName());
        }

        if (updatedProduct.getDescription() != null) {
            existing.setDescription(updatedProduct.getDescription());
        }

        if (updatedProduct.getPrice() != null) {
            existing.setPrice(updatedProduct.getPrice());
        }

        if (updatedProduct.getStockQuantity() != null) {
            existing.setStockQuantity(updatedProduct.getStockQuantity());
        }

        if (updatedProduct.getImageUrl() != null) {
            existing.setImageUrl(updatedProduct.getImageUrl());
        }

        return productRepository.save(existing).toDto();
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product =  getProductById(id);
        product.setState(State.INACTIVE);
        productRepository.save(product);
    }

    @Override
    public Page<CategoryDto> getAllCategories(Pageable pageable) {
        Page<Category> categories = categoryRepository.findAll(pageable);
        return categories.map(Category::toDto);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(Category category) {
        return categoryRepository.save(category).toDto();
    }

}
