package com.internetprog.shopex.controller.admin;

import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.UserRepository;
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

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model, @AuthenticationPrincipal User currentUser) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentUserEmail", currentUser != null ? currentUser.getEmail() : null);
        return "admin/users";
    }

    @PostMapping("/{id}/toggle-enabled")
    public String toggleEnabled(@PathVariable Long id,
                                 @AuthenticationPrincipal User currentUser,
                                 RedirectAttributes redirectAttributes) {
        return userRepository.findById(id)
                .map(user -> {
                    boolean isSelf = currentUser != null
                            && currentUser.getEmail() != null
                            && currentUser.getEmail().equalsIgnoreCase(user.getEmail());
                    if (isSelf) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                "You cannot disable your own account.");
                        return "redirect:/admin/users";
                    }
                    user.setEnabled(!user.isEnabled());
                    userRepository.save(user);
                    redirectAttributes.addFlashAttribute("successMessage",
                            "User " + user.getEmail() + " is now " + (user.isEnabled() ? "enabled" : "disabled") + ".");
                    return "redirect:/admin/users";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
                    return "redirect:/admin/users";
                });
    }
}
