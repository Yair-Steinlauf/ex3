package com.internetprog.shopex.service;

import com.internetprog.shopex.dto.RegistrationForm;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Accounts: registration and the admin-side user management. Controllers never
 * touch UserRepository directly — the rules about who may be disabled and how a
 * password is stored live here.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailIsTaken(String email) {
        return email != null && userRepository.findByEmail(email).isPresent();
    }

    /**
     * Creates a regular user account with a hashed password. Callers are
     * expected to have validated the form and checked {@link #emailIsTaken}.
     */
    @Transactional
    public User register(RegistrationForm form) {
        User user = new User();
        user.setFirstName(form.getFirstName());
        user.setLastName(form.getLastName());
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole("USER");
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public long count() {
        return userRepository.count();
    }

    /**
     * Flips a user's enabled flag.
     *
     * @throws IllegalStateException if the user does not exist, or if an admin
     *                               tries to disable their own account
     */
    @Transactional
    public User toggleEnabled(Long id, User actingUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("User not found."));

        if (actingUser != null && actingUser.getEmail() != null
                && actingUser.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new IllegalStateException("You cannot disable your own account.");
        }

        user.setEnabled(!user.isEnabled());
        return userRepository.save(user);
    }
}
