package com.internetprog.shopex.repository;

import com.internetprog.shopex.entity.Category;
import com.internetprog.shopex.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data-layer slice test: only JPA + the repositories are loaded, against an
 * in-memory database.
 */
@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Category electronics;

    @BeforeEach
    void setUp() {
        electronics = new Category();
        electronics.setName("Electronics");
        entityManager.persist(electronics);

        Category books = new Category();
        books.setName("Books");
        entityManager.persist(books);

        entityManager.persist(product("Wireless Headphones", "199.99", 5, electronics));
        entityManager.persist(product("Wireless Mouse", "34.50", 10, electronics));
        entityManager.persist(product("Clean Code", "42.00", 3, books));
        entityManager.flush();
    }

    private Product product(String name, String price, int stock, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setCategory(category);
        return product;
    }

    @Test
    void findByNameContainingIgnoreCase_matchesCaseInsensitiveSubstring() {
        Page<Product> results = productRepository
                .findByNameContainingIgnoreCase("wireless", PageRequest.of(0, 10));

        assertThat(results.getTotalElements()).isEqualTo(2);
        assertThat(results.getContent())
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Wireless Headphones", "Wireless Mouse");
    }

    @Test
    void findByCategory_returnsOnlyThatCategory() {
        Page<Product> results = productRepository.findByCategory(electronics, PageRequest.of(0, 10));

        assertThat(results.getTotalElements()).isEqualTo(2);
        assertThat(results.getContent()).allMatch(p -> p.getCategory().getName().equals("Electronics"));
    }

    @Test
    void paginationSplitsResultsAcrossPages() {
        Page<Product> firstPage = productRepository.findAll(PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
    }

    @Test
    void decrementStock_succeedsWhenEnoughStock() {
        Product headphones = productRepository.findByNameContainingIgnoreCase("Wireless Headphones",
                PageRequest.of(0, 1)).getContent().get(0);

        int updated = productRepository.decrementStock(headphones.getId(), 2);
        entityManager.clear();

        assertThat(updated).isEqualTo(1);
        assertThat(productRepository.findById(headphones.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    /**
     * The guard that makes concurrent checkouts safe: the UPDATE only applies
     * when enough stock is left, so a short request changes nothing.
     */
    @Test
    void decrementStock_refusesAndChangesNothingWhenStockIsShort() {
        Product cleanCode = productRepository.findByNameContainingIgnoreCase("Clean Code",
                PageRequest.of(0, 1)).getContent().get(0);

        int updated = productRepository.decrementStock(cleanCode.getId(), 4);
        entityManager.clear();

        assertThat(updated).isZero();
        assertThat(productRepository.findById(cleanCode.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void findTop4ByOrderByCreatedAtDesc_returnsAtMostFour() {
        assertThat(productRepository.findTop4ByOrderByCreatedAtDesc()).hasSizeLessThanOrEqualTo(4);
    }
}
