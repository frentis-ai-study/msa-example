package com.example.shop;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /** 전체 재고 조회 */
    @Transactional(readOnly = true)
    public List<Inventory> findAll() {
        return inventoryRepository.findAll();
    }

    /** ID로 재고 조회 */
    @Transactional(readOnly = true)
    public Optional<Inventory> findById(Long id) {
        return inventoryRepository.findById(id);
    }

    /** 재고 저장(신규/수정) */
    @Transactional
    public Inventory save(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    /** 재고 삭제 */
    @Transactional
    public void deleteById(Long id) {
        inventoryRepository.deleteById(id);
    }
}
