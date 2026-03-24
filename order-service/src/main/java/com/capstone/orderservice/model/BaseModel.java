package com.capstone.orderservice.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
public abstract class BaseModel {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(updatable = false, nullable = false)
    private Date createdAt;
    private Date lastUpdatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = new Date();
        lastUpdatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate(){
        lastUpdatedAt = new Date();
    }

}
