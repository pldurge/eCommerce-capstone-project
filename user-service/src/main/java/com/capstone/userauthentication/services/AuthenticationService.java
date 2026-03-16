package com.capstone.userauthentication.services;

import com.capstone.userauthentication.configs.JwtService;
import com.capstone.userauthentication.configs.KafkaConfigs;
import com.capstone.userauthentication.dtos.UserDtos.*;
import com.capstone.userauthentication.exceptions.UserAlreadyExistsException;
import com.capstone.userauthentication.models.Role;
import com.capstone.userauthentication.models.User;
import com.capstone.userauthentication.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService implements IAuthenticationService{

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already in use " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .role(Role.CUSTOMER)
                .build();

        user = userRepository.save(user);

        kafkaTemplate.send(KafkaConfigs.USER_REGISTERED_TOPIC, user.getId().toString(),
                Map.of("userId", user.getId(), "email", user.getEmail(), "name", user.getName()));

        log.info("User Registered: {}", user.getEmail());
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtService.generateToken(user);
        //String refreshToken = jwtService.generateRefreshToken(user);

        kafkaTemplate.send(KafkaConfigs.USER_LOGGED_TOPIC, user.getId().toString(),
                Map.of("userId", user.getId(), "email", user.getEmail(), "name", user.getName()));

        log.info("User logged in: {}", user.getEmail());

        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    @Override
    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + email));

        return getUserProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + email));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());

        user = userRepository.save(user);

        return getUserProfileResponse(user);
    }

    private UserProfileResponse getUserProfileResponse(User user) {
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(user.getId());
        profile.setEmail(user.getEmail());
        profile.setName(user.getName());
        profile.setPhoneNumber(user.getPhoneNumber());
        profile.setRole(user.getRole().name());
        return profile;
    }

    @Override
    public void passwordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + request.getEmail()));

        String resetToken = java.util.UUID.randomUUID().toString();

        kafkaTemplate.send(KafkaConfigs.PASSWORD_RESET_TOPIC, user.getId().toString(),
                Map.of("email", user.getEmail(), "resetToken", resetToken,
                        "name", user.getName()));

        log.info("Password reset requested for: {}", user.getEmail());
    }

    @Override
    public AuthResponse registerAdmin(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already in use " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .role(Role.ADMIN)
                .build();

        user = userRepository.save(user);

        kafkaTemplate.send(KafkaConfigs.USER_REGISTERED_TOPIC, user.getId().toString(),
                Map.of("userId", user.getId(), "email", user.getEmail(), "name", user.getName()));

        log.info("User Registered: {}", user.getEmail());
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    @Override
    public UserProfileResponse getProfileById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        return getUserProfileResponse(user);

    }

}