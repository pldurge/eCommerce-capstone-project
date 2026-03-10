package com.capstone.productcatalog.controllers;


import com.capstone.productcatalog.dtos.CategoryDto;
import com.capstone.productcatalog.dtos.ProductDto;
import com.capstone.productcatalog.models.Product;
import com.capstone.productcatalog.services.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/productapis")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    @GetMapping("/products")
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy){
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductByIdToClient(id));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductDto>> getProductsByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId, pageable));
    }


    @PostMapping("/product")
    public ResponseEntity<ProductDto> createProduct(@RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(product));
    }

    @PatchMapping("/updateproduct/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable UUID id, @RequestBody ProductDto dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto.toProduct()));
    }

    @PutMapping("/replaceproduct/{id}")
    public ResponseEntity<ProductDto> replaceProduct(@PathVariable UUID id, @RequestBody ProductDto dto) {
        return ResponseEntity.ok(productService.replaceProduct(id, dto.toProduct()));
    }

    @DeleteMapping("/product/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<Page<CategoryDto>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.getAllCategories(pageable));
    }

    @PostMapping("/category")
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createCategory(dto.toCategory()));
    }


}
/*
path variable /id/
request body { "" : ""
Query params ?category=electronics


2 ways to solve ambiguity
1. Primary implementation
2. Qualifer
 */
/*
Primary
Qualifer
 */