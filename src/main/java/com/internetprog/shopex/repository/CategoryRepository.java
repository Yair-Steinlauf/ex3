package com.internetprog.shopex.repository;

import com.internetprog.shopex.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
