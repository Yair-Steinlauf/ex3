package com.internetprog.shopex.controller;

import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.OrderRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ProfileController {

    private final OrderRepository orderRepository;

    public ProfileController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal User currentUser, Model model) {
        List<Order> orders = orderRepository.findByUser(currentUser);

        model.addAttribute("user", currentUser);
        model.addAttribute("orders", orders);
        return "profile/profile";
    }
}
