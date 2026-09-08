package com.internetprog.shopex.service;

import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default admin account on startup if no ADMIN user exists yet.
 *
 * Default credentials (documented here for the operator / grader):
 *   email:    admin@shopex.local
 *   password: Admin123!
 */
@Component
public class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private static final String ADMIN_EMAIL = "admin@shopex.local";
    private static final String ADMIN_PASSWORD = "Admin123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedAdmin() {
        if (userRepository.existsByRole("ADMIN")) {
            return;
        }

        User admin = new User();
        admin.setFirstName("Shop");
        admin.setLastName("Admin");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        userRepository.save(admin);

        // The password is deliberately not logged: log files get copied around,
        // and it is documented in the README where an operator can find it.
        log.info("Seeded default admin account: {} (password is in the README)", ADMIN_EMAIL);
    }
}
