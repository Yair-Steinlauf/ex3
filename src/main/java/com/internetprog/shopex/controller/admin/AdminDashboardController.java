package com.internetprog.shopex.controller.admin;

import com.internetprog.shopex.service.OrderService;
import com.internetprog.shopex.service.ProductService;
import com.internetprog.shopex.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;

    public AdminDashboardController(ProductService productService,
                                     OrderService orderService,
                                     UserService userService) {
        this.productService = productService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalProducts", productService.count());
        model.addAttribute("pendingOrders", orderService.countPending());
        model.addAttribute("totalUsers", userService.count());
        return "admin/dashboard";
    }
}
