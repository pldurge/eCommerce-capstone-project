package com.capstone.productcatalog.services;

import com.capstone.productcatalog.dtos.CategoryDto;
import com.capstone.productcatalog.dtos.ProductDto;
import com.capstone.productcatalog.exceptions.ProductNotFoundException;
import com.capstone.productcatalog.models.Category;
import com.capstone.productcatalog.models.Product;
import com.capstone.productcatalog.models.ProductDocument;
import com.capstone.productcatalog.models.State;
import com.capstone.productcatalog.repositories.CategoryRepository;
import com.capstone.productcatalog.repositories.ProductRepository;
import com.capstone.productcatalog.repositories.ProductSearchRepository;
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
    private final ProductSearchRepository productSearchRepository;

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
    public Page<ProductDocument> searchProducts(String keyword, Pageable pageable) {
        return productSearchRepository.searchByKeyword(keyword, pageable);
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
    public ProductDto createProduct(ProductDto dto) {
        if (dto.getName() == null || dto.getName().isBlank())
            throw new IllegalArgumentException("Product name is required");
        if (dto.getPrice() == null || dto.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Product price must be greater than zero");
        if (dto.getStockQuantity() == null || dto.getStockQuantity() < 0)
            throw new IllegalArgumentException("Stock quantity must be zero or greater");
        if (dto.getCategory() == null || dto.getCategory().getId() == null)
            throw new IllegalArgumentException("A valid category is required (send category.id)");

        Category category = categoryRepository.findById(dto.getCategory().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found with id: " + dto.getCategory().getId()));

        Product product = dto.toProduct();
        product.setCategory(category);
        product.setState(State.ACTIVE);
        product = productRepository.save(product);
        indexProduct(product);
        return product.toDto();
    }

    @Override
    @Transactional
    public ProductDto replaceProduct(UUID id, ProductDto newProduct) {
        Product existing = getProductById(id);

        existing.setName(newProduct.getName());
        existing.setDescription(newProduct.getDescription());
        existing.setPrice(newProduct.getPrice());
        existing.setStockQuantity(newProduct.getStockQuantity());
        existing.setImageUrl(newProduct.getImageUrl());
        existing.setCategory(newProduct.getCategory().toCategory());
        existing = productRepository.save(existing);
        indexProduct(existing);
        return existing.toDto();
    }

    @Override
    @Transactional
    public ProductDto updateProduct(UUID id, ProductDto updatedProduct) {
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

        existing =  productRepository.save(existing);
        return existing.toDto();
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product =  getProductById(id);
        product.setState(State.INACTIVE);
        productRepository.save(product);

        ProductDocument productDocument = productSearchRepository.findById(id.toString())
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        productDocument.setState("INACTIVE");
        productSearchRepository.save(productDocument);
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



    private void indexProduct(Product product) {
        ProductDocument doc = ProductDocument.builder()
                .id(product.getId().toString())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .state(product.getState().name())
                .category(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrl(product.getImageUrl())
                .build();
        productSearchRepository.save(doc);
        log.info("Product indexed in Elasticsearch: {}", product.getId());
    }

    @Override
    @Transactional
    public boolean reduceStock(UUID productId, int quantity) {
        int updated = productRepository.reduceStock(productId, quantity);
        if (updated == 0) {
            log.warn("Insufficient stock for product={} qty={}", productId, quantity);
            return false;
        }
        log.info("Stock reduced by {} for product={}", quantity, productId);
        return true;
    }


}