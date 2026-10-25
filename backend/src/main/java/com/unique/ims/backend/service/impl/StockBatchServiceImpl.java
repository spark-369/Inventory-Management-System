package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.dto.StockBatchDto;
import com.unique.ims.backend.messaging.DomainMessagePublisher;
import com.unique.ims.backend.messaging.request.ExpiryScanJobRequest;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.model.StockBatch;
import com.unique.ims.backend.model.Warehouse;
import com.unique.ims.backend.repository.ProductRepository;
import com.unique.ims.backend.repository.StockBatchRepository;
import com.unique.ims.backend.repository.WarehouseRepository;
import com.unique.ims.backend.service.StockBatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockBatchServiceImpl implements StockBatchService {

    private final StockBatchRepository stockBatchRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final DomainMessagePublisher messagePublisher;

    public StockBatchServiceImpl(StockBatchRepository stockBatchRepository,
                                 ProductRepository productRepository,
                                 WarehouseRepository warehouseRepository,
                                 DomainMessagePublisher messagePublisher) {
        this.stockBatchRepository = stockBatchRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.messagePublisher = messagePublisher;
    }

    @Override
    @Transactional
    public StockBatchDto createStockBatch(StockBatchDto dto) {
        StockBatch batch = new StockBatch();
        batch.setProduct(resolveProduct(dto.getProductId()));
        batch.setWarehouse(resolveWarehouse(dto.getWarehouseId()));
        batch.setBatchNumber(dto.getBatchNumber());
        batch.setExpiryDate(dto.getExpiryDate());
        batch.setQuantity(dto.getQuantity());
        batch.setCostPrice(dto.getCostPrice());
        batch.setActive(dto.isActive());

        StockBatchDto saved = mapToDto(stockBatchRepository.save(batch));

        // A new batch can arrive already near its expiry date. The sweep is
        // *delayed* slightly so importing several batches produces one scan
        // instead of one per row.
        publishExpiryScan("Stock batch " + batch.getBatchNumber() + " created");

        return saved;
    }

    @Override
    @Transactional
    public StockBatchDto updateStockBatch(Long id, StockBatchDto dto) {
        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock batch not found: " + id));

        if (dto.getProductId() != null) {
            batch.setProduct(resolveProduct(dto.getProductId()));
        }
        if (dto.getWarehouseId() != null) {
            batch.setWarehouse(resolveWarehouse(dto.getWarehouseId()));
        }
        if (dto.getBatchNumber() != null && !dto.getBatchNumber().isBlank()) {
            batch.setBatchNumber(dto.getBatchNumber());
        }
        batch.setExpiryDate(dto.getExpiryDate());
        batch.setQuantity(dto.getQuantity());
        batch.setCostPrice(dto.getCostPrice());
        batch.setActive(dto.isActive());

        StockBatchDto saved = mapToDto(stockBatchRepository.save(batch));
        publishExpiryScan("Stock batch " + batch.getBatchNumber() + " updated");
        return saved;
    }

    /**
     * Enqueues an {@code EXPIRY_SCAN} message once the surrounding transaction
     * commits, with a short delay so a burst of batch writes coalesces into a
     * single sweep rather than one scan per row.
     */
    private void publishExpiryScan(String reason) {
        messagePublisher.scheduleAfterCommit(
                ExpiryScanJobRequest.defaultWindow(reason, "inventory-api"),
                Duration.ofSeconds(5));
    }

    @Override
    public StockBatchDto getStockBatchById(Long id) {
        return mapToDto(stockBatchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock batch not found: " + id)));
    }

    @Override
    public List<StockBatchDto> getStockBatchesByProduct(Long productId) {
        Product product = resolveProduct(productId);
        return stockBatchRepository.findByProduct(product).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockBatchDto> getAllStockBatches() {
        return stockBatchRepository.findAll().stream()
                .sorted(Comparator.comparing(StockBatch::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteStockBatch(Long id) {
        if (!stockBatchRepository.existsById(id)) {
            throw new IllegalArgumentException("Stock batch not found: " + id);
        }
        stockBatchRepository.deleteById(id);
    }

    private Product resolveProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product is required.");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    private Warehouse resolveWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse is required.");
        }
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + warehouseId));
    }

    private StockBatchDto mapToDto(StockBatch batch) {
        StockBatchDto dto = new StockBatchDto();
        dto.setId(batch.getId());
        dto.setProductId(batch.getProduct().getId());
        dto.setProductName(batch.getProduct().getName());
        dto.setWarehouseId(batch.getWarehouse().getId());
        dto.setWarehouseName(batch.getWarehouse().getName());
        dto.setBatchNumber(batch.getBatchNumber());
        dto.setExpiryDate(batch.getExpiryDate());
        dto.setQuantity(batch.getQuantity());
        dto.setCostPrice(batch.getCostPrice());
        dto.setActive(batch.isActive());
        return dto;
    }
}
