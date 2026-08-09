package com.internetprog.shopex.web;

import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Route-level access control: the same matrix for anonymous / user / admin,
 * plus CSRF enforcement on state-changing requests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User regularUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        regularUser = userRepository.findByEmail("shopper@example.com").orElseGet(() -> {
            User user = new User();
            user.setFirstName("Reg");
            user.setLastName("Shopper");
            user.setEmail("shopper@example.com");
            user.setPassword(passwordEncoder.encode("Passw0rd!"));
            user.setRole("USER");
            user.setEnabled(true);
            return userRepository.save(user);
        });
        adminUser = userRepository.findByEmail("admin@shopex.local").orElseThrow();
    }

    // --- public pages ---

    @Test
    void publicPagesAreReachableAnonymously() throws Exception {
        for (String path : new String[]{"/", "/products", "/cart", "/login", "/register"}) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    // --- protected pages ---

    @Test
    void protectedPagesRedirectAnonymousUsersToLogin() throws Exception {
        for (String path : new String[]{"/profile", "/checkout", "/admin", "/admin/products", "/admin/orders", "/admin/users"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    void adminPagesAreForbiddenForRegularUsers() throws Exception {
        for (String path : new String[]{"/admin", "/admin/products", "/admin/products/new", "/admin/orders", "/admin/users"}) {
            mockMvc.perform(get(path).with(user(regularUser)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminPagesAreReachableForAdmins() throws Exception {
        for (String path : new String[]{"/admin", "/admin/products", "/admin/products/new", "/admin/orders", "/admin/users"}) {
            mockMvc.perform(get(path).with(user(adminUser)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void ownPagesAreReachableForLoggedInUsers() throws Exception {
        mockMvc.perform(get("/profile").with(user(regularUser))).andExpect(status().isOk());
        mockMvc.perform(get("/checkout").with(user(regularUser))).andExpect(status().isOk());
    }

    // --- admin write endpoints ---

    @Test
    void adminWriteEndpointsAreForbiddenForRegularUsers() throws Exception {
        mockMvc.perform(post("/admin/products").with(user(regularUser)).with(csrf())
                        .param("name", "Hacked").param("price", "1").param("stock", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/products/1/delete").with(user(regularUser)).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/" + adminUser.getId() + "/toggle-enabled")
                        .with(user(regularUser)).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findByEmail("admin@shopex.local").orElseThrow().isEnabled()).isTrue();
    }

    // --- CSRF ---

    @Test
    void stateChangingRequestsWithoutCsrfTokenAreRejected() throws Exception {
        mockMvc.perform(post("/cart/add").param("productId", "1").param("quantity", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/products").with(user(adminUser))
                        .param("name", "NoCsrf").param("price", "1").param("stock", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/logout").with(user(regularUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfProtectedRequestSucceedsWithAToken() throws Exception {
        mockMvc.perform(post("/cart/add").with(csrf())
                        .param("productId", "1").param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
    }

    // --- security headers (OWASP baseline that Spring Security applies) ---

    @Test
    void securityHeadersArePresent() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Cache-Control"));
    }
}
