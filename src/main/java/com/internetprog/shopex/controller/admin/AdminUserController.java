package com.internetprog.shopex.controller.admin;

import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model, @AuthenticationPrincipal User currentUser) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("currentUserEmail", currentUser != null ? currentUser.getEmail() : null);
        return "admin/users";
    }

    @PostMapping("/{id}/toggle-enabled")
    public String toggleEnabled(@PathVariable Long id,
                                 @AuthenticationPrincipal User currentUser,
                                 RedirectAttributes redirectAttributes) {
        try {
            User updated = userService.toggleEnabled(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "User " + updated.getEmail() + " is now " + (updated.isEnabled() ? "enabled" : "disabled") + ".");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
