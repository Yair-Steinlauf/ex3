package com.internetprog.shopex.config;

import com.internetprog.shopex.service.CartService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the session cart's item count to every view so the header badge is
 * accurate on all pages, not only the ones whose controller thought of it.
 */
@ControllerAdvice
public class CartModelAdvice {

    private final CartService cartService;

    public CartModelAdvice(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("cartCount")
    public int cartCount() {
        return cartService.getItemCount();
    }
}
