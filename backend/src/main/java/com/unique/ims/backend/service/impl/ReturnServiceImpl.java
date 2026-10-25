package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.config.CacheNames;
import com.unique.ims.backend.dto.ReturnDto;
import com.unique.ims.backend.messaging.DomainMessagePublisher;
import com.unique.ims.backend.messaging.request.InventorySyncJobRequest;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.model.Return;
import com.unique.ims.backend.model.Warehouse;
import com.unique.ims.backend.repository.ProductRepository;
import com.unique.ims.backend.repository.ReturnRepository;
import com.unique.ims.backend.repository.WarehouseRepository;
import com.unique.ims.backend.service.ReturnService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRepository returnRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final DomainMessagePublisher messagePublisher;

    public ReturnServiceImpl(ReturnRepository returnRepository,
                             ProductRepository productRepository,
                             WarehouseRepository warehouseRepository,
                             DomainMessagePublisher messagePublisher) {
        this.returnRepository = returnRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.messagePublisher = messagePublisher;
    }

    @Override
    @Transactional
    public ReturnDto createReturn(ReturnDto dto) {
        Return record = new Return();
        record.setReturnDate(LocalDateTime.now());
        record.setStatus(Return.ReturnStatus.PENDING);
        record.setReason(dto.getReason());
        record.setReturnNumber("RTN-" + System.currentTimeMillis());
        record.setType(parseReturnType(dto.getType()));
        record.setQuantity(dto.getQuantity());
        record.setBatchNumber(dto.getBatchNumber());
        record.setUnitPrice(dto.getUnitPrice());
        record.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : computeTotal(dto));
        record.setProduct(resolveProduct(dto.getProductId()));
        record.setWarehouse(resolveWarehouse(dto.getWarehouseId()));
        return mapToDto(returnRepository.save(record));
    }

    @Override
    @Transactional
    public ReturnDto updateReturn(Long id, ReturnDto dto) {
        Return record = returnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return not found: " + id));

        if (record.getStatus() != Return.ReturnStatus.PENDING) {
            throw new IllegalArgumentException("Only pending returns can be edited.");
        }

        if (dto.getType() != null && !dto.getType().isBlank()) {
            record.setType(parseReturnType(dto.getType()));
        }
        if (dto.getProductId() != null) {
            record.setProduct(resolveProduct(dto.getProductId()));
        }
        if (dto.getWarehouseId() != null) {
            record.setWarehouse(resolveWarehouse(dto.getWarehouseId()));
        }
        record.setQuantity(dto.getQuantity());
        record.setBatchNumber(dto.getBatchNumber());
        record.setUnitPrice(dto.getUnitPrice());
        record.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : computeTotal(dto));
        record.setReason(dto.getReason());
        return mapToDto(returnRepository.save(record));
    }

    @Override
    public ReturnDto getReturnById(Long id) {
        return mapToDto(returnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return not found: " + id)));
    }

    @Override
    public ReturnDto getReturnByNumber(String returnNumber) {
        return mapToDto(returnRepository.findByReturnNumber(returnNumber)
                .orElseThrow(() -> new IllegalArgumentException("Return not found: " + returnNumber)));
    }

    @Override
    public List<ReturnDto> getAllReturns() {
        return returnRepository.findAll().stream()
                .sorted(Comparator.comparing(Return::getReturnDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteReturn(Long id) {
        if (!returnRepository.existsById(id)) {
            throw new IllegalArgumentException("Return not found: " + id);
        }
        returnRepository.deleteById(id);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public ReturnDto approveReturn(Long id) {
        Return record = returnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return not found: " + id));

        if (record.getStatus() == Return.ReturnStatus.APPROVED) {
            throw new IllegalArgumentException("Return " + record.getReturnNumber() + " is already approved.");
        }

        record.setStatus(Return.ReturnStatus.APPROVED);
        record = returnRepository.save(record);

        Product product = record.getProduct();
        int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int quantity = record.getQuantity() != null ? record.getQuantity() : 0;

        if (record.getType() == Return.ReturnType.PURCHASE_RETURN) {
            product.setCurrentStock(Math.max(0, currentStock - quantity));
        } else {
            product.setCurrentStock(currentStock + quantity);
        }
        productRepository.save(product);

        // Approving a return moves stock, so ask a worker to re-sync the snapshot
        // and re-evaluate the reorder level after the transaction commits.
        messagePublisher.publishAfterCommit(InventorySyncJobRequest.forProduct(
                product.getId(),
                "Return " + record.getReturnNumber() + " approved",
                "returns-api"));

        return mapToDto(record);
    }

    private Return.ReturnType parseReturnType(String type) {
        try {
            return Return.ReturnType.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Unsupported return type: " + type
                    + ". Expected PURCHASE_RETURN or SALE_RETURN.");
        }
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

    private java.math.BigDecimal computeTotal(ReturnDto dto) {
        if (dto.getUnitPrice() == null || dto.getQuantity() == null) {
            return null;
        }
        return dto.getUnitPrice().multiply(java.math.BigDecimal.valueOf(dto.getQuantity()));
    }

    private ReturnDto mapToDto(Return record) {
        ReturnDto dto = new ReturnDto();
        dto.setId(record.getId());
        dto.setReturnNumber(record.getReturnNumber());
        dto.setType(record.getType() != null ? record.getType().name() : null);
        dto.setProductId(record.getProduct().getId());
        dto.setProductName(record.getProduct().getName());
        dto.setWarehouseId(record.getWarehouse().getId());
        dto.setWarehouseName(record.getWarehouse().getName());
        dto.setQuantity(record.getQuantity());
        dto.setBatchNumber(record.getBatchNumber());
        dto.setUnitPrice(record.getUnitPrice());
        dto.setTotalAmount(record.getTotalAmount());
        dto.setStatus(record.getStatus() != null ? record.getStatus().name() : null);
        dto.setReason(record.getReason());
        dto.setReturnDate(record.getReturnDate());
        return dto;
    }
}
