package com.capstone.productcatalog.dtos;



import com.capstone.productcatalog.models.Category;
import com.capstone.productcatalog.models.Product;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private CategoryDto category;
    private Double price;
    private String imageUrl;

    public Product convertToProduct(){
        Product product = new Product();
        product.setId(this.getId());
        product.setName(this.getName());
        product.setDescription(this.getDescription());
        product.setPrice(this.getPrice());
        product.setImageUrl(this.getImageUrl());
        if(product.getCategory() != null) {
            Category category1 = new Category();
            category1.setName(this.getCategory().getName());
            category1.setId(this.getCategory().getId());
            category1.setDescription(this.getCategory().getDescription());
            product.setCategory(category1);
        }
        return product;
    }
}
