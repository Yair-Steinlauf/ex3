package com.internetprog.shopex.controller.admin;

import com.internetprog.shopex.repository.OrderRepository;
import com.internetprog.shopex.repository.ProductRepository;
import com.internetprog.shopex.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AdminDashboardController(ProductRepository productRepository,
                                     OrderRepository orderRepository,
                                     UserRepository userRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        long totalProducts = productRepository.count();
        long pendingOrders = orderRepository.findAll().stream()
                .filter(order -> "PENDING".equals(order.getStatus()))
                .count();
        long totalUsers = userRepository.count();

        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("totalUsers", totalUsers);
        return "admin/dashboard";
    }
}
