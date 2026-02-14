package com.capstone.userauthentication.services;


import com.capstone.userauthentication.models.User;
import com.capstone.userauthentication.pojos.UserToken;

public interface IAuthenticationService {
    User signup(String name, String email, String password);
    UserToken login(String email, String password);
    boolean validateToken(String token);
}
