package com.example.shop;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** GET /orders - 전체 주문 조회 */
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.findAll();
    }

    /** GET /orders/{id} - 특정 주문 조회 */
    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    /** POST /orders - 주문 생성 (재고 감소 및 배송 건 생성) */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order saved = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** PUT /orders/{id} - 주문 업데이트 */
    @PutMapping("/{id}")
    public Order updateOrder(@PathVariable Long id, @RequestBody Order order) {
        if (!orderService.findById(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        order.setId(id);
        return orderService.updateOrder(order);
    }

    /** DELETE /orders/{id} - 주문 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        if (!orderService.findById(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        orderService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}