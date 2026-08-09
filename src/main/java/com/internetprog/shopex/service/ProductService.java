package com.internetprog.shopex.service;

import com.internetprog.shopex.dto.ProductForm;
import com.internetprog.shopex.entity.Category;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.repository.CategoryRepository;
import com.internetprog.shopex.repository.OrderItemRepository;
import com.internetprog.shopex.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * The product catalog: browsing for shoppers and CRUD for the admin backend.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    public static final int DEFAULT_PAGE_SIZE = 12;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductService(ProductRepository productRepository,
                           CategoryRepository categoryRepository,
                           OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository = orderItemRepository;
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
        Category category = categoryId == null
                ? null
                : categoryRepository.findById(categoryId).orElse(null);

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

    /**
     * @throws ResponseStatusException 404 if there is no such product
     */
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public long count() {
        return productRepository.count();
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

    @Transactional
    public Product create(ProductForm form) {
        Product product = new Product();
        apply(form, product);
        return productRepository.save(product);
    }

    /**
     * Copies the submitted fields onto the existing product so its id and
     * creation timestamp are preserved.
     *
     * @throws IllegalStateException if there is no such product
     */
    @Transactional
    public Product update(Long id, ProductForm form) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Product not found."));
        apply(form, product);
        return productRepository.save(product);
    }

    /**
     * Deletes a product, refusing when past orders still reference it — checked
     * explicitly rather than by catching whatever the ORM happens to throw,
     * which differs depending on what the persistence context already holds.
     *
     * @throws IllegalStateException if the product does not exist, or is still
     *                               referenced by existing orders
     */
    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalStateException("Product not found.");
        }
        if (orderItemRepository.existsByProductId(id)) {
            throw new IllegalStateException(
                    "Cannot delete this product because it has existing orders referencing it.");
        }
        try {
            productRepository.deleteById(id);
            productRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // Safety net if an order arrives between the check and the delete.
            throw new IllegalStateException(
                    "Cannot delete this product because it has existing orders referencing it.");
        }
    }

    private void apply(ProductForm form, Product product) {
        product.setName(form.getName());
        product.setDescription(form.getDescription());
        product.setPrice(form.getPrice());
        product.setStock(form.getStock());
        product.setImageUrl(form.getImageUrl());
        product.setCategory(form.getCategoryId() == null
                ? null
                : categoryRepository.findById(form.getCategoryId()).orElse(null));
    }
}
