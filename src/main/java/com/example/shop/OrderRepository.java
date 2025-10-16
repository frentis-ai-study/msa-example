package com.example.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "orders", collectionResourceRel = "orders")
public interface OrderRepository extends JpaRepository<Order, Long> {
}
