package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.dto.StockMovementDto;
import com.unique.ims.backend.messaging.DomainMessagePublisher;
import com.unique.ims.backend.messaging.request.AlertScanJobRequest;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.model.StockMovement;
import com.unique.ims.backend.model.Warehouse;
import com.unique.ims.backend.repository.ProductRepository;
import com.unique.ims.backend.repository.StockMovementRepository;
import com.unique.ims.backend.repository.WarehouseRepository;
import com.unique.ims.backend.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the stock-effect rules of {@code createStockMovement}.
 *
 * <p>This is the most safety-critical arithmetic in the service layer: an IN
 * movement must raise the stock level, an OUT movement must lower it without ever
 * going negative, and an ADJUSTMENT must set it absolutely. A regression here
 * silently corrupts inventory, so each direction gets its own test.</p>
 */
@ExtendWith(MockitoExtension.class)
class StockMovementServiceImplTest {

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private DomainMessagePublisher messagePublisher;

    @InjectMocks
    private StockMovementServiceImpl service;

    private static final Long PRODUCT_ID = 1L;
    private static final Long WAREHOUSE_ID = 2L;

    private StockMovementDto request(String movementType, int quantity) {
        StockMovementDto dto = new StockMovementDto();
        dto.setProductId(PRODUCT_ID);
        dto.setWarehouseId(WAREHOUSE_ID);
        dto.setMovementType(movementType);
        dto.setQuantity(quantity);
        dto.setReferenceType("SYSTEM");
        return dto;
    }

    /** Stubs the repositories and makes save() echo the entity back with an id. */
    private Product givenProduct(int currentStock) {
        Product product = TestData.product(PRODUCT_ID, "Coffee", currentStock, 5);
        Warehouse warehouse = TestData.warehouse(WAREHOUSE_ID, "Main");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(warehouse));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            movement.setId(100L);
            return movement;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return product;
    }

    @Test
    @DisplayName("an IN movement increases the stock level")
    void inMovementIncreasesStock() {
        Product product = givenProduct(10);

        service.createStockMovement(request("IN", 5));

        assertThat(product.getCurrentStock()).isEqualTo(15);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("an OUT movement decreases the stock level")
    void outMovementDecreasesStock() {
        Product product = givenProduct(10);

        service.createStockMovement(request("OUT", 4));

        assertThat(product.getCurrentStock()).isEqualTo(6);
    }

    @Test
    @DisplayName("an OUT movement never drives stock below zero")
    void outMovementClampsAtZero() {
        Product product = givenProduct(3);

        service.createStockMovement(request("OUT", 10));

        assertThat(product.getCurrentStock()).isZero();
    }

    @Test
    @DisplayName("an ADJUSTMENT sets the stock absolutely rather than adding to it")
    void adjustmentSetsAbsoluteStock() {
        Product product = givenProduct(100);

        service.createStockMovement(request("ADJUSTMENT", 7));

        assertThat(product.getCurrentStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("rejects a zero or negative quantity before touching stock")
    void rejectsNonPositiveQuantity() {
        StockMovementDto dto = request("IN", 0);
        Product product = TestData.product(PRODUCT_ID, "Coffee", 10, 5);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(WAREHOUSE_ID))
                .thenReturn(Optional.of(TestData.warehouse(WAREHOUSE_ID, "Main")));

        assertThatThrownBy(() -> service.createStockMovement(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        verify(stockMovementRepository, never()).save(any(StockMovement.class));
        assertThat(product.getCurrentStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("rejects an unknown movement type with the list of valid values")
    void rejectsUnknownMovementType() {
        StockMovementDto dto = request("SIDEWAYS", 1);
        Product product = TestData.product(PRODUCT_ID, "Coffee", 10, 5);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(WAREHOUSE_ID))
                .thenReturn(Optional.of(TestData.warehouse(WAREHOUSE_ID, "Main")));

        assertThatThrownBy(() -> service.createStockMovement(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported movement type")
                .hasMessageContaining("ADJUSTMENT");
    }

    @Test
    @DisplayName("rejects an unknown product")
    void rejectsUnknownProduct() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createStockMovement(request("IN", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("defaults a blank reference type to SYSTEM")
    void defaultsReferenceTypeToSystem() {
        Product product = givenProduct(10);
        StockMovementDto dto = request("IN", 1);
        dto.setReferenceType(null);

        StockMovementDto result = service.createStockMovement(dto);

        assertThat(result.getReferenceType()).isEqualTo("SYSTEM");
        assertThat(product.getCurrentStock()).isEqualTo(11);
    }

    @Test
    @DisplayName("publishes a LOW_STOCK_SCAN message after the movement is recorded")
    void publishesLowStockScanMessage() {
        givenProduct(2);

        service.createStockMovement(request("OUT", 1));

        ArgumentCaptor<AlertScanJobRequest> captor = ArgumentCaptor.forClass(AlertScanJobRequest.class);
        verify(messagePublisher).publishAfterCommit(captor.capture());
        assertThat(captor.getValue().getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(captor.getValue().getMessageType().name()).isEqualTo("LOW_STOCK_SCAN");
    }

    @Test
    @DisplayName("returns a DTO carrying the resolved product and warehouse names")
    void mapsToDto() {
        givenProduct(10);

        StockMovementDto result = service.createStockMovement(request("IN", 2));

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getProductName()).isEqualTo("Coffee");
        assertThat(result.getWarehouseName()).isEqualTo("Main");
        assertThat(result.getMovementType()).isEqualTo("IN");
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getCreatedAt()).isNotNull();
    }
}
