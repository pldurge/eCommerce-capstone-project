package com.capstone.userauthentication.controllers;


import com.capstone.userauthentication.services.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.capstone.userauthentication.dtos.UserDtos.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    // ─── Auth Endpoints ──────────────────────────────────────────────────────
    @PostMapping("/api/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.register(request));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    // ─── User Profile Endpoints ──────────────────────────────────────────────

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refreshToken(request));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request);
        return ResponseEntity.noContent().build();
    }

    // Step 1 — request password reset email
    @PostMapping("/api/auth/password-reset")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authenticationService.requestPasswordReset(request);
        return ResponseEntity.accepted().build();
    }

    //  Step 2 — confirm reset using the token from the email link
    @PostMapping("/api/auth/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordChangeRequest request) {
        authenticationService.confirmPasswordReset(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/users/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authenticationService.getProfile(userDetails.getUsername()));
    }

    @PutMapping("/api/users/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authenticationService.updateProfile(userDetails.getUsername(), request));
    }

    @PostMapping("/api/admin/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> createAdmin(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.registerAdmin(request));
    }

    // FIX 3 (internal): Called by notification-service to resolve userId → email
    @GetMapping("/api/users/internal/{userId}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable UUID userId) {
        // This endpoint is internal — protected by network policy, not exposed via API Gateway
        return ResponseEntity.ok(authenticationService.getProfileById(userId));
    }
}
