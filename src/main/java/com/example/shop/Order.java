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
}
