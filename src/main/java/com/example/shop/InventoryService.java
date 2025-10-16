package com.example.shop;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * 재고 감소 - 별도 트랜잭션으로 실행
     * Order의 @PostPersist에서 호출되므로 REQUIRES_NEW 필수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseStock(Long productId, Integer qty) {
        // 1. 재고 조회
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        // 2. 재고 충분 여부 확인
        if (inventory.getStock() < qty) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + inventory.getStock());
        }

        // 3. 재고 감소
        inventory.setStock(inventory.getStock() - qty);
        inventoryRepository.save(inventory);
    }
}
