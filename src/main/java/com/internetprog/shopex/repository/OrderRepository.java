package com.internetprog.shopex.repository;

import com.internetprog.shopex.entity.Order;
import com.internetprog.shopex.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser(User user);

    List<Order> findAllByOrderByOrderDateDesc();

    long countByStatus(String status);
}
