package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.config.CacheNames;
import com.unique.ims.backend.dto.StockMovementDto;
import com.unique.ims.backend.messaging.DomainMessagePublisher;
import com.unique.ims.backend.messaging.request.AlertScanJobRequest;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.model.StockMovement;
import com.unique.ims.backend.model.Warehouse;
import com.unique.ims.backend.repository.ProductRepository;
import com.unique.ims.backend.repository.StockMovementRepository;
import com.unique.ims.backend.repository.WarehouseRepository;
import com.unique.ims.backend.service.StockMovementService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final DomainMessagePublisher messagePublisher;

    public StockMovementServiceImpl(StockMovementRepository stockMovementRepository,
                                    ProductRepository productRepository,
                                    WarehouseRepository warehouseRepository,
                                    DomainMessagePublisher messagePublisher) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.messagePublisher = messagePublisher;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public StockMovementDto createStockMovement(StockMovementDto dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + dto.getProductId()));
        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + dto.getWarehouseId()));

        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setMovementType(parseMovementType(dto.getMovementType()));
        movement.setQuantity(dto.getQuantity());
        movement.setReferenceType(parseReferenceType(dto.getReferenceType()));
        movement.setReferenceId(dto.getReferenceId());
        movement.setNotes(dto.getNotes());
        movement.setCreatedAt(LocalDateTime.now());

        StockMovement saved = stockMovementRepository.save(movement);

        applyStockEffect(product, saved.getMovementType(), saved.getQuantity());

        // Any stock change can push a product below its reorder level, so ask a
        // worker to re-evaluate it once the movement has committed.
        messagePublisher.publishAfterCommit(AlertScanJobRequest.forProduct(
                product.getId(),
                "Stock movement " + saved.getMovementType() + " of " + saved.getQuantity(),
                "inventory-api"));

        return mapToDto(saved);
    }

    private void applyStockEffect(Product product, StockMovement.MovementType type, Integer quantity) {
        int current = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int qty = quantity != null ? quantity : 0;
        int updated = switch (type) {
            case IN, PURCHASE, RETURN, TRANSFER_IN -> current + qty;
            case OUT, SALE, TRANSFER_OUT -> Math.max(0, current - qty);
            case ADJUSTMENT -> qty;
        };
        product.setCurrentStock(updated);
        productRepository.save(product);
    }

    @Override
    public List<StockMovementDto> getStockMovementsByProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return stockMovementRepository.findByProduct(product).stream()
                .sorted(Comparator.comparing(StockMovement::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementDto> getStockMovementsByWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + warehouseId));
        return stockMovementRepository.findByWarehouse(warehouse).stream()
                .sorted(Comparator.comparing(StockMovement::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementDto> getAllStockMovements() {
        return stockMovementRepository.findAll().stream()
                .sorted(Comparator.comparing(StockMovement::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private StockMovement.MovementType parseMovementType(String type) {
        try {
            return StockMovement.MovementType.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Unsupported movement type: " + type
                    + ". Expected one of IN, OUT, TRANSFER_IN, TRANSFER_OUT, ADJUSTMENT, RETURN, SALE, PURCHASE.");
        }
    }

    private StockMovement.ReferenceType parseReferenceType(String type) {
        if (type == null || type.isBlank()) {
            return StockMovement.ReferenceType.SYSTEM;
        }
        try {
            return StockMovement.ReferenceType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported reference type: " + type
                    + ". Expected one of PURCHASE, SALE, TRANSFER, ADJUSTMENT, RETURN, SYSTEM.");
        }
    }

    private StockMovementDto mapToDto(StockMovement movement) {
        StockMovementDto dto = new StockMovementDto();
        dto.setId(movement.getId());
        dto.setProductId(movement.getProduct().getId());
        dto.setProductName(movement.getProduct().getName());
        dto.setWarehouseId(movement.getWarehouse().getId());
        dto.setWarehouseName(movement.getWarehouse().getName());
        dto.setMovementType(movement.getMovementType() != null ? movement.getMovementType().name() : null);
        dto.setQuantity(movement.getQuantity());
        dto.setReferenceType(movement.getReferenceType() != null ? movement.getReferenceType().name() : null);
        dto.setReferenceId(movement.getReferenceId());
        dto.setNotes(movement.getNotes());
        dto.setCreatedAt(movement.getCreatedAt());
        return dto;
    }
}
