package com.capstone.userauthentication.services;


import com.capstone.userauthentication.dtos.UserDtos.*;

import java.util.UUID;

public interface IAuthenticationService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserProfileResponse getProfile(String email);
    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);
    void passwordReset(PasswordResetRequest request);
    UserProfileResponse getProfileById(UUID userId);
    AuthResponse registerAdmin(RegisterRequest request);
}
