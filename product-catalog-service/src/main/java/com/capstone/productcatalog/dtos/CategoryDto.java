package com.capstone.productcatalog.dtos;

import com.capstone.productcatalog.models.Category;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
public class CategoryDto {
    private UUID id;
    private String name;
    private String description;

    public Category toCategory() {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription(description);
        return category;
    }
}
