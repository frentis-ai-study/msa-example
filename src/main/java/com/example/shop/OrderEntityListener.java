package com.example.shop;

import jakarta.persistence.PrePersist;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Configurable
public class OrderEntityListener {

    private static InventoryService inventoryService;
    private static DeliveryService deliveryService;

    @Autowired
    public void setInventoryService(InventoryService inventoryService) {
        OrderEntityListener.inventoryService = inventoryService;
    }

    @Autowired
    public void setDeliveryService(DeliveryService deliveryService) {
        OrderEntityListener.deliveryService = deliveryService;
    }

    @PrePersist
    public void prePersist(Order order) {
        // 1. 재고 조회 및 검증
        Long productId = Long.parseLong(order.getProductId());
        Inventory inventory = inventoryService.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        // 2. 재고 충분 여부 확인
        if (inventory.getStock() < order.getQty()) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + inventory.getStock());
        }

        // 3. 재고 감소
        inventory.setStock(inventory.getStock() - order.getQty());
        inventoryService.save(inventory);

        // 4. 배송 건은 PostPersist에서 생성 (order.id가 필요)
    }

    @jakarta.persistence.PostPersist
    public void postPersist(Order order) {
        // 5. 배송 건 생성
        Delivery delivery = new Delivery();
        delivery.setOrderId(order.getId());
        delivery.setCustomerId(order.getCustomerId());
        delivery.setQty(order.getQty());
        delivery.setAddress(order.getAddress());
        delivery.setStatus("PENDING");
        deliveryService.save(delivery);
    }
}
