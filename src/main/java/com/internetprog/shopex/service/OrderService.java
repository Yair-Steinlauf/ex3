package com.internetprog.shopex.service;

import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.OrderItem;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.OrderRepository;
import com.internetprog.shopex.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the transactional "place order" workflow: validate stock for every
 * cart line, then create the order + items and decrement stock atomically.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Places an order for the given user from the contents of the given cart.
     * Validates stock for every line first; if any line is short, throws and
     * nothing is persisted (the whole transaction rolls back). On success the
     * cart is cleared and the persisted order (with its items) is returned.
     */
    @Transactional
    public Order placeOrder(User user, CartService cart) {
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Your cart is empty.");
        }

        // Validate stock for every line up front so a shortage on any single
        // line aborts the whole order instead of partially creating it.
        for (CartService.CartItem line : cart.getItems()) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Product no longer available: " + line.getName()));
            if (product.getStock() < line.getQuantity()) {
                throw new IllegalStateException(
                        "Not enough stock for \"" + product.getName() + "\" (only "
                                + product.getStock() + " left).");
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus("PENDING");
        order.setTotalAmount(cart.getTotal());

        for (CartService.CartItem line : cart.getItems()) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Product no longer available: " + line.getName()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(line.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            order.getItems().add(orderItem);

            product.setStock(product.getStock() - line.getQuantity());
            productRepository.save(product);
        }

        Order saved = orderRepository.save(order);
        cart.clear();
        return saved;
    }
}
