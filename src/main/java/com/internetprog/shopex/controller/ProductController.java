package com.internetprog.shopex.controller;

import com.internetprog.shopex.entity.Category;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.Review;
import com.internetprog.shopex.service.ProductService;
import com.internetprog.shopex.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
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
import java.util.Optional;

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
        Page<Product> productPage = productService.search(q, categoryId, sort, page);
        List<Category> categories = productService.getAllCategories();

        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categories);
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);

        return "products/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Product product = productService.getById(id);
        populateDetailModel(model, product);
        model.addAttribute("reviewForm", new Review());
        return "products/detail";
    }

    @PostMapping("/{id}/reviews")
    public String addReview(@PathVariable Long id,
                             @Valid @ModelAttribute("reviewForm") Review reviewForm,
                             BindingResult bindingResult,
                             Authentication authentication,
                             Model model) {
        Product product = productService.getById(id);

        if (!isAuthenticated(authentication)) {
            // Defensive server-side check: Spring Security should already reject an
            // unauthenticated POST to this endpoint, but we don't rely solely on the
            // review form being hidden client-side.
            populateDetailModel(model, product);
            model.addAttribute("reviewForm", reviewForm);
            model.addAttribute("reviewError", "You must be logged in to submit a review.");
            return "products/detail";
        }

        if (!bindingResult.hasErrors()) {
            Optional<Review> saved = reviewService.addReview(product, authentication.getName(), reviewForm);
            if (saved.isPresent()) {
                return "redirect:/products/" + id;
            }
            bindingResult.reject("user.notfound", "Could not find your user account.");
        }

        populateDetailModel(model, product);
        model.addAttribute("reviewForm", reviewForm);
        return "products/detail";
    }

    private void populateDetailModel(Model model, Product product) {
        List<Review> reviews = reviewService.findByProduct(product);
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", reviewService.averageRating(reviews));
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
