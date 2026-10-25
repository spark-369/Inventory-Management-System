package com.unique.ims.backend.support;

import com.unique.ims.backend.model.Alert;
import com.unique.ims.backend.model.Category;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.model.Sale;
import com.unique.ims.backend.model.SaleItem;
import com.unique.ims.backend.model.StockBatch;
import com.unique.ims.backend.model.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Fixture factory for the JPA entities used across the unit tests.
 *
 * <p>Centralising construction keeps each test focused on behaviour instead of
 * on twenty lines of setters, and means a new non-null column only has to be
 * handled in one place.</p>
 */
public final class TestData {

    private TestData() {
    }

    public static Warehouse warehouse(Long id, String name) {
        return Warehouse.builder().id(id).name(name).active(true).build();
    }

    public static Category category(Long id, String name) {
        return Category.builder().id(id).name(name).build();
    }

    public static Product product(Long id, String name, int currentStock, Integer reorderLevel) {
        Product product = new Product();
        product.setId(id);
        product.setSku("SKU-" + id);
        product.setName(name);
        product.setCurrentStock(currentStock);
        product.setReorderLevel(reorderLevel);
        product.setPurchasePrice(new BigDecimal("10.00"));
        product.setSellingPrice(new BigDecimal("15.00"));
        product.setActive(true);
        return product;
    }

    public static StockBatch batch(Long id, Product product, String batchNumber, LocalDate expiryDate) {
        return StockBatch.builder()
                .id(id)
                .product(product)
                .warehouse(warehouse(1L, "Main"))
                .batchNumber(batchNumber)
                .expiryDate(expiryDate)
                .quantity(10)
                .active(true)
                .build();
    }

    public static Sale sale(Long id, String invoiceNumber, Sale.SaleStatus status, Warehouse warehouse) {
        return Sale.builder()
                .id(id)
                .invoiceNumber(invoiceNumber)
                .warehouse(warehouse)
                .status(status)
                .saleDate(LocalDateTime.now())
                .discount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .build();
    }

    /** Attaches a single line item so {@code completeSale} has work to do. */
    public static Sale saleWithItem(Long id, String invoiceNumber, Product product, int quantity) {
        Sale sale = sale(id, invoiceNumber, Sale.SaleStatus.PENDING, warehouse(1L, "Main"));
        SaleItem item = SaleItem.builder()
                .id(1L)
                .sale(sale)
                .product(product)
                .quantity(quantity)
                .unitPrice(product.getSellingPrice())
                .totalPrice(product.getSellingPrice().multiply(BigDecimal.valueOf(quantity)))
                .build();
        Set<SaleItem> items = new LinkedHashSet<>();
        items.add(item);
        sale.setSaleItems(items);
        return sale;
    }

    public static Alert alert(Long id, Alert.AlertType type, Product product, boolean resolved) {
        return Alert.builder()
                .id(id)
                .type(type)
                .message("existing " + type + " alert")
                .product(product)
                .resolved(resolved)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
