package com.example.shop;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** GET /inventories - 전체 재고 조회 */
    @GetMapping
    public List<Inventory> getAllInventories() {
        return inventoryService.findAll();
    }

    /** GET /inventories/{id} - 특정 재고 조회 */
    @GetMapping("/{id}")
    public Inventory getInventory(@PathVariable Long id) {
        return inventoryService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found"));
    }

    /** POST /inventories - 재고 생성 */
    @PostMapping
    public ResponseEntity<Inventory> createInventory(@RequestBody Inventory inventory) {
        Inventory saved = inventoryService.save(inventory);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** PUT /inventories/{id} - 재고 업데이트 */
    @PutMapping("/{id}")
    public Inventory updateInventory(@PathVariable Long id, @RequestBody Inventory inventory) {
        if (!inventoryService.findById(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found");
        }
        inventory.setId(id);
        return inventoryService.save(inventory);
    }

    /** DELETE /inventories/{id} - 재고 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventory(@PathVariable Long id) {
        if (!inventoryService.findById(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found");
        }
        inventoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
