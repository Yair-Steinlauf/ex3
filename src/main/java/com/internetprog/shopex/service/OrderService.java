package com.internetprog.shopex.service;

import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.OrderItem;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.OrderRepository;
import com.internetprog.shopex.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orders: placing them (the one transactional money-and-stock operation) and
 * reading them back for the profile page and the admin backend.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    /** The statuses an admin may set; the single source of truth for the UI too. */
    public static final List<String> STATUSES = List.of("PENDING", "CONFIRMED", "SHIPPED", "CANCELLED");

    private static final String PENDING = "PENDING";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Places an order for the given user from the contents of the given cart.
     * Stock for each line is taken with a conditional single-statement UPDATE
     * ({@link ProductRepository#decrementStock}), so a concurrent checkout for
     * the same product cannot oversell; if any line is short, the exception
     * rolls back the whole transaction — including decrements already made for
     * earlier lines. On success the cart is cleared and the persisted order
     * (with its items) is returned.
     */
    @Transactional
    public Order placeOrder(User user, CartService cart) {
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Your cart is empty.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(PENDING);

        BigDecimal total = BigDecimal.ZERO;
        for (CartService.CartItem line : cart.getItems()) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Product no longer available: " + line.getName()));

            if (productRepository.decrementStock(product.getId(), line.getQuantity()) == 0) {
                throw new IllegalStateException(
                        "Not enough stock for \"" + product.getName() + "\".");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(line.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            order.getItems().add(orderItem);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        }

        // Derived from the order's own lines, so the total always matches them
        // even if a price changed while the product sat in the cart.
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        cart.clear();
        return saved;
    }

    public List<Order> findForUser(User user) {
        return orderRepository.findByUser(user);
    }

    public List<Order> findAllNewestFirst() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    public long countPending() {
        return orderRepository.countByStatus(PENDING);
    }

    /**
     * Loads an order with its lines for the user it belongs to.
     *
     * @throws ResponseStatusException 404 if it does not exist, 403 if it
     *                                 belongs to somebody else
     */
    public Order getForOwner(Long id, User owner) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getUser() == null || !order.getUser().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may not view this order.");
        }
        return order;
    }

    /**
     * @throws IllegalStateException if the status is not one of {@link #STATUSES}
     *                               or the order does not exist
     */
    @Transactional
    public Order updateStatus(Long id, String status) {
        if (!STATUSES.contains(status)) {
            throw new IllegalStateException("Invalid status: " + status);
        }
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Order not found."));
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
