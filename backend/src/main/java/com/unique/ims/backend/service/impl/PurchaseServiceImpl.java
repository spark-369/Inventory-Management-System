package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.config.CacheNames;
import com.unique.ims.backend.dto.PurchaseDto;
import com.unique.ims.backend.dto.PurchaseItemDto;
import com.unique.ims.backend.messaging.DomainMessagePublisher;
import com.unique.ims.backend.messaging.request.InventorySyncJobRequest;
import com.unique.ims.backend.model.Alert;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.model.Purchase;
import com.unique.ims.backend.model.PurchaseItem;
import com.unique.ims.backend.model.Supplier;
import com.unique.ims.backend.model.Warehouse;
import com.unique.ims.backend.repository.AlertRepository;
import com.unique.ims.backend.repository.ProductRepository;
import com.unique.ims.backend.repository.PurchaseItemRepository;
import com.unique.ims.backend.repository.PurchaseRepository;
import com.unique.ims.backend.repository.SupplierRepository;
import com.unique.ims.backend.repository.WarehouseRepository;
import com.unique.ims.backend.service.PurchaseService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final AlertRepository alertRepository;
    private final DomainMessagePublisher messagePublisher;

    public PurchaseServiceImpl(PurchaseRepository purchaseRepository,
                               PurchaseItemRepository purchaseItemRepository,
                               ProductRepository productRepository,
                               WarehouseRepository warehouseRepository,
                               SupplierRepository supplierRepository,
                               AlertRepository alertRepository,
                               DomainMessagePublisher messagePublisher) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.supplierRepository = supplierRepository;
        this.alertRepository = alertRepository;
        this.messagePublisher = messagePublisher;
    }

    @Override
    @Transactional
    public PurchaseDto createPurchase(PurchaseDto dto) {
        Purchase purchase = new Purchase();
        purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setStatus(Purchase.PurchaseStatus.PENDING);
        purchase.setNotes(dto.getNotes());
        purchase.setPurchaseOrderNumber("PO-" + System.currentTimeMillis());
        purchase.setSupplier(resolveSupplier(dto.getSupplierId()));
        purchase.setWarehouse(resolveWarehouse(dto.getWarehouseId()));

        purchase = purchaseRepository.save(purchase);
        BigDecimal totalAmount = persistItems(purchase, dto.getItems());
        purchase.setTotalAmount(totalAmount);
        purchase = purchaseRepository.save(purchase);

        // Re-read so the eager `purchaseItems` collection is populated in the DTO.
        return mapToDto(purchaseRepository.findById(purchase.getId()).orElse(purchase));
    }

    @Override
    @Transactional
    public PurchaseDto updatePurchase(Long id, PurchaseDto dto) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + id));

        purchase.setNotes(dto.getNotes());
        if (dto.getSupplierId() != null) {
            purchase.setSupplier(resolveSupplier(dto.getSupplierId()));
        }
        if (dto.getWarehouseId() != null) {
            purchase.setWarehouse(resolveWarehouse(dto.getWarehouseId()));
        }

        BigDecimal totalAmount = purchase.getTotalAmount();
        if (dto.getItems() != null) {
            if (purchase.getPurchaseItems() != null) {
                purchaseItemRepository.deleteAll(purchase.getPurchaseItems());
                purchase.getPurchaseItems().clear();
            }
            totalAmount = persistItems(purchase, dto.getItems());
        }
        purchase.setTotalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);
        purchase = purchaseRepository.save(purchase);

        // Re-read so the eager `purchaseItems` collection reflects the latest state.
        return mapToDto(purchaseRepository.findById(purchase.getId()).orElse(purchase));
    }

    @Override
    public PurchaseDto getPurchaseById(Long id) {
        return mapToDto(purchaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + id)));
    }

    @Override
    public PurchaseDto getPurchaseByOrderNumber(String orderNumber) {
        return mapToDto(purchaseRepository.findByPurchaseOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found for order: " + orderNumber)));
    }

    @Override
    public List<PurchaseDto> getAllPurchases() {
        return purchaseRepository.findAll().stream()
                .sorted(Comparator.comparing(Purchase::getPurchaseDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePurchase(Long id) {
        if (!purchaseRepository.existsById(id)) {
            throw new IllegalArgumentException("Purchase not found: " + id);
        }
        purchaseRepository.deleteById(id);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public PurchaseDto receivePurchase(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + id));

        if (purchase.getStatus() == Purchase.PurchaseStatus.RECEIVED) {
            throw new IllegalArgumentException("Purchase " + purchase.getPurchaseOrderNumber() + " is already received.");
        }

        purchase.setStatus(Purchase.PurchaseStatus.RECEIVED);
        purchase = purchaseRepository.save(purchase);

        // Captured for the lambda below: `purchase` is reassigned above, so it is
        // not effectively final and cannot be referenced inside the closure.
        final String purchaseOrderNumber = purchase.getPurchaseOrderNumber();

        if (purchase.getPurchaseItems() != null) {
            purchase.getPurchaseItems().forEach(item -> {
                Product product = item.getProduct();
                int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
                int newStock = currentStock + (item.getQuantity() != null ? item.getQuantity() : 0);
                product.setCurrentStock(newStock);
                productRepository.save(product);

                if (product.getReorderLevel() != null && newStock > product.getReorderLevel()) {
                    alertRepository.findByProductAndTypeAndResolvedFalse(product, Alert.AlertType.LOW_STOCK)
                            .forEach(alert -> {
                                alert.setResolved(true);
                                alert.setResolvedAt(LocalDateTime.now());
                                alertRepository.save(alert);
                            });
                }

                // Ask a worker to re-sync the stock snapshot for this product. The
                // message is only published once the receipt has committed, so the
                // worker never observes a half-applied stock level.
                messagePublisher.publishAfterCommit(InventorySyncJobRequest.forProduct(
                        product.getId(),
                        "Received purchase " + purchaseOrderNumber,
                        "purchasing-api"));
            });
        }

        return mapToDto(purchase);
    }

    private Supplier resolveSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("Supplier is required.");
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
    }

    private Warehouse resolveWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse is required.");
        }
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + warehouseId));
    }

    private BigDecimal persistItems(Purchase purchase, List<PurchaseItemDto> itemDtos) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (itemDtos == null) {
            return totalAmount;
        }
        for (PurchaseItemDto itemDto : itemDtos) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDto.getProductId()));
            int quantity = itemDto.getQuantity() != null ? itemDto.getQuantity() : 0;
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero for '" + product.getName() + "'.");
            }
            BigDecimal unitPrice = itemDto.getUnitPrice() != null
                    ? itemDto.getUnitPrice()
                    : (product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO);

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            item.setBatchNumber(itemDto.getBatchNumber());
            purchaseItemRepository.save(item);
            totalAmount = totalAmount.add(item.getTotalPrice());
        }
        return totalAmount;
    }

    private PurchaseDto mapToDto(Purchase purchase) {
        PurchaseDto dto = new PurchaseDto();
        dto.setId(purchase.getId());
        dto.setPurchaseOrderNumber(purchase.getPurchaseOrderNumber());
        dto.setSupplierId(purchase.getSupplier().getId());
        dto.setSupplierName(purchase.getSupplier().getName());
        dto.setWarehouseId(purchase.getWarehouse().getId());
        dto.setWarehouseName(purchase.getWarehouse().getName());
        dto.setPurchaseDate(purchase.getPurchaseDate());
        dto.setStatus(purchase.getStatus() != null ? purchase.getStatus().name() : null);
        dto.setTotalAmount(purchase.getTotalAmount());
        dto.setNotes(purchase.getNotes());
        dto.setItems(purchase.getPurchaseItems() == null ? new ArrayList<>()
                : purchase.getPurchaseItems().stream().map(this::mapItemToDto).collect(Collectors.toList()));
        return dto;
    }

    private PurchaseItemDto mapItemToDto(PurchaseItem item) {
        PurchaseItemDto dto = new PurchaseItemDto();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setBatchNumber(item.getBatchNumber());
        return dto;
    }
}
