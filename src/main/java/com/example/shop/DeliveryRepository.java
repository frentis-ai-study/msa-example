package com.example.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "deliveries", collectionResourceRel = "deliveries")
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
}
