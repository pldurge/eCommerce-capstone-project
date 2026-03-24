package com.capstone.userauthentication.services;

import com.capstone.userauthentication.configs.JwtService;
import com.capstone.userauthentication.configs.KafkaConfigs;
import com.capstone.userauthentication.dtos.UserDtos.*;
import com.capstone.userauthentication.exceptions.SessionExpiredException;
import com.capstone.userauthentication.exceptions.UserAlreadyExistsException;
import com.capstone.userauthentication.models.Role;
import com.capstone.userauthentication.models.User;
import com.capstone.userauthentication.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService{

    private static final String BLACKLIST_PREFIX  = "blacklist:";
    private static final String REFRESH_PREFIX    = "refresh:";
    private static final String PWD_RESET_PREFIX  = "pwd_reset:";
    private static final long   PWD_RESET_TTL_SEC = 3600; // 1 hour

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;


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
        String accessToken  = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        storeRefreshToken(refreshToken, user.getEmail());
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getName(), user.getRole().name());
    }


    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        storeRefreshToken(refreshToken, user.getEmail());

        kafkaTemplate.send(KafkaConfigs.USER_LOGGED_TOPIC, user.getId().toString(),
                Map.of("userId", user.getId(), "email", user.getEmail(), "name", user.getName()));

        log.info("User logged in: {}", user.getEmail());

        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getName(), user.getRole().name());
    }

    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        String storedEmail = redisTemplate.opsForValue().get(REFRESH_PREFIX + request.getRefreshToken());
        if(storedEmail == null) {
            throw new SessionExpiredException("Refresh token is invalid or expired. Please log in again.");
        }

        User user = userRepository.findByEmail(storedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!jwtService.isRefreshTokenValid(request.getRefreshToken(), user)) {
            redisTemplate.delete(REFRESH_PREFIX + request.getRefreshToken());
            throw new SessionExpiredException("Refresh token is invalid or expired. Please log in again.");
        }

        redisTemplate.delete(REFRESH_PREFIX + request.getRefreshToken());
        String newAccessToken  = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        storeRefreshToken(newRefreshToken, user.getEmail());

        log.info("Tokens refreshed for: {}", user.getEmail());
        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    public void logout(LogoutRequest request) {
        // Blacklist the access token for its remaining lifetime
        long remainingMillis = jwtService.getExpirationMillis(request.getAccessToken());
        if (remainingMillis > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + request.getAccessToken(),
                    "true",
                    remainingMillis,
                    TimeUnit.MILLISECONDS
            );
        }
        // Invalidate the refresh token immediately
        redisTemplate.delete(REFRESH_PREFIX + request.getRefreshToken());
        log.info("User logged out, tokens invalidated");
    }


    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + email));

        return getUserProfileResponse(user);
    }


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


    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + request.getEmail()));

        String resetToken = UUID.randomUUID().toString();
        // Store token → email mapping with 1-hour TTL in Redis
        redisTemplate.opsForValue().set(
                PWD_RESET_PREFIX + resetToken,
                user.getEmail(),
                PWD_RESET_TTL_SEC,
                TimeUnit.SECONDS
        );

        kafkaTemplate.send(KafkaConfigs.PASSWORD_RESET_TOPIC, user.getId().toString(),
                Map.of("email", user.getEmail(), "resetToken", resetToken,
                        "name", user.getName()));

        log.info("Password reset token stored in Redis for: {}", user.getEmail());
    }

    @Transactional
    public void confirmPasswordReset(PasswordChangeRequest request) {
        String email = redisTemplate.opsForValue().get(PWD_RESET_PREFIX + request.getResetToken());
        if (email == null) {
            throw new RuntimeException("Password reset token is invalid or has expired.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + email));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Consume the token so it cannot be reused
        redisTemplate.delete(PWD_RESET_PREFIX + request.getResetToken());
        log.info("Password successfully reset for: {}", email);
    }


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

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        storeRefreshToken(refreshToken, user.getEmail());

        kafkaTemplate.send(KafkaConfigs.USER_REGISTERED_TOPIC, user.getId().toString(),
                Map.of("userId", user.getId(), "email", user.getEmail(), "name", user.getName()));

        log.info("Admin User Registered: {}", user.getEmail());
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getName(), user.getRole().name());
    }


    public UserProfileResponse getProfileById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        return getUserProfileResponse(user);

    }

    private void storeRefreshToken(String token, String email) {
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + token,
                email,
                jwtService.getRefreshExpirationMillis(),
                TimeUnit.MILLISECONDS
        );
    }

}