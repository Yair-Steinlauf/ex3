package com.internetprog.shopex.service;

import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.Review;
import com.internetprog.shopex.entity.User;
import com.internetprog.shopex.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for reading and submitting product reviews.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
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

    /**
     * Persist a new review for the given product, authored by the given user.
     * The Review is built here from plain values rather than bound from the
     * request, so its id always starts out null and save() is guaranteed to
     * INSERT rather than overwrite an existing review.
     */
    public Review addReview(Product product, User author, int rating, String comment) {
        Review review = new Review();
        review.setProduct(product);
        review.setUser(author);
        review.setRating(rating);
        review.setComment(comment);
        return reviewRepository.save(review);
    }
}
