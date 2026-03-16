package com.capstone.userauthentication.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

public class UserDtos {

    @Data
    public static class RegisterRequest{
        @NotBlank @Email
        private String email;

        @NotBlank @Size(min = 8, message = "Password must be at-least 8 character")
        private String password;

        @NotBlank
        private String name;

        private String phoneNumber;
    }

    @Data
    public static class LoginRequest{
        @NotBlank @Email
        private String email;

        @NotBlank
        private String password;
    }

    @Data
    @AllArgsConstructor
    public static class AuthResponse{
        private String token;
        private String email;
        private String name;
        private String role;
    }

    @Data
    public static class UserProfileResponse{
        private UUID id;
        private String email;
        private String name;
        private String phoneNumber;
        private String role;
    }

    @Data
    public static class UpdateProfileRequest{
        private String name;
        private String phoneNumber;
    }

    @Data
    public static class PasswordResetRequest{
        @NotBlank @Email
        private String email;
    }

    @Data
    public static class PasswordChangeRequest{

        @NotBlank @Size(min = 8, message = "Password must be of at-least 8 character")
        private String newPassword;

        @NotBlank
        private String resetToken;
    }

}
