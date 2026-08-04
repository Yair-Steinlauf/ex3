package com.internetprog.shopex.service;

import com.internetprog.shopex.entity.Product;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Session-scoped shopping cart. One instance per HTTP session (Spring proxies
 * this bean so it can be safely injected into singleton controllers/services).
 * Purely in-memory — no JPA entity, no static state.
 */
@Component
@SessionScope
public class CartService implements Serializable {

    private final List<CartItem> items = new ArrayList<>();

    public void addItem(Product product, int quantity) {
        if (product == null || quantity <= 0) {
            return;
        }
        Optional<CartItem> existing = findItem(product.getId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            items.add(new CartItem(product.getId(), product.getName(), product.getPrice(), quantity));
        }
    }

    public void removeItem(Long productId) {
        items.removeIf(item -> item.getProductId().equals(productId));
    }

    public void updateQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            removeItem(productId);
            return;
        }
        findItem(productId).ifPresent(item -> item.setQuantity(quantity));
    }

    public List<CartItem> getItems() {
        return items;
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public void clear() {
        items.clear();
    }

    private Optional<CartItem> findItem(Long productId) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    /**
     * Plain in-session snapshot of a cart line. Deliberately NOT a JPA entity —
     * price/name are copied at add-time so the cart is stable even if the
     * underlying product changes while it sits in the cart.
     */
    public static class CartItem implements Serializable {
        private final Long productId;
        private final String name;
        private final BigDecimal unitPrice;
        private int quantity;

        public CartItem(Long productId, String name, BigDecimal unitPrice, int quantity) {
            this.productId = productId;
            this.name = name;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getSubtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
