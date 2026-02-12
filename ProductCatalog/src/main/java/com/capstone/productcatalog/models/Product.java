package com.capstone.productcatalog.models;


import com.capstone.productcatalog.dtos.CategoryDto;
import com.capstone.productcatalog.dtos.FakeStoreProductDto;
import com.capstone.productcatalog.dtos.ProductDto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;

import jakarta.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Product extends BaseModel{
    private String name;
    private String description;
    private double price;
    private String imageUrl;

    @ManyToOne(cascade = CascadeType.ALL)
    private Category category;

    public FakeStoreProductDto convertToFakeStoreProduct(){
        FakeStoreProductDto fakeStoreProductDto = new FakeStoreProductDto();
        fakeStoreProductDto.setId(this.getId());
        fakeStoreProductDto.setTitle(this.getName());
        fakeStoreProductDto.setPrice(this.getPrice());
        fakeStoreProductDto.setDescription(this.getDescription());
        fakeStoreProductDto.setImage(this.getImageUrl());
        if(this.getCategory() != null) {
            fakeStoreProductDto.setCategory(this.getCategory().getName());
        }
        return fakeStoreProductDto;
    }

    public ProductDto convertToProductDto(){
        ProductDto productDto = new ProductDto();
        productDto.setId(this.getId());
        productDto.setName(this.getName());
        productDto.setDescription(this.getDescription());
        productDto.setPrice(this.getPrice());
        productDto.setImageUrl(this.getImageUrl());
        if(this.getCategory() != null) {
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setName(this.getCategory().getName());
            categoryDto.setId(this.getCategory().getId());
            categoryDto.setDescription(this.getCategory().getDescription());
            productDto.setCategory(categoryDto);
        }
        return productDto;

    }
}
