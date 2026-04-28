package com.capstone.userauthentication.configs;

import com.capstone.userauthentication.models.Role;
import com.capstone.userauthentication.models.User;
import com.capstone.userauthentication.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Seed credentials — change via .env before going to production ────────
    private static final String ADMIN_EMAIL    = "admin@ecommerce.com";
    private static final String ADMIN_PASSWORD = "Admin@1234";
    private static final String ADMIN_NAME     = "Super Admin";
    private static final String ADMIN_PHONE    = "9000000000";

    @Override
    public void run(String... args) {
        // Idempotent — skip entirely if the admin already exists
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Admin user already exists — skipping seed");
            return;
        }

        User admin = User.builder()
                .name(ADMIN_NAME)
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .phoneNumber(ADMIN_PHONE)
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);
        log.info("──────────────────────────────────────────────────");
        log.info("  Admin user seeded successfully");
        log.info("  Email    : {}", ADMIN_EMAIL);
        log.info("  Password : {}", ADMIN_PASSWORD);
        log.info("  Change this password after first login!");
        log.info("──────────────────────────────────────────────────");
    }
}
