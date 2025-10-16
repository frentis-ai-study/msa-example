package com.example.shop;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    /**
     * 배송 생성 - 별도 트랜잭션으로 실행
     * Order의 @PostPersist에서 호출되므로 REQUIRES_NEW 필수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDelivery(Long orderId, String customerId, Integer qty, String address) {
        Delivery delivery = new Delivery();
        delivery.setOrderId(orderId);
        delivery.setCustomerId(customerId);
        delivery.setQty(qty);
        delivery.setAddress(address);
        delivery.setStatus("PENDING");

        deliveryRepository.save(delivery);
    }
}
