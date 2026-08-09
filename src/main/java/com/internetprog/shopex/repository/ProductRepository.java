package com.internetprog.shopex.repository;

import com.internetprog.shopex.entity.Category;
import com.internetprog.shopex.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByCategory(Category category, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndCategory(String name, Category category, Pageable pageable);

    List<Product> findTop4ByOrderByCreatedAtDesc();

    /**
     * Atomically takes {@code quantity} units of stock, but only if enough is
     * available — the check and the decrement happen in a single SQL UPDATE,
     * so two concurrent checkouts cannot both take the last unit.
     *
     * @return the number of rows updated: 1 on success, 0 if stock was short
     */
    @Modifying
    @Query("update Product p set p.stock = p.stock - :quantity where p.id = :id and p.stock >= :quantity")
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);
}
