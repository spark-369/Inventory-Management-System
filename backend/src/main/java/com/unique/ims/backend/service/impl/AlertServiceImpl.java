package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.dto.AlertDto;
import com.unique.ims.backend.model.Alert;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.model.StockBatch;
import com.unique.ims.backend.repository.AlertRepository;
import com.unique.ims.backend.repository.ProductRepository;
import com.unique.ims.backend.repository.StockBatchRepository;
import com.unique.ims.backend.service.AlertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;

    public AlertServiceImpl(AlertRepository alertRepository, ProductRepository productRepository, StockBatchRepository stockBatchRepository) {
        this.alertRepository = alertRepository;
        this.productRepository = productRepository;
        this.stockBatchRepository = stockBatchRepository;
    }

    @Override
    public List<AlertDto> getAllAlerts() {
        return alertRepository.findAll().stream()
                .sorted(Comparator.comparing(Alert::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertDto> getUnresolvedAlerts() {
        return alertRepository.findByResolvedFalse().stream()
                .sorted(Comparator.comparing(Alert::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertDto> getAlertsByProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return alertRepository.findByProduct(product).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public AlertDto resolveAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        return mapToDto(alertRepository.save(alert));
    }

    @Override
    public AlertDto createAlert(AlertDto alertDto) {
        Alert alert = new Alert();
        alert.setType(parseAlertType(alertDto.getType()));
        alert.setMessage(alertDto.getMessage());

        if (alertDto.getProductId() != null) {
            Product product = productRepository.findById(alertDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + alertDto.getProductId()));
            alert.setProduct(product);
        }

        alert.setBatchNumber(alertDto.getBatchNumber());
        alert.setExpiryDate(alertDto.getExpiryDate());
        alert.setResolved(false);
        alert.setCreatedAt(LocalDateTime.now());

        return mapToDto(alertRepository.save(alert));
    }

    @Override
    @Transactional
    public int scanLowStock() {
        int created = 0;
        for (Product product : productRepository.findAll()) {
            created += lowStockAlertFor(product);
        }
        return created;
    }

    @Override
    @Transactional
    public int scanLowStockForProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product id is required for a low-stock scan.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return lowStockAlertFor(product);
    }

    @Override
    @Transactional
    public int scanExpiry(int warningWindowDays) {
        int window = warningWindowDays > 0 ? warningWindowDays : DEFAULT_EXPIRY_WARNING_WINDOW_DAYS;
        int created = 0;
        for (StockBatch batch : stockBatchRepository.findAll()) {
            if (batch.getExpiryDate() == null) {
                continue;
            }
            LocalDate expiryDate = batch.getExpiryDate();
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

            Alert.AlertType alertType;
            String message;

            if (daysUntilExpiry < 0) {
                alertType = Alert.AlertType.EXPIRY_ALERT;
                message = "Expired: " + batch.getProduct().getName() + " batch " + batch.getBatchNumber()
                        + " expired on " + expiryDate;
            } else if (daysUntilExpiry <= window) {
                alertType = Alert.AlertType.EXPIRY_WARNING;
                message = "Expiry warning: " + batch.getProduct().getName() + " batch " + batch.getBatchNumber()
                        + " expires on " + expiryDate + " (" + daysUntilExpiry + " days)";
            } else {
                continue;
            }

            boolean hasUnresolvedExpiryAlert = alertRepository.findByProductAndBatchNumberAndTypeAndResolvedFalse(
                    batch.getProduct(), batch.getBatchNumber(), alertType).isEmpty();

            if (hasUnresolvedExpiryAlert) {
                Alert alert = new Alert();
                alert.setType(alertType);
                alert.setMessage(message);
                alert.setProduct(batch.getProduct());
                alert.setBatchNumber(batch.getBatchNumber());
                alert.setExpiryDate(expiryDate);
                alert.setResolved(false);
                alert.setCreatedAt(LocalDateTime.now());
                alertRepository.save(alert);
                created++;
            }
        }
        return created;
    }

    /**
     * Raises a single low-stock alert for the product when it sits at or below
     * its reorder level and no unresolved alert exists yet.
     *
     * @return 1 when an alert was created, otherwise 0
     */
    private int lowStockAlertFor(Product product) {
        Integer currentStock = product.getCurrentStock();
        Integer reorderLevel = product.getReorderLevel();

        if (currentStock == null || reorderLevel == null || currentStock > reorderLevel) {
            return 0;
        }

        boolean hasUnresolvedLowStockAlert = alertRepository.findByProductAndTypeAndResolvedFalse(
                product, Alert.AlertType.LOW_STOCK).isEmpty();

        if (!hasUnresolvedLowStockAlert) {
            return 0;
        }

        Alert alert = new Alert();
        alert.setType(Alert.AlertType.LOW_STOCK);
        alert.setMessage("Low stock: " + product.getName() + " has " + currentStock
                + " units (reorder level: " + reorderLevel + ")");
        alert.setProduct(product);
        alert.setResolved(false);
        alert.setCreatedAt(LocalDateTime.now());
        alertRepository.save(alert);
        return 1;
    }

    private Alert.AlertType parseAlertType(String type) {
        try {
            return Alert.AlertType.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Unsupported alert type: " + type
                    + ". Expected one of LOW_STOCK, EXPIRY_WARNING, EXPIRY_ALERT.");
        }
    }

    private AlertDto mapToDto(Alert alert) {
        AlertDto dto = new AlertDto();
        dto.setId(alert.getId());
        dto.setType(alert.getType() != null ? alert.getType().name() : null);
        dto.setMessage(alert.getMessage());
        dto.setProductId(alert.getProduct() != null ? alert.getProduct().getId() : null);
        dto.setProductName(alert.getProduct() != null ? alert.getProduct().getName() : null);
        dto.setBatchNumber(alert.getBatchNumber());
        dto.setExpiryDate(alert.getExpiryDate());
        dto.setResolved(alert.isResolved());
        dto.setCreatedAt(alert.getCreatedAt());
        dto.setResolvedAt(alert.getResolvedAt());
        return dto;
    }
}
