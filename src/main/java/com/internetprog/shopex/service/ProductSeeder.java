package com.internetprog.shopex.service;

import com.internetprog.shopex.entity.Category;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.repository.CategoryRepository;
import com.internetprog.shopex.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds the catalog with sample categories and products on startup so the demo
 * doesn't start with an empty product list. Idempotent: only runs when the
 * products table is empty, so it's safe to leave enabled across restarts.
 *
 * <p>Sample images are served from {@code static/images/products} rather than a
 * placeholder CDN, so the catalog renders identically with no internet
 * connection and costs no third-party round trip.
 */
@Component
public class ProductSeeder {

    private static final Logger log = LoggerFactory.getLogger(ProductSeeder.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductSeeder(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (productRepository.count() > 0) {
            return;
        }
        log.info("Seeding product catalog with sample data...");

        Category electronics = getOrCreateCategory("Electronics");
        Category homeKitchen = getOrCreateCategory("Home & Kitchen");
        Category books = getOrCreateCategory("Books");

        createProduct("Wireless Noise-Cancelling Headphones",
                "Over-ear Bluetooth headphones with active noise cancellation and 30-hour battery life.",
                "199.99", 45, "/images/products/headphones.webp", electronics);
        createProduct("4K Ultra HD Smart TV - 55 inch",
                "55-inch 4K smart television with HDR support and built-in streaming apps.",
                "549.00", 20, "/images/products/tv.webp", electronics);
        createProduct("Mechanical Keyboard RGB",
                "Compact mechanical keyboard with hot-swappable switches and per-key RGB lighting.",
                "89.90", 60, "/images/products/keyboard.webp", electronics);
        createProduct("Wireless Ergonomic Mouse",
                "Ergonomic wireless mouse with adjustable DPI and silent clicks.",
                "34.50", 100, "/images/products/mouse.webp", electronics);
        createProduct("Stainless Steel Cookware Set",
                "10-piece stainless steel cookware set, dishwasher safe and induction compatible.",
                "129.99", 25, "/images/products/cookware.webp", homeKitchen);
        createProduct("Programmable Coffee Maker",
                "12-cup programmable drip coffee maker with reusable filter and auto shut-off.",
                "54.99", 40, "/images/products/coffee.webp", homeKitchen);
        createProduct("Robot Vacuum Cleaner",
                "Smart robot vacuum with mapping navigation and app control.",
                "249.00", 15, "/images/products/vacuum.webp", homeKitchen);
        createProduct("Non-Stick Frying Pan Set",
                "3-piece non-stick frying pan set in multiple sizes, PFOA-free coating.",
                "39.99", 80, "/images/products/pan.webp", homeKitchen);
        createProduct("Clean Code: A Handbook of Agile Software Craftsmanship",
                "Classic software engineering book on writing maintainable, readable code.",
                "42.00", 50, "/images/products/cleancode.webp", books);
        createProduct("Designing Data-Intensive Applications",
                "In-depth guide to the architecture of modern data systems.",
                "48.50", 35, "/images/products/ddia.webp", books);

        log.info("Product catalog seeding complete.");
    }

    private Category getOrCreateCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName(name);
                    return categoryRepository.save(category);
                });
    }

    private void createProduct(String name, String description, String price, int stock, String imageUrl, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setImageUrl(imageUrl);
        product.setCategory(category);
        productRepository.save(product);
    }
}
