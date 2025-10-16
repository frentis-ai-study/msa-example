package com.example.shop;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "shop-order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String productId;
    private Integer qty;
    private String customerId;
    private String address;
    private String status;

    @PrePersist
    public void prePersist() {
        // Spring Context에서 서비스 빈 가져오기
        InventoryService inventoryService = ApplicationContextProvider.getBean(InventoryService.class);

        // 1. 재고 조회 및 검증
        Long productId = Long.parseLong(this.productId);
        Inventory inventory = inventoryService.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        // 2. 재고 충분 여부 확인
        if (inventory.getStock() < this.qty) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + inventory.getStock());
        }

        // 3. 재고 감소
        inventory.setStock(inventory.getStock() - this.qty);
        inventoryService.save(inventory);
    }

    @PostPersist
    public void postPersist() {
        // Spring Context에서 서비스 빈 가져오기
        DeliveryService deliveryService = ApplicationContextProvider.getBean(DeliveryService.class);

        // 4. 배송 건 생성
        Delivery delivery = new Delivery();
        delivery.setOrderId(this.id);
        delivery.setCustomerId(this.customerId);
        delivery.setQty(this.qty);
        delivery.setAddress(this.address);
        delivery.setStatus("PENDING");
        deliveryService.save(delivery);
    }
}
