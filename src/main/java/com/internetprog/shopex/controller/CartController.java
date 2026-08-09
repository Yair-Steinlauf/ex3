package com.internetprog.shopex.controller;

import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.service.CartService;
import com.internetprog.shopex.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Session cart: viewing and editing require no login (guests can shop).
 */
@Controller
public class CartController {

    private final CartService cartService;
    private final ProductService productService;

    public CartController(CartService cartService, ProductService productService) {
        this.cartService = cartService;
        this.productService = productService;
    }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        model.addAttribute("cartItems", cartService.getItems());
        model.addAttribute("cartTotal", cartService.getTotal());
        return "cart/view";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                             @RequestParam(defaultValue = "1") int quantity,
                             RedirectAttributes redirectAttributes) {
        Optional<Product> product = productService.findById(productId);
        if (product.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Product not found.");
            return "redirect:/cart";
        }

        cartService.addItem(product.get(), Math.max(quantity, 1));
        redirectAttributes.addFlashAttribute("successMessage",
                "Added " + product.get().getName() + " to your cart.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId) {
        cartService.removeItem(productId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(@RequestParam Long productId,
                                  @RequestParam int quantity) {
        cartService.updateQuantity(productId, quantity);
        return "redirect:/cart";
    }
}
