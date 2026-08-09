package com.internetprog.shopex.controller;

import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.service.CartService;
import com.internetprog.shopex.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Checkout flow. /checkout/** and /orders/** are guarded by SecurityConfig, so
 * anonymous requests are redirected to /login and bounced back here afterwards
 * via Spring Security's saved-request mechanism — no hand-rolled redirect logic,
 * and no re-checking of authentication in this controller.
 */
@Controller
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;

    public CheckoutController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @GetMapping("/checkout")
    public String checkout(Model model) {
        addCartToModel(model);
        return "checkout/checkout";
    }

    @PostMapping("/checkout/place")
    public String placeOrder(@AuthenticationPrincipal User currentUser, Model model) {
        try {
            Order order = orderService.placeOrder(currentUser, cartService);
            return "redirect:/orders/" + order.getId() + "/confirmation";
        } catch (IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            addCartToModel(model);
            return "checkout/checkout";
        }
    }

    @GetMapping("/orders/{id}/confirmation")
    public String confirmation(@PathVariable Long id,
                                @AuthenticationPrincipal User currentUser,
                                Model model) {
        model.addAttribute("order", orderService.getForOwner(id, currentUser));
        return "checkout/confirmation";
    }

    private void addCartToModel(Model model) {
        model.addAttribute("cartItems", cartService.getItems());
        model.addAttribute("cartTotal", cartService.getTotal());
    }
}
