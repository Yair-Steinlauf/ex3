package com.internetprog.shopex.repository;

import com.internetprog.shopex.entity.OrderItem;
import com.internetprog.shopex.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByProduct(Product product);
}
