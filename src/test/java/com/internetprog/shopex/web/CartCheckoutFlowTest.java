package com.internetprog.shopex.web;

import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.OrderRepository;
import com.internetprog.shopex.repository.ProductRepository;
import com.internetprog.shopex.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The shopping journey end to end: a guest fills a session cart, the cart
 * survives into the logged-in session, checkout creates the order and moves
 * stock, and the confirmation page is readable only by its owner.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CartCheckoutFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private User buyer;
    private User someoneElse;
    private Product product;

    @BeforeEach
    void setUp() {
        buyer = createUser("buyer@example.com");
        someoneElse = createUser("intruder@example.com");
        product = productRepository.findAll().get(0);
    }

    private User createUser(String email) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("Passw0rd!"));
            user.setRole("USER");
            user.setEnabled(true);
            return userRepository.save(user);
        });
    }

    @Test
    void guestCanFillACartAndItPersistsAcrossRequests() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cart/add").session(session).with(csrf())
                        .param("productId", product.getId().toString())
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(product.getName())));
    }

    @Test
    void cartsInDifferentSessionsStayIsolated() throws Exception {
        MockHttpSession first = new MockHttpSession();
        MockHttpSession second = new MockHttpSession();

        mockMvc.perform(post("/cart/add").session(first).with(csrf())
                .param("productId", product.getId().toString()).param("quantity", "3"));

        mockMvc.perform(get("/cart").session(second))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your cart is empty")));
    }

    @Test
    void quantityCanBeUpdatedAndItemsRemoved() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cart/add").session(session).with(csrf())
                .param("productId", product.getId().toString()).param("quantity", "1"));
        mockMvc.perform(post("/cart/update").session(session).with(csrf())
                .param("productId", product.getId().toString()).param("quantity", "4"));

        mockMvc.perform(get("/cart").session(session))
                .andExpect(content().string(containsString("value=\"4\"")));

        mockMvc.perform(post("/cart/remove").session(session).with(csrf())
                .param("productId", product.getId().toString()));

        mockMvc.perform(get("/cart").session(session))
                .andExpect(content().string(containsString("Your cart is empty")));
    }

    @Test
    void checkoutCreatesAnOrderMovesStockAndShowsTheConfirmation() throws Exception {
        MockHttpSession session = new MockHttpSession();
        int stockBefore = product.getStock();

        mockMvc.perform(post("/cart/add").session(session).with(csrf())
                .param("productId", product.getId().toString()).param("quantity", "2"));

        mockMvc.perform(post("/checkout/place").session(session).with(user(buyer)).with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Stock moves via a bulk UPDATE, which bypasses the persistence context,
        // so re-read from the database rather than from the first-level cache.
        entityManager.flush();
        entityManager.clear();

        List<Order> orders = orderRepository.findByUser(buyer);
        assertThat(orders).hasSize(1);
        Order order = orders.get(0);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount())
                .isEqualByComparingTo(product.getPrice().multiply(java.math.BigDecimal.valueOf(2)));
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
                .isEqualTo(stockBefore - 2);

        // Renders with open-in-view disabled, i.e. the lines were fetched eagerly.
        mockMvc.perform(get("/orders/{id}/confirmation", order.getId()).with(user(buyer)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(product.getName())));
    }

    @Test
    void anotherUserCannotReadSomeoneElsesConfirmation() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/cart/add").session(session).with(csrf())
                .param("productId", product.getId().toString()).param("quantity", "1"));
        mockMvc.perform(post("/checkout/place").session(session).with(user(buyer)).with(csrf()));

        Long orderId = orderRepository.findByUser(buyer).get(0).getId();

        mockMvc.perform(get("/orders/{id}/confirmation", orderId).with(user(someoneElse)))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkoutWithAnEmptyCartShowsAnErrorAndCreatesNoOrder() throws Exception {
        mockMvc.perform(post("/checkout/place").with(user(buyer)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("empty")));

        assertThat(orderRepository.findByUser(buyer)).isEmpty();
    }

    @Test
    void orderingMoreThanTheAvailableStockIsRejected() throws Exception {
        MockHttpSession session = new MockHttpSession();
        int stockBefore = product.getStock();

        mockMvc.perform(post("/cart/add").session(session).with(csrf())
                .param("productId", product.getId().toString())
                .param("quantity", String.valueOf(stockBefore + 50)));

        mockMvc.perform(post("/checkout/place").session(session).with(user(buyer)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Not enough stock")));

        entityManager.flush();
        entityManager.clear();

        assertThat(orderRepository.findByUser(buyer)).isEmpty();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
                .isEqualTo(stockBefore);
    }
}
