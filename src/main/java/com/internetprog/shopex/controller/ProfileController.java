package com.internetprog.shopex.controller;

import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.OrderRepository;
import com.internetprog.shopex.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ProfileController(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + principal.getName()));

        List<Order> orders = orderRepository.findByUser(user);

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        return "profile/profile";
    }
}
