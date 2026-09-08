package com.internetprog.shopex.web;

import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Malformed and missing resources must produce the right status and the custom
 * error pages rather than a generic 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User admin() {
        return userRepository.findByEmail("admin@shopex.local").orElseThrow();
    }

    @Test
    void unknownProductIdReturnsTheCustom404() throws Exception {
        mockMvc.perform(get("/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("Page not found")));
    }

    @Test
    void nonNumericPathVariableReturnsTheCustom400() throws Exception {
        mockMvc.perform(get("/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"))
                .andExpect(content().string(containsString("Bad request")));
    }

    @Test
    void nonNumericQueryParameterReturnsTheCustom400() throws Exception {
        mockMvc.perform(get("/products").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"));
        mockMvc.perform(get("/products").param("categoryId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"));
    }

    @Test
    void outOfRangePageStillRendersTheCatalog() throws Exception {
        mockMvc.perform(get("/products").param("page", "9999")).andExpect(status().isOk());
        mockMvc.perform(get("/products").param("page", "-1")).andExpect(status().isOk());
    }

    @Test
    void unknownCategoryFilterIsIgnoredRatherThanFailing() throws Exception {
        mockMvc.perform(get("/products").param("categoryId", "999999")).andExpect(status().isOk());
    }

    @Test
    void searchTermsAreTreatedAsDataNotSql() throws Exception {
        mockMvc.perform(get("/products").param("q", "' OR 1=1--")).andExpect(status().isOk());
    }

    /**
     * A POST-only endpoint opened with GET used to fall through to the catch-all
     * handler and answer 500 with a stack trace in the log.
     */
    @Test
    void wrongHttpMethodReturns405NotAnInternalError() throws Exception {
        mockMvc.perform(get("/admin/products/1/delete").with(user(admin())))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(view().name("error/405"))
                .andExpect(content().string(containsString("Method not allowed")));
    }

    @Test
    void forbiddenPageIsRenderedForNonAdmins() throws Exception {
        User regular = userRepository.findByEmail("regular-403@example.com").orElseGet(() -> {
            User user = new User();
            user.setFirstName("Reg");
            user.setLastName("User");
            user.setEmail("regular-403@example.com");
            user.setPassword("hash");
            user.setRole("USER");
            user.setEnabled(true);
            return userRepository.save(user);
        });

        mockMvc.perform(get("/admin").with(user(regular)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanStillReachTheDashboard() throws Exception {
        mockMvc.perform(get("/admin").with(user(admin()))).andExpect(status().isOk());
    }
}
