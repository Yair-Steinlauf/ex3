package com.internetprog.shopex.repository;

import com.internetprog.shopex.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Used before deleting a product: an order line referencing it must keep
     * the product around so past orders stay readable.
     */
    boolean existsByProductId(Long productId);
}
