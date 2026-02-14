package com.capstone.userauthentication.dtos;

import com.capstone.userauthentication.models.Role;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private List<Role> roles;

}