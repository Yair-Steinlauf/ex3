package com.internetprog.shopex.repository;

import com.internetprog.shopex.entity.Category;
import com.internetprog.shopex.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Product> findByCategory(Category category);

    Page<Product> findByCategory(Category category, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndCategory(String name, Category category, Pageable pageable);

    List<Product> findTop4ByOrderByCreatedAtDesc();
}
