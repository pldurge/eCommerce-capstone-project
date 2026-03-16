package com.capstone.productcatalog.models;

import com.capstone.productcatalog.dtos.CategoryDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "categories")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseModel{

    @Column(nullable = false, unique = true)
    private String name;
    private String description;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products;

    public CategoryDto toDto() {
        CategoryDto dto = new CategoryDto();
        dto.setId(this.getId());
        dto.setName(this.getName());
        dto.setDescription(this.getDescription());
        return dto;
    }

}
