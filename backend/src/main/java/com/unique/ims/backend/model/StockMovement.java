package com.unique.ims.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    private MovementType movementType;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;

    private Long referenceId;

    private String notes;

    private LocalDateTime createdAt;

    public enum MovementType {
        IN, OUT, TRANSFER_IN, TRANSFER_OUT, ADJUSTMENT, RETURN, SALE, PURCHASE
    }

    public enum ReferenceType {
        PURCHASE, SALE, TRANSFER, ADJUSTMENT, RETURN, SYSTEM
    }
}