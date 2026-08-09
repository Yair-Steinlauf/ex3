package com.internetprog.shopex.service;

import com.internetprog.shopex.entity.Category;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.repository.CategoryRepository;
import com.internetprog.shopex.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service for browsing and searching the product catalog.
 */
@Service
public class ProductService {

    public static final int DEFAULT_PAGE_SIZE = 12;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Search products by optional name substring and optional category, sorted and paginated.
     *
     * @param query      optional case-insensitive substring to match against the product name
     * @param categoryId optional category id to filter by
     * @param sort       "price" or "name" (defaults to "name")
     * @param page       zero-based page index
     */
    public Page<Product> search(String query, Long categoryId, String sort, int page) {
        Sort sortOrder = "price".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "price")
                : Sort.by(Sort.Direction.ASC, "name");
        Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_PAGE_SIZE, sortOrder);

        boolean hasQuery = StringUtils.hasText(query);
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElse(null);
        }

        if (hasQuery && category != null) {
            return productRepository.findByNameContainingIgnoreCaseAndCategory(query, category, pageable);
        } else if (hasQuery) {
            return productRepository.findByNameContainingIgnoreCase(query, pageable);
        } else if (category != null) {
            return productRepository.findByCategory(category, pageable);
        } else {
            return productRepository.findAll(pageable);
        }
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Most recently added products, for the home page's featured section.
     */
    public List<Product> getFeatured() {
        return productRepository.findTop4ByOrderByCreatedAtDesc();
    }
}
