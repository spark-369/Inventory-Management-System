package com.unique.ims.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Return {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String returnNumber;

    @Enumerated(EnumType.STRING)
    private ReturnType type;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    private Integer quantity;

    private String batchNumber;

    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private ReturnStatus status;

    private String reason;

    private LocalDateTime returnDate;

    private LocalDateTime createdAt;

    public enum ReturnType {
        PURCHASE_RETURN, SALE_RETURN
    }

    public enum ReturnStatus {
        PENDING, APPROVED, COMPLETED, REJECTED
    }
}