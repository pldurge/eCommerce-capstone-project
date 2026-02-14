package com.capstone.userauthentication.pojos;

import com.capstone.userauthentication.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserToken {
    private String token;
    private User user;
}
