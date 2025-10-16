package com.example.shop;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    /** 전체 배송 조회 */
    @Transactional(readOnly = true)
    public List<Delivery> findAll() {
        return deliveryRepository.findAll();
    }

    /** ID로 배송 조회 */
    @Transactional(readOnly = true)
    public Optional<Delivery> findById(Long id) {
        return deliveryRepository.findById(id);
    }

    /** 배송 저장(신규/수정) */
    @Transactional
    public Delivery save(Delivery delivery) {
        return deliveryRepository.save(delivery);
    }

    /** 배송 삭제 */
    @Transactional
    public void deleteById(Long id) {
        deliveryRepository.deleteById(id);
    }
}
