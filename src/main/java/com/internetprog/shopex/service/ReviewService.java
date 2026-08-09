package com.internetprog.shopex.service;

import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.Review;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.ReviewRepository;
import com.internetprog.shopex.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for reading and submitting product reviews.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    public List<Review> findByProduct(Product product) {
        return reviewRepository.findByProduct(product);
    }

    /**
     * Average rating for the given reviews, or 0 if there are none.
     */
    public double averageRating(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Persist a new review for the given product, authored by the user with the given email.
     * The Review entity is built here from plain values rather than bound directly from the
     * request, so its id always starts out null and save() is guaranteed to INSERT.
     *
     * @return the saved review, or empty if no user exists for that email
     */
    public Optional<Review> addReview(Product product, String userEmail, int rating, String comment) {
        return userRepository.findByEmail(userEmail).map(user -> {
            Review review = new Review();
            review.setProduct(product);
            review.setUser(user);
            review.setRating(rating);
            review.setComment(comment);
            return reviewRepository.save(review);
        });
    }
}
