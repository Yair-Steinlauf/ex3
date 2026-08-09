package com.internetprog.shopex.controller;

import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.OrderRepository;
import com.internetprog.shopex.service.CartService;
import com.internetprog.shopex.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

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
    private final OrderRepository orderRepository;

    public CheckoutController(CartService cartService,
                               OrderService orderService,
                               OrderRepository orderRepository) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/checkout")
    public String checkout(Model model) {
        model.addAttribute("cartItems", cartService.getItems());
        model.addAttribute("cartTotal", cartService.getTotal());
        return "checkout/checkout";
    }

    @PostMapping("/checkout/place")
    public String placeOrder(@AuthenticationPrincipal User currentUser, Model model) {
        try {
            Order order = orderService.placeOrder(currentUser, cartService);
            return "redirect:/orders/" + order.getId() + "/confirmation";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("cartItems", cartService.getItems());
            model.addAttribute("cartTotal", cartService.getTotal());
            return "checkout/checkout";
        }
    }

    @GetMapping("/orders/{id}/confirmation")
    public String confirmation(@PathVariable Long id,
                                @AuthenticationPrincipal User currentUser,
                                Model model) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getUser() == null || !order.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may not view this order.");
        }

        model.addAttribute("order", order);
        return "checkout/confirmation";
    }
}
