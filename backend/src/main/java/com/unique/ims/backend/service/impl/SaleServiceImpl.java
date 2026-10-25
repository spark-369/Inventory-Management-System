package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.config.CacheNames;
import com.unique.ims.backend.dto.SaleDto;
import com.unique.ims.backend.dto.SaleItemDto;
import com.unique.ims.backend.messaging.DomainMessagePublisher;
import com.unique.ims.backend.messaging.request.AlertScanJobRequest;
import com.unique.ims.backend.messaging.request.OrderConfirmationJobRequest;
import com.unique.ims.backend.model.Customer;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.model.Sale;
import com.unique.ims.backend.model.SaleItem;
import com.unique.ims.backend.model.Warehouse;
import com.unique.ims.backend.repository.CustomerRepository;
import com.unique.ims.backend.repository.ProductRepository;
import com.unique.ims.backend.repository.SaleItemRepository;
import com.unique.ims.backend.repository.SaleRepository;
import com.unique.ims.backend.repository.WarehouseRepository;
import com.unique.ims.backend.service.SaleService;
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
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;
    private final DomainMessagePublisher messagePublisher;

    public SaleServiceImpl(SaleRepository saleRepository,
                           SaleItemRepository saleItemRepository,
                           ProductRepository productRepository,
                           WarehouseRepository warehouseRepository,
                           CustomerRepository customerRepository,
                           DomainMessagePublisher messagePublisher) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.customerRepository = customerRepository;
        this.messagePublisher = messagePublisher;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public SaleDto createSale(SaleDto saleDto) {
        Sale sale = new Sale();
        sale.setSaleDate(LocalDateTime.now());
        sale.setStatus(Sale.SaleStatus.PENDING);
        sale.setNotes(saleDto.getNotes());
        sale.setDiscount(saleDto.getDiscount() != null ? saleDto.getDiscount() : BigDecimal.ZERO);
        sale.setTaxAmount(saleDto.getTaxAmount() != null ? saleDto.getTaxAmount() : BigDecimal.ZERO);
        sale.setInvoiceNumber("INV-" + System.currentTimeMillis());
        sale.setWarehouse(resolveWarehouse(saleDto.getWarehouseId()));

        if (saleDto.getCustomerId() != null) {
            Customer customer = customerRepository.findById(saleDto.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + saleDto.getCustomerId()));
            sale.setCustomer(customer);
        }

        sale = saleRepository.save(sale);
        BigDecimal totalAmount = persistItems(sale, saleDto.getItems());

        sale.setTotalAmount(totalAmount);
        sale.setGrandTotal(totalAmount.subtract(sale.getDiscount()).add(sale.getTaxAmount()));
        sale = saleRepository.save(sale);

        // Publish an ORDER_CONFIRMATION message once the sale is committed. The
        // notification is rendered on a worker thread instead of blocking this
        // request, and the message survives a restart because JobRunr persists it.
        publishOrderConfirmation(sale, saleDto.getItems() == null ? 0 : saleDto.getItems().size());

        // Re-read so the eager `saleItems` collection is populated in the DTO.
        return mapToDto(saleRepository.findById(sale.getId()).orElse(sale));
    }

    /**
     * Emits the {@code ORDER_CONFIRMATION} message for a freshly created sale.
     * Only a denormalised snapshot travels through the broker — never the JPA
     * entity — so the payload stays small and JSON-serialisable.
     *
     * @param itemCount number of lines on the request, because the freshly saved
     *                  entity's {@code saleItems} collection is not yet populated
     */
    private void publishOrderConfirmation(Sale sale, int itemCount) {
        Customer customer = sale.getCustomer();

        messagePublisher.publishAfterCommit(new OrderConfirmationJobRequest(
                sale.getId(),
                sale.getInvoiceNumber(),
                customer != null ? customer.getId() : null,
                customer != null ? customer.getName() : null,
                customer != null ? customer.getEmail() : null,
                sale.getGrandTotal(),
                itemCount,
                sale.getSaleDate() != null ? sale.getSaleDate().toString() : null));
    }

    @Override
    @Transactional
    public SaleDto updateSale(Long id, SaleDto saleDto) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + id));

        sale.setNotes(saleDto.getNotes());
        if (saleDto.getDiscount() != null) {
            sale.setDiscount(saleDto.getDiscount());
        }
        if (saleDto.getTaxAmount() != null) {
            sale.setTaxAmount(saleDto.getTaxAmount());
        }
        if (saleDto.getWarehouseId() != null) {
            sale.setWarehouse(resolveWarehouse(saleDto.getWarehouseId()));
        }
        if (saleDto.getCustomerId() != null) {
            sale.setCustomer(customerRepository.findById(saleDto.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + saleDto.getCustomerId())));
        } else {
            sale.setCustomer(null);
        }

        BigDecimal totalAmount = sale.getTotalAmount();
        if (saleDto.getItems() != null) {
            if (sale.getSaleItems() != null) {
                saleItemRepository.deleteAll(sale.getSaleItems());
                sale.getSaleItems().clear();
            }
            totalAmount = persistItems(sale, saleDto.getItems());
        }

        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
        sale.setTotalAmount(totalAmount);
        sale.setGrandTotal(totalAmount.subtract(sale.getDiscount()).add(sale.getTaxAmount()));
        sale = saleRepository.save(sale);

        // Re-read so the eager `saleItems` collection reflects the latest state.
        return mapToDto(saleRepository.findById(sale.getId()).orElse(sale));
    }

    @Override
    public SaleDto getSaleById(Long id) {
        return mapToDto(saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + id)));
    }

    @Override
    public SaleDto getSaleByInvoiceNumber(String invoiceNumber) {
        return mapToDto(saleRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found for invoice: " + invoiceNumber)));
    }

    @Override
    public List<SaleDto> getAllSales() {
        return saleRepository.findAll().stream()
                .sorted(Comparator.comparing(Sale::getSaleDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSale(Long id) {
        if (!saleRepository.existsById(id)) {
            throw new IllegalArgumentException("Sale not found: " + id);
        }
        saleRepository.deleteById(id);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public SaleDto completeSale(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + id));

        if (sale.getStatus() == Sale.SaleStatus.COMPLETED) {
            throw new IllegalArgumentException("Sale " + sale.getInvoiceNumber() + " is already completed.");
        }

        List<SaleItem> items = sale.getSaleItems() == null
                ? new ArrayList<>()
                : new ArrayList<>(sale.getSaleItems());

        for (SaleItem item : items) {
            Product product = item.getProduct();
            int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
            if (item.getQuantity() != null && item.getQuantity() > currentStock) {
                throw new IllegalArgumentException("Insufficient stock for '" + product.getName()
                        + "'. Available: " + currentStock + ", requested: " + item.getQuantity() + ".");
            }
        }

        sale.setStatus(Sale.SaleStatus.COMPLETED);
        sale = saleRepository.save(sale);

        for (SaleItem item : items) {
            Product product = item.getProduct();
            int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
            int newStock = Math.max(0, currentStock - item.getQuantity());
            product.setCurrentStock(newStock);
            productRepository.save(product);

            // Delegate the reorder-level check to a worker: the low-stock scan runs
            // after commit, so the HTTP request stays fast even for large orders.
            messagePublisher.publishAfterCommit(AlertScanJobRequest.forProduct(
                    product.getId(),
                    "Stock reduced by sale " + sale.getInvoiceNumber(),
                    "sales-api"));
        }

        return mapToDto(sale);
    }

    private Warehouse resolveWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse is required.");
        }
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + warehouseId));
    }

    private BigDecimal persistItems(Sale sale, List<SaleItemDto> itemDtos) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (itemDtos == null) {
            return totalAmount;
        }
        for (SaleItemDto itemDto : itemDtos) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDto.getProductId()));
            int quantity = itemDto.getQuantity() != null ? itemDto.getQuantity() : 0;
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero for '" + product.getName() + "'.");
            }
            BigDecimal unitPrice = itemDto.getUnitPrice() != null
                    ? itemDto.getUnitPrice()
                    : (product.getSellingPrice() != null ? product.getSellingPrice() : BigDecimal.ZERO);

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            item.setBatchNumber(itemDto.getBatchNumber());
            saleItemRepository.save(item);
            totalAmount = totalAmount.add(item.getTotalPrice());
        }
        return totalAmount;
    }

    private SaleDto mapToDto(Sale sale) {
        SaleDto dto = new SaleDto();
        dto.setId(sale.getId());
        dto.setInvoiceNumber(sale.getInvoiceNumber());
        dto.setWarehouseId(sale.getWarehouse().getId());
        dto.setWarehouseName(sale.getWarehouse().getName());
        dto.setSaleDate(sale.getSaleDate());
        dto.setStatus(sale.getStatus() != null ? sale.getStatus().name() : null);
        dto.setTotalAmount(sale.getTotalAmount());
        dto.setDiscount(sale.getDiscount());
        dto.setTaxAmount(sale.getTaxAmount());
        dto.setGrandTotal(sale.getGrandTotal());
        dto.setNotes(sale.getNotes());
        if (sale.getCustomer() != null) {
            dto.setCustomerId(sale.getCustomer().getId());
            dto.setCustomerName(sale.getCustomer().getName());
        }
        dto.setItems(sale.getSaleItems() == null ? new ArrayList<>()
                : sale.getSaleItems().stream().map(this::mapItemToDto).collect(Collectors.toList()));
        return dto;
    }

    private SaleItemDto mapItemToDto(SaleItem item) {
        SaleItemDto dto = new SaleItemDto();
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
