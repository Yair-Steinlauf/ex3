package com.internetprog.shopex.controller;

import com.internetprog.shopex.dto.RegistrationForm;
import com.internetprog.shopex.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Login page and self-service registration. The login POST itself is handled by
 * Spring Security's filter chain, not by a method here.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {

        if (form.getPassword() != null && !form.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }
        if (userService.emailIsTaken(form.getEmail())) {
            bindingResult.rejectValue("email", "email.duplicate", "An account with this email already exists");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        userService.register(form);
        redirectAttributes.addFlashAttribute("successMessage", "Registration successful. Please log in.");
        return "redirect:/login";
    }
}
