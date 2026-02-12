package com.capstone.productcatalog.dtos;


import com.capstone.productcatalog.models.Category;
import com.capstone.productcatalog.models.Product;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FakeStoreProductDto {
    private Long id;
    private String title;
    private String description;
    private String image;
    private Double price;
    private String category;

    public Product convertToProduct(){
        Product product = new Product();
        product.setId(id);
        product.setName(title);
        product.setDescription(description);
        product.setPrice(price);
        product.setImageUrl(image);

        Category category1 = new Category();
        category1.setName(this.category);
        product.setCategory(category1);

        return product;
    }
}
