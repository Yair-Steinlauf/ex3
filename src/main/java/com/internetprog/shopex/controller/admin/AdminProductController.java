package com.internetprog.shopex.controller.admin;

import com.internetprog.shopex.entity.Category;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.repository.CategoryRepository;
import com.internetprog.shopex.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public AdminProductController(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Allows the "category" form field (submitted as a Category id) to be bound
     * directly onto the Product.category association without touching the entity.
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Category.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.isBlank()) {
                    setValue(null);
                } else {
                    setValue(categoryRepository.findById(Long.valueOf(text)).orElse(null));
                }
            }

            @Override
            public String getAsText() {
                Category category = (Category) getValue();
                return category != null && category.getId() != null ? category.getId().toString() : "";
            }
        });
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "admin/products";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return productRepository.findById(id)
                .map(product -> {
                    model.addAttribute("product", product);
                    model.addAttribute("categories", categoryRepository.findAll());
                    return "admin/product-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Product not found.");
                    return "redirect:/admin/products";
                });
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("product") Product product,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/product-form";
        }
        productRepository.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Product \"" + product.getName() + "\" created.");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("product") Product product,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/product-form";
        }
        return productRepository.findById(id)
                .map(existing -> {
                    // Copy editable fields onto the managed entity so id/createdAt are preserved.
                    existing.setName(product.getName());
                    existing.setDescription(product.getDescription());
                    existing.setPrice(product.getPrice());
                    existing.setStock(product.getStock());
                    existing.setImageUrl(product.getImageUrl());
                    existing.setCategory(product.getCategory());
                    productRepository.save(existing);
                    redirectAttributes.addFlashAttribute("successMessage", "Product \"" + existing.getName() + "\" updated.");
                    return "redirect:/admin/products";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Product not found.");
                    return "redirect:/admin/products";
                });
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!productRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Product not found.");
            return "redirect:/admin/products";
        }
        try {
            productRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted.");
        } catch (DataIntegrityViolationException ex) {
            // Product is referenced by existing OrderItems (or similar FK constraint) - block instead of crashing.
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete this product because it has existing orders referencing it.");
        }
        return "redirect:/admin/products";
    }
}
