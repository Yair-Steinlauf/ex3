package com.internetprog.shopex.repository;

import com.internetprog.shopex.entity.Product;
import com.internetprog.shopex.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct(Product product);
}
