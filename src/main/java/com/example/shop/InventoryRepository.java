package com.example.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "inventories", collectionResourceRel = "inventories")
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
