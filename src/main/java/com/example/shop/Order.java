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

    @PostPersist
    public void postPersist() {
        // Spring Context에서 Service 빈 가져오기
        InventoryService inventoryService = ApplicationContextProvider.getBean(InventoryService.class);
        DeliveryService deliveryService = ApplicationContextProvider.getBean(DeliveryService.class);

        // 1. 재고 감소 (별도 트랜잭션으로 실행)
        Long productId = Long.parseLong(this.productId);
        inventoryService.decreaseStock(productId, this.qty);

        // 2. 배송 건 생성 (별도 트랜잭션으로 실행)
        deliveryService.createDelivery(this.id, this.customerId, this.qty, this.address);
    }
}
