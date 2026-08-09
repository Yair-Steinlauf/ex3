package com.internetprog.shopex.service;

import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.OrderItem;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.OrderRepository;
import com.internetprog.shopex.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Owns the transactional "place order" workflow: reserve stock for every cart
 * line and create the order + items atomically.
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
     * Stock for each line is taken with a conditional single-statement UPDATE
     * ({@link ProductRepository#decrementStock}), so a concurrent checkout for
     * the same product cannot oversell; if any line is short, the exception
     * rolls back the whole transaction (including decrements already made for
     * earlier lines). On success the cart is cleared and the persisted order
     * (with its items) is returned.
     */
    @Transactional
    public Order placeOrder(User user, CartService cart) {
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Your cart is empty.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus("PENDING");

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

        // Total is derived from the order's own lines (price at purchase), so
        // it always equals the sum of the items even if a price changed while
        // the product sat in the cart.
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        cart.clear();
        return saved;
    }
}
