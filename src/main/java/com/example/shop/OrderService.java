package com.example.shop;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final DeliveryService deliveryService;

    public OrderService(OrderRepository orderRepository,
                       InventoryService inventoryService,
                       DeliveryService deliveryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.deliveryService = deliveryService;
    }

    /** 전체 주문 조회 */
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    /** ID로 주문 조회 */
    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    /** 주문 생성 - 재고 감소 및 배송 건 생성 */
    @Transactional
    public Order createOrder(Order order) {
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

        // 4. 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 5. 배송 건 생성
        Delivery delivery = new Delivery();
        delivery.setOrderId(savedOrder.getId());
        delivery.setCustomerId(savedOrder.getCustomerId());
        delivery.setQty(savedOrder.getQty());
        delivery.setAddress(savedOrder.getAddress());
        delivery.setStatus("PENDING");
        deliveryService.save(delivery);

        return savedOrder;
    }

    /** 주문 수정 */
    @Transactional
    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }

    /** 주문 삭제 */
    @Transactional
    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }
}