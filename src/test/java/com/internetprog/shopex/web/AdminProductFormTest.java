package com.internetprog.shopex.web;

import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.ProductRepository;
import com.internetprog.shopex.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Regression tests for the admin product form, which used to fail with a 500 on
 * every open because the template passed a conditional expression as a fragment
 * parameter — meaning product CRUD was unusable from the UI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminProductFormTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail("admin@shopex.local").orElseThrow();
    }

    @Test
    void newProductFormRenders() throws Exception {
        mockMvc.perform(get("/admin/products/new").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("New Product")));
    }

    @Test
    void editProductFormRendersAndIsPrefilled() throws Exception {
        Product existing = productRepository.findAll().get(0);

        mockMvc.perform(get("/admin/products/{id}/edit", existing.getId()).with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Edit Product")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(existing.getName())));
    }

    @Test
    void creatingAValidProductPersistsItAndRedirects() throws Exception {
        long before = productRepository.count();

        mockMvc.perform(post("/admin/products").with(user(admin)).with(csrf())
                        .param("name", "Test Widget")
                        .param("description", "A widget for tests")
                        .param("price", "19.99")
                        .param("stock", "5"))
                .andExpect(status().is3xxRedirection());

        assertThat(productRepository.count()).isEqualTo(before + 1);
        assertThat(productRepository.findAll()).anyMatch(p -> "Test Widget".equals(p.getName()));
    }

    @Test
    void invalidProductRedisplaysTheFormWithErrorsAndSavesNothing() throws Exception {
        long before = productRepository.count();

        mockMvc.perform(post("/admin/products").with(user(admin)).with(csrf())
                        .param("name", "")
                        .param("price", "-5")
                        .param("stock", "-3"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "name", "price", "stock"));

        assertThat(productRepository.count()).isEqualTo(before);
    }

    @Test
    void zeroPriceIsRejected() throws Exception {
        mockMvc.perform(post("/admin/products").with(user(admin)).with(csrf())
                        .param("name", "Free Widget")
                        .param("price", "0")
                        .param("stock", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("productForm", "price"));

        assertThat(productRepository.findAll()).noneMatch(p -> "Free Widget".equals(p.getName()));
    }

    @Test
    void updatingAProductChangesItInPlace() throws Exception {
        Product existing = productRepository.findAll().get(0);
        Long id = existing.getId();

        mockMvc.perform(post("/admin/products/{id}", id).with(user(admin)).with(csrf())
                        .param("name", "Renamed Product")
                        .param("price", "77.70")
                        .param("stock", "12"))
                .andExpect(status().is3xxRedirection());

        Product updated = productRepository.findById(id).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Renamed Product");
        assertThat(updated.getPrice()).isEqualByComparingTo("77.70");
        assertThat(updated.getStock()).isEqualTo(12);
    }
}
