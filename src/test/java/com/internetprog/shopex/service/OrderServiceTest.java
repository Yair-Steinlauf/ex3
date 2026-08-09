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
import org.springframework.web.server.ResponseStatusException;

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

    // --- reading orders back ---

    @Test
    void findForUser_returnsOnlyThatUsersOrders() {
        User other = new User();
        other.setFirstName("Other");
        other.setLastName("Buyer");
        other.setEmail("other-buyer@example.com");
        other.setPassword("hash");
        other.setRole("USER");
        entityManager.persist(other);

        orderService.placeOrder(customer, cartWith(headphones, 1));
        orderService.placeOrder(other, cartWith(headphones, 2));

        assertThat(orderService.findForUser(customer)).hasSize(1);
        assertThat(orderService.findForUser(other)).hasSize(1);
        assertThat(orderService.findAllNewestFirst()).hasSize(2);
    }

    @Test
    void getForOwner_returnsTheOrderWithItsLines() {
        Order placed = orderService.placeOrder(customer, cartWith(headphones, 2));
        entityManager.flush();
        entityManager.clear();

        Order loaded = orderService.getForOwner(placed.getId(), customer);

        assertThat(loaded.getId()).isEqualTo(placed.getId());
        assertThat(loaded.getItems()).hasSize(1);
        assertThat(loaded.getItems().get(0).getProduct().getName()).isEqualTo("Headphones");
    }

    @Test
    void getForOwner_refusesSomebodyElsesOrder() {
        User intruder = new User();
        intruder.setFirstName("Nosy");
        intruder.setLastName("Person");
        intruder.setEmail("intruder-service@example.com");
        intruder.setPassword("hash");
        intruder.setRole("USER");
        entityManager.persist(intruder);

        Order placed = orderService.placeOrder(customer, cartWith(headphones, 1));

        assertThatThrownBy(() -> orderService.getForOwner(placed.getId(), intruder))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void getForOwner_reportsAMissingOrderAsNotFound() {
        assertThatThrownBy(() -> orderService.getForOwner(999_999L, customer))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // --- admin status updates ---

    @Test
    void countPending_countsOnlyPendingOrders() {
        Order first = orderService.placeOrder(customer, cartWith(headphones, 1));
        orderService.placeOrder(customer, cartWith(headphones, 1));
        assertThat(orderService.countPending()).isEqualTo(2);

        orderService.updateStatus(first.getId(), "SHIPPED");

        assertThat(orderService.countPending()).isEqualTo(1);
    }

    @Test
    void updateStatus_rejectsAStatusOutsideTheAllowedList() {
        Order placed = orderService.placeOrder(customer, cartWith(headphones, 1));

        assertThatThrownBy(() -> orderService.updateStatus(placed.getId(), "HACKED"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status");

        assertThat(orderRepository.findById(placed.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void updateStatus_rejectsAnUnknownOrder() {
        assertThatThrownBy(() -> orderService.updateStatus(999_999L, "SHIPPED"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }
}
