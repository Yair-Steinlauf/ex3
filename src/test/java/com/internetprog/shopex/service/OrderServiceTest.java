package com.internetprog.shopex.service;

import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.OrderRepository;
import com.internetprog.shopex.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checkout is the one place where money and stock change, so it gets its own
 * tests: totals, stock decrements, and the "not enough stock" abort.
 */
@DataJpaTest
@Import(OrderService.class)
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User customer;
    private Product headphones;
    private Product lastOne;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setFirstName("Test");
        customer.setLastName("Customer");
        customer.setEmail("customer@example.com");
        customer.setPassword("irrelevant-hash");
        customer.setRole("USER");
        entityManager.persist(customer);

        headphones = new Product();
        headphones.setName("Headphones");
        headphones.setPrice(new BigDecimal("100.00"));
        headphones.setStock(10);
        entityManager.persist(headphones);

        lastOne = new Product();
        lastOne.setName("Last One In Stock");
        lastOne.setPrice(new BigDecimal("25.00"));
        lastOne.setStock(1);
        entityManager.persist(lastOne);

        entityManager.flush();
    }

    private CartService cartWith(Product product, int quantity) {
        CartService cart = new CartService();
        cart.addItem(product, quantity);
        return cart;
    }

    @Test
    void placeOrder_createsOrderWithLinesAndDecrementsStock() {
        CartService cart = cartWith(headphones, 3);

        Order order = orderService.placeOrder(customer, cart);
        entityManager.flush();
        entityManager.clear();

        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo("PENDING");
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(order.getItems().get(0).getPriceAtPurchase()).isEqualByComparingTo("100.00");
        assertThat(productRepository.findById(headphones.getId()).orElseThrow().getStock()).isEqualTo(7);
    }

    @Test
    void placeOrder_totalEqualsTheSumOfItsLines() {
        CartService cart = new CartService();
        cart.addItem(headphones, 2);
        cart.addItem(lastOne, 1);

        Order order = orderService.placeOrder(customer, cart);

        assertThat(order.getTotalAmount()).isEqualByComparingTo("225.00");
        BigDecimal sumOfLines = order.getItems().stream()
                .map(item -> item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(sumOfLines);
    }

    @Test
    void placeOrder_clearsTheCart() {
        CartService cart = cartWith(headphones, 1);

        orderService.placeOrder(customer, cart);

        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getItemCount()).isZero();
    }

    @Test
    void placeOrder_rejectsWhenStockIsShortAndLeavesStockUntouched() {
        CartService cart = cartWith(lastOne, 2);

        assertThatThrownBy(() -> orderService.placeOrder(customer, cart))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough stock");

        entityManager.clear();
        assertThat(productRepository.findById(lastOne.getId()).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    void placeOrder_rejectsAnEmptyCart() {
        assertThatThrownBy(() -> orderService.placeOrder(customer, new CartService()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");

        assertThat(orderRepository.count()).isZero();
    }

    /**
     * A shortage on the second line must undo the first line's decrement too.
     */
    @Test
    void placeOrder_isAllOrNothingAcrossLines() {
        CartService cart = new CartService();
        cart.addItem(headphones, 1);
        cart.addItem(lastOne, 5);

        assertThatThrownBy(() -> orderService.placeOrder(customer, cart))
                .isInstanceOf(IllegalStateException.class);

        assertThat(orderRepository.count()).isZero();
    }
}
