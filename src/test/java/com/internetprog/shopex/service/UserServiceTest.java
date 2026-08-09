package com.internetprog.shopex.service;

import com.internetprog.shopex.dto.RegistrationForm;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({UserService.class, UserServiceTest.PasswordEncoderConfig.class})
class UserServiceTest {

    @TestConfiguration
    static class PasswordEncoderConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegistrationForm form;

    @BeforeEach
    void setUp() {
        form = new RegistrationForm();
        form.setFirstName("Dana");
        form.setLastName("Cohen");
        form.setEmail("dana@example.com");
        form.setPassword("Passw0rd!");
        form.setConfirmPassword("Passw0rd!");
    }

    @Test
    void register_storesTheUserWithAHashedPassword() {
        User created = userService.register(form);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getRole()).isEqualTo("USER");
        assertThat(created.isEnabled()).isTrue();
        assertThat(created.getPassword())
                .isNotEqualTo("Passw0rd!")
                .startsWith("$2");
        assertThat(passwordEncoder.matches("Passw0rd!", created.getPassword())).isTrue();
    }

    @Test
    void emailIsTaken_reflectsWhatIsStored() {
        assertThat(userService.emailIsTaken("dana@example.com")).isFalse();
        userService.register(form);
        assertThat(userService.emailIsTaken("dana@example.com")).isTrue();
        assertThat(userService.emailIsTaken(null)).isFalse();
    }

    @Test
    void passwordsMatch_isPartOfTheForm() {
        assertThat(form.passwordsMatch()).isTrue();
        form.setConfirmPassword("something-else");
        assertThat(form.passwordsMatch()).isFalse();
    }

    @Test
    void toggleEnabled_flipsTheFlag() {
        User target = userService.register(form);
        User admin = adminUser();

        assertThat(userService.toggleEnabled(target.getId(), admin).isEnabled()).isFalse();
        assertThat(userService.toggleEnabled(target.getId(), admin).isEnabled()).isTrue();
    }

    @Test
    void toggleEnabled_refusesToDisableYourOwnAccount() {
        User admin = adminUser();

        assertThatThrownBy(() -> userService.toggleEnabled(admin.getId(), admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("your own account");

        assertThat(userRepository.findById(admin.getId()).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void toggleEnabled_rejectsAnUnknownUser() {
        assertThatThrownBy(() -> userService.toggleEnabled(999_999L, adminUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void countAndFindAll_seeRegisteredUsers() {
        long before = userService.count();
        userService.register(form);

        assertThat(userService.count()).isEqualTo(before + 1);
        assertThat(userService.findAll()).anyMatch(u -> "dana@example.com".equals(u.getEmail()));
    }

    private User adminUser() {
        User admin = new User();
        admin.setFirstName("Shop");
        admin.setLastName("Admin");
        admin.setEmail("admin-under-test@example.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        return userRepository.save(admin);
    }
}
