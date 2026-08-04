package com.internetprog.shopex.controller.admin;

import com.internetprog.shopex.repository.OrderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private static final List<String> STATUSES = List.of("PENDING", "CONFIRMED", "SHIPPED", "CANCELLED");

    private final OrderRepository orderRepository;

    public AdminOrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", orderRepository.findAllByOrderByOrderDateDesc());
        model.addAttribute("statuses", STATUSES);
        return "admin/orders";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                                @RequestParam String status,
                                RedirectAttributes redirectAttributes) {
        if (!STATUSES.contains(status)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid status: " + status);
            return "redirect:/admin/orders";
        }
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(status);
                    orderRepository.save(order);
                    redirectAttributes.addFlashAttribute("successMessage", "Order #" + order.getId() + " status updated to " + status + ".");
                    return "redirect:/admin/orders";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Order not found.");
                    return "redirect:/admin/orders";
                });
    }
}
