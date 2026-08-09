package com.internetprog.shopex.web;

import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.Review;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.ProductRepository;
import com.internetprog.shopex.repository.ReviewRepository;
import com.internetprog.shopex.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for the review form.
 *
 * The form used to bind onto the Review entity, and Spring binds URI template
 * variables onto model attributes, so {id} from /products/{id}/reviews landed
 * in Review.id and save() merged instead of inserting: it either failed
 * outright or silently overwrote whichever review happened to have that id.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewSubmissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private User reviewer;
    private User otherAuthor;
    private Product targetProduct;
    private Product unrelatedProduct;

    @BeforeEach
    void setUp() {
        reviewer = userRepository.findByEmail("reviewer@example.com").orElseGet(() -> {
            User user = new User();
            user.setFirstName("Rachel");
            user.setLastName("Reviewer");
            user.setEmail("reviewer@example.com");
            user.setPassword(passwordEncoder.encode("Passw0rd!"));
            user.setRole("USER");
            user.setEnabled(true);
            return userRepository.save(user);
        });
        otherAuthor = userRepository.findByEmail("admin@shopex.local").orElseThrow();

        List<Product> products = productRepository.findAll();
        targetProduct = products.get(0);
        unrelatedProduct = products.get(1);
    }

    @Test
    void submittingAReviewInsertsANewRowAndLeavesAnExistingReviewUntouched() throws Exception {
        // A pre-existing review whose id collides with the product id being reviewed.
        entityManager.createNativeQuery(
                        "INSERT INTO reviews (id, product_id, user_id, rating, comment, created_at) VALUES (?,?,?,?,?,?)")
                .setParameter(1, targetProduct.getId())
                .setParameter(2, unrelatedProduct.getId())
                .setParameter(3, otherAuthor.getId())
                .setParameter(4, 5)
                .setParameter(5, "ORIGINAL REVIEW BY SOMEONE ELSE")
                .setParameter(6, LocalDateTime.now())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        long before = reviewRepository.count();

        mockMvc.perform(post("/products/{id}/reviews", targetProduct.getId())
                        .with(user(reviewer)).with(csrf())
                        .param("rating", "4")
                        .param("comment", "MY NEW REVIEW"))
                .andExpect(status().is3xxRedirection());

        entityManager.flush();
        entityManager.clear();

        assertThat(reviewRepository.count()).isEqualTo(before + 1);

        Review original = reviewRepository.findById(targetProduct.getId()).orElseThrow();
        assertThat(original.getComment()).isEqualTo("ORIGINAL REVIEW BY SOMEONE ELSE");
        assertThat(original.getRating()).isEqualTo(5);
        assertThat(original.getUser().getId()).isEqualTo(otherAuthor.getId());
        assertThat(original.getProduct().getId()).isEqualTo(unrelatedProduct.getId());

        assertThat(reviewRepository.findByProduct(targetProduct))
                .anyMatch(review -> "MY NEW REVIEW".equals(review.getComment())
                        && review.getUser().getId().equals(reviewer.getId()));
    }

    @Test
    void submittingAReviewOnAnEmptyReviewTableWorks() throws Exception {
        long before = reviewRepository.count();

        mockMvc.perform(post("/products/{id}/reviews", targetProduct.getId())
                        .with(user(reviewer)).with(csrf())
                        .param("rating", "5")
                        .param("comment", "First review"))
                .andExpect(status().is3xxRedirection());

        entityManager.flush();
        entityManager.clear();
        assertThat(reviewRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void outOfRangeRatingIsRejectedAndNothingIsSaved() throws Exception {
        long before = reviewRepository.count();

        mockMvc.perform(post("/products/{id}/reviews", targetProduct.getId())
                        .with(user(reviewer)).with(csrf())
                        .param("rating", "7")
                        .param("comment", "should not be stored"))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        assertThat(reviewRepository.count()).isEqualTo(before);
    }

    @Test
    void anonymousUsersCannotPostReviews() throws Exception {
        long before = reviewRepository.count();

        mockMvc.perform(post("/products/{id}/reviews", targetProduct.getId()).with(csrf())
                        .param("rating", "5")
                        .param("comment", "anonymous"))
                .andExpect(status().is3xxRedirection());

        entityManager.flush();
        entityManager.clear();
        assertThat(reviewRepository.count()).isEqualTo(before);
    }
}
