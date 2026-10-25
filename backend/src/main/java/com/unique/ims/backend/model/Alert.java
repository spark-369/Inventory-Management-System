package com.unique.ims.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AlertType type;

    private String message;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private String batchNumber;

    private LocalDate expiryDate;

    @Builder.Default
    private boolean resolved = false;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    public enum AlertType {
        LOW_STOCK, EXPIRY_WARNING, EXPIRY_ALERT
    }
}