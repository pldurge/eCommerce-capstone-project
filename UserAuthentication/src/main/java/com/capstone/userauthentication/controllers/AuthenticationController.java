package com.capstone.userauthentication.controllers;

import com.capstone.userauthentication.dtos.LoginRequestDTO;
import com.capstone.userauthentication.dtos.SignupRequestDTO;
import com.capstone.userauthentication.dtos.UserDTO;
import com.capstone.userauthentication.models.User;
import com.capstone.userauthentication.services.IAuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final IAuthenticationService authenticationService;

    public AuthenticationController(IAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signUp(@RequestBody SignupRequestDTO requestDTO){
        User user = authenticationService.signup(requestDTO.getName(), requestDTO.getEmail(), requestDTO.getPassword());
        return new ResponseEntity<>(user.convertToUserDTO(), HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@RequestBody LoginRequestDTO requestDTO){
        User user = authenticationService.login(requestDTO.getEmail(), requestDTO.getPassword());
        return new  ResponseEntity<>(user.convertToUserDTO(), HttpStatus.OK);
    }
}
