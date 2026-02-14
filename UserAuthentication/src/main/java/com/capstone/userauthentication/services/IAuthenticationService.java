package com.capstone.userauthentication.services;


import com.capstone.userauthentication.models.User;

public interface IAuthenticationService {
    User signup(String name, String email, String password);
    User login(String email, String password);
}
