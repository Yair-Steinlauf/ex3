package com.internetprog.shopex.controller;

import com.internetprog.shopex.dto.ReviewForm;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.Review;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.service.ProductService;
import com.internetprog.shopex.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Product catalog: search/browse, product detail, and review submission.
 */
@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    public ProductController(ProductService productService, ReviewService reviewService) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q,
                        @RequestParam(name = "categoryId", required = false) Long categoryId,
                        @RequestParam(name = "sort", required = false, defaultValue = "name") String sort,
                        @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                        Model model) {
        model.addAttribute("productPage", productService.search(q, categoryId, sort, page));
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        return "products/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        populateDetailModel(model, productService.getById(id));
        model.addAttribute("reviewForm", new ReviewForm());
        return "products/detail";
    }

    /**
     * Only reachable by an authenticated user — SecurityConfig is the single
     * place that decides that, so this method does not re-check it.
     */
    @PostMapping("/{id}/reviews")
    public String addReview(@PathVariable Long id,
                             @Valid @ModelAttribute("reviewForm") ReviewForm reviewForm,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal User currentUser,
                             Model model) {
        Product product = productService.getById(id);

        if (bindingResult.hasErrors()) {
            populateDetailModel(model, product);
            model.addAttribute("reviewForm", reviewForm);
            return "products/detail";
        }

        reviewService.addReview(product, currentUser, reviewForm.getRating(), reviewForm.getComment());
        return "redirect:/products/" + id;
    }

    private void populateDetailModel(Model model, Product product) {
        List<Review> reviews = reviewService.findByProduct(product);
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", reviewService.averageRating(reviews));
    }
}
