package com.internetprog.shopex.controller.admin;

import com.internetprog.shopex.dto.ProductForm;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        populateFormModel(model, null);
        return "admin/product-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return productService.findById(id)
                .map(product -> {
                    model.addAttribute("productForm", ProductForm.from(product));
                    populateFormModel(model, product.getId());
                    return "admin/product-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Product not found.");
                    return "redirect:/admin/products";
                });
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("productForm") ProductForm productForm,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, null);
            return "admin/product-form";
        }
        Product created = productService.create(productForm);
        redirectAttributes.addFlashAttribute("successMessage", "Product \"" + created.getName() + "\" created.");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("productForm") ProductForm productForm,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, id);
            return "admin/product-form";
        }
        try {
            Product updated = productService.update(id, productForm);
            redirectAttributes.addFlashAttribute("successMessage", "Product \"" + updated.getName() + "\" updated.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/products";
    }

    /**
     * Shared model attributes for the create/edit form. The title is computed
     * here rather than in the template: Thymeleaf forbids conditional
     * expressions inside a fragment parameter, which is what the layout's
     * head(title=...) call is.
     */
    private void populateFormModel(Model model, Long productId) {
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("productId", productId);
        model.addAttribute("formTitle", productId == null ? "New Product" : "Edit Product");
    }
}
