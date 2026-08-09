package com.internetprog.shopex.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Form backing object for writing a product review. Deliberately not the Review
 * entity: {id} from /products/{id}/reviews would bind onto Review.id and turn
 * the save into an update of an unrelated review.
 */
public class ReviewForm {

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int rating = 5;

    @Size(max = 1000, message = "Comment must be at most 1000 characters")
    private String comment;

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
