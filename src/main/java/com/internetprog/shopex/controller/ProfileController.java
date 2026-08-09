package com.internetprog.shopex.controller;

import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    private final OrderService orderService;

    public ProfileController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("orders", orderService.findForUser(currentUser));
        return "profile/profile";
    }
}
