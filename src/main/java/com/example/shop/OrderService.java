package com.example.shop;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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

    /** 주문 저장(신규/수정) */
    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    /** 주문 삭제 */
    @Transactional
    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }
}