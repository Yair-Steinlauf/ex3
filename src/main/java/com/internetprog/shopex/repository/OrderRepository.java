package com.internetprog.shopex.repository;

import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser(User user);

    List<Order> findAllByOrderByOrderDateDesc();

    long countByStatus(String status);

    /**
     * Loads an order together with its lines and their products in one query,
     * so the confirmation page can render them without an open session.
     */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findWithItemsById(Long id);
}
