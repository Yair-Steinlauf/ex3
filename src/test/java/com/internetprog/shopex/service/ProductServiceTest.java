package com.internetprog.shopex.service;

import com.internetprog.shopex.dto.ProductForm;
import com.internetprog.shopex.entity.Category;
import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.OrderItem;
import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(ProductService.class)
class ProductServiceTest {

    @Autowired
    private ProductService productService;

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
        entityManager.flush();
    }

    private ProductForm form(String name, String price, int stock) {
        ProductForm form = new ProductForm();
        form.setName(name);
        form.setDescription("Description of " + name);
        form.setPrice(new BigDecimal(price));
        form.setStock(stock);
        form.setCategoryId(electronics.getId());
        return form;
    }

    @Test
    void create_persistsTheFormAndResolvesTheCategory() {
        Product created = productService.create(form("Speaker", "59.90", 8));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Speaker");
        assertThat(created.getPrice()).isEqualByComparingTo("59.90");
        assertThat(created.getStock()).isEqualTo(8);
        assertThat(created.getCategory().getName()).isEqualTo("Electronics");
    }

    @Test
    void create_acceptsNoCategory() {
        ProductForm form = form("Uncategorised", "10.00", 1);
        form.setCategoryId(null);

        assertThat(productService.create(form).getCategory()).isNull();
    }

    @Test
    void update_changesFieldsButKeepsTheIdentity() {
        Product created = productService.create(form("Speaker", "59.90", 8));
        Long id = created.getId();

        Product updated = productService.update(id, form("Speaker Mk II", "69.90", 3));

        assertThat(updated.getId()).isEqualTo(id);
        assertThat(updated.getName()).isEqualTo("Speaker Mk II");
        assertThat(updated.getPrice()).isEqualByComparingTo("69.90");
        assertThat(updated.getStock()).isEqualTo(3);
        assertThat(updated.getCreatedAt()).isEqualTo(created.getCreatedAt());
    }

    @Test
    void update_rejectsAnUnknownProduct() {
        assertThatThrownBy(() -> productService.update(999_999L, form("Ghost", "1.00", 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void delete_removesAnUnreferencedProduct() {
        Product created = productService.create(form("Disposable", "5.00", 1));

        productService.delete(created.getId());

        assertThat(productRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void delete_rejectsAnUnknownProduct() {
        assertThatThrownBy(() -> productService.delete(999_999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void delete_refusesWhenAnOrderStillReferencesTheProduct() {
        // Persist everything through the same EntityManager so the order line
        // references a managed product.
        Product created = new Product();
        created.setName("Ordered");
        created.setPrice(new BigDecimal("20.00"));
        created.setStock(5);
        created.setCategory(electronics);
        entityManager.persist(created);

        User buyer = new User();
        buyer.setFirstName("Buyer");
        buyer.setLastName("One");
        buyer.setEmail("buyer-fk@example.com");
        buyer.setPassword("hash");
        buyer.setRole("USER");
        entityManager.persist(buyer);

        Order order = new Order();
        order.setUser(buyer);
        order.setStatus("PENDING");
        order.setTotalAmount(new BigDecimal("20.00"));
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(created);
        item.setQuantity(1);
        item.setPriceAtPurchase(new BigDecimal("20.00"));
        order.getItems().add(item);
        entityManager.persist(order);
        entityManager.flush();

        assertThatThrownBy(() -> productService.delete(created.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("existing orders");
    }

    @Test
    void getById_throws404ForAnUnknownProduct() {
        assertThatThrownBy(() -> productService.getById(999_999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void search_filtersByNameAndCategory() {
        productService.create(form("Bluetooth Speaker", "59.90", 8));
        productService.create(form("Bluetooth Headphones", "99.90", 4));
        ProductForm book = form("Paperback Novel", "15.00", 20);
        book.setCategoryId(null);
        productService.create(book);

        Page<Product> byName = productService.search("bluetooth", null, "name", 0);
        assertThat(byName.getTotalElements()).isEqualTo(2);

        Page<Product> byCategory = productService.search(null, electronics.getId(), "name", 0);
        assertThat(byCategory.getTotalElements()).isEqualTo(2);

        Page<Product> both = productService.search("headphones", electronics.getId(), "price", 0);
        assertThat(both.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_ignoresAnUnknownCategoryRatherThanFailing() {
        productService.create(form("Speaker", "59.90", 8));

        assertThat(productService.search(null, 999_999L, "name", 0).getTotalElements()).isEqualTo(1);
    }
}
