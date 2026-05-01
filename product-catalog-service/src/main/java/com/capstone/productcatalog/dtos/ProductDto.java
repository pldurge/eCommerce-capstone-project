package com.capstone.productcatalog.dtos;



import com.capstone.productcatalog.models.Category;
import com.capstone.productcatalog.models.Product;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProductDto {
    private UUID id;
    @NotBlank(message = "Product name is required")
    private String name;
    private String description;

    @NotNull(message = "Category is required (send category object with id)")
    private CategoryDto category;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be zero or greater")
    private Integer stockQuantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;
    private String imageUrl;

    public Product toProduct() {
        Product product = new Product();
        product.setId(this.getId());
        product.setName(this.getName());
        product.setDescription(this.getDescription());
        product.setStockQuantity(this.getStockQuantity());
        product.setPrice(this.getPrice());
        product.setImageUrl(this.getImageUrl());
        if (this.getCategory() != null) {
            Category category1 = new Category();
            category1.setName(this.getCategory().getName());
            category1.setId(this.getCategory().getId());
            category1.setDescription(this.getCategory().getDescription());
            product.setCategory(category1);
        }
        return product;
    }
}
