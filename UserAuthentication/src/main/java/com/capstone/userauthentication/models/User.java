package com.capstone.userauthentication.models;

import com.capstone.userauthentication.dtos.UserDTO;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
public class User extends BaseModel{
    private String name;
    private String password;
    private String email;

    @ManyToMany
    private List<Role> roles;

    public UserDTO convertToUserDTO() {
        UserDTO userDto = new UserDTO();
        userDto.setEmail(this.email);
        userDto.setName(this.name);
        userDto.setRoles(this.roles);
        userDto.setId(this.getId());
        return userDto;
    }
}
