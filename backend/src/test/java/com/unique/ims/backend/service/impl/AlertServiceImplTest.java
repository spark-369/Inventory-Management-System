package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.dto.AlertDto;
import com.unique.ims.backend.model.Alert;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.model.StockBatch;
import com.unique.ims.backend.repository.AlertRepository;
import com.unique.ims.backend.repository.ProductRepository;
import com.unique.ims.backend.repository.StockBatchRepository;
import com.unique.ims.backend.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the alert scanning rules.
 *
 * <p>The interesting behaviour is the "don't spam" guard: a scan must raise an
 * alert only when the condition holds <em>and</em> no unresolved alert of that
 * type already exists. Each test pins one of those two halves.</p>
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockBatchRepository stockBatchRepository;

    @InjectMocks
    private AlertServiceImpl alertService;

    private static final LocalDate TODAY = LocalDate.now();

    @Test
    @DisplayName("scanLowStock creates an alert for a product at or below its reorder level")
    void scanLowStockCreatesAlertForLowProduct() {
        Product low = TestData.product(1L, "Coffee", 2, 5);
        when(productRepository.findAll()).thenReturn(List.of(low));
        when(alertRepository.findByProductAndTypeAndResolvedFalse(low, Alert.AlertType.LOW_STOCK))
                .thenReturn(List.of());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int created = alertService.scanLowStock();

        assertThat(created).isEqualTo(1);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        Alert saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(Alert.AlertType.LOW_STOCK);
        assertThat(saved.getProduct()).isSameAs(low);
        assertThat(saved.isResolved()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getMessage()).contains("Coffee").contains("2").contains("5");
    }

    @Test
    @DisplayName("scanLowStock skips a product that is above its reorder level")
    void scanLowStockSkipsHealthyProduct() {
        Product healthy = TestData.product(1L, "Tea", 50, 5);
        when(productRepository.findAll()).thenReturn(List.of(healthy));

        int created = alertService.scanLowStock();

        assertThat(created).isZero();
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("scanLowStock does not duplicate an existing unresolved alert")
    void scanLowStockSkipsWhenAlertAlreadyOpen() {
        Product low = TestData.product(1L, "Coffee", 1, 5);
        Alert existing = TestData.alert(9L, Alert.AlertType.LOW_STOCK, low, false);
        when(productRepository.findAll()).thenReturn(List.of(low));
        when(alertRepository.findByProductAndTypeAndResolvedFalse(low, Alert.AlertType.LOW_STOCK))
                .thenReturn(List.of(existing));

        int created = alertService.scanLowStock();

        assertThat(created).isZero();
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("scanLowStock ignores products with a null stock or reorder level")
    void scanLowStockIgnoresIncompleteProducts() {
        Product noReorder = TestData.product(1L, "No reorder", 3, null);
        Product noStock = TestData.product(2L, "No stock", 0, 5);
        noStock.setCurrentStock(null);
        when(productRepository.findAll()).thenReturn(List.of(noReorder, noStock));

        int created = alertService.scanLowStock();

        assertThat(created).isZero();
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("scanLowStock counts only the products that actually produced an alert")
    void scanLowStockCountsOnlyNewAlerts() {
        Product newlyLow = TestData.product(1L, "Newly low", 1, 5);
        Product alreadyFlagged = TestData.product(2L, "Already flagged", 1, 5);
        Product healthy = TestData.product(3L, "Healthy", 99, 5);

        when(productRepository.findAll()).thenReturn(List.of(newlyLow, alreadyFlagged, healthy));
        when(alertRepository.findByProductAndTypeAndResolvedFalse(newlyLow, Alert.AlertType.LOW_STOCK))
                .thenReturn(List.of());
        when(alertRepository.findByProductAndTypeAndResolvedFalse(alreadyFlagged, Alert.AlertType.LOW_STOCK))
                .thenReturn(List.of(TestData.alert(1L, Alert.AlertType.LOW_STOCK, alreadyFlagged, false)));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(alertService.scanLowStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("scanLowStockForProduct requires an id and a known product")
    void scanLowStockForProductValidatesInput() {
        assertThatThrownBy(() -> alertService.scanLowStockForProduct(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");

        when(productRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> alertService.scanLowStockForProduct(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("scanLowStockForProduct only evaluates the requested product")
    void scanLowStockForProductScopesToOneProduct() {
        Product low = TestData.product(5L, "Scoped", 0, 3);
        when(productRepository.findById(5L)).thenReturn(Optional.of(low));
        when(alertRepository.findByProductAndTypeAndResolvedFalse(low, Alert.AlertType.LOW_STOCK))
                .thenReturn(List.of());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(alertService.scanLowStockForProduct(5L)).isEqualTo(1);
        verify(productRepository, never()).findAll();
    }

    @Test
    @DisplayName("scanExpiry raises EXPIRY_ALERT for a batch that already expired")
    void scanExpiryFlagsExpiredBatch() {
        Product product = TestData.product(1L, "Milk", 10, 2);
        StockBatch expired = TestData.batch(1L, product, "LOT-1", TODAY.minusDays(3));
        when(stockBatchRepository.findAll()).thenReturn(List.of(expired));
        when(alertRepository.findByProductAndBatchNumberAndTypeAndResolvedFalse(
                product, "LOT-1", Alert.AlertType.EXPIRY_ALERT)).thenReturn(List.of());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int created = alertService.scanExpiry(7);

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(Alert.AlertType.EXPIRY_ALERT);
        assertThat(captor.getValue().getBatchNumber()).isEqualTo("LOT-1");
        assertThat(captor.getValue().getExpiryDate()).isEqualTo(TODAY.minusDays(3));
    }

    @Test
    @DisplayName("scanExpiry raises EXPIRY_WARNING inside the warning window")
    void scanExpiryWarnsWithinWindow() {
        Product product = TestData.product(1L, "Yoghurt", 10, 2);
        StockBatch soon = TestData.batch(1L, product, "LOT-2", TODAY.plusDays(3));
        when(stockBatchRepository.findAll()).thenReturn(List.of(soon));
        when(alertRepository.findByProductAndBatchNumberAndTypeAndResolvedFalse(
                product, "LOT-2", Alert.AlertType.EXPIRY_WARNING)).thenReturn(List.of());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int created = alertService.scanExpiry(7);

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(Alert.AlertType.EXPIRY_WARNING);
    }

    @Test
    @DisplayName("scanExpiry leaves batches outside the window alone")
    void scanExpiryIgnoresFarFutureBatch() {
        Product product = TestData.product(1L, "Rice", 10, 2);
        StockBatch later = TestData.batch(1L, product, "LOT-3", TODAY.plusDays(60));
        when(stockBatchRepository.findAll()).thenReturn(List.of(later));

        assertThat(alertService.scanExpiry(7)).isZero();
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("scanExpiry skips batches with no expiry date")
    void scanExpirySkipsBatchWithoutExpiry() {
        Product product = TestData.product(1L, "Canned", 10, 2);
        StockBatch noExpiry = TestData.batch(1L, product, "LOT-4", null);
        when(stockBatchRepository.findAll()).thenReturn(List.of(noExpiry));

        assertThat(alertService.scanExpiry(7)).isZero();
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("scanExpiry honours the window boundary: exactly N days still warns")
    void scanExpiryIncludesBoundaryDay() {
        Product product = TestData.product(1L, "Boundary", 10, 2);
        StockBatch boundary = TestData.batch(1L, product, "LOT-5", TODAY.plusDays(7));
        when(stockBatchRepository.findAll()).thenReturn(List.of(boundary));
        when(alertRepository.findByProductAndBatchNumberAndTypeAndResolvedFalse(
                product, "LOT-5", Alert.AlertType.EXPIRY_WARNING)).thenReturn(List.of());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(alertService.scanExpiry(7)).isEqualTo(1);
    }

    @Test
    @DisplayName("scanExpiry falls back to the default window for a non-positive argument")
    void scanExpiryFallsBackToDefaultWindow() {
        Product product = TestData.product(1L, "Default", 10, 2);
        StockBatch withinDefault = TestData.batch(1L, product, "LOT-6", TODAY.plusDays(5));
        when(stockBatchRepository.findAll()).thenReturn(List.of(withinDefault));
        when(alertRepository.findByProductAndBatchNumberAndTypeAndResolvedFalse(
                product, "LOT-6", Alert.AlertType.EXPIRY_WARNING)).thenReturn(List.of());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(alertService.scanExpiry(0)).isEqualTo(1);
    }

    @Test
    @DisplayName("createAlert rejects an unsupported alert type with a helpful message")
    void createAlertRejectsUnknownType() {
        AlertDto dto = new AlertDto();
        dto.setType("NOT_A_TYPE");

        assertThatThrownBy(() -> alertService.createAlert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported alert type")
                .hasMessageContaining("LOW_STOCK");
    }

    @Test
    @DisplayName("resolveAlert stamps resolved=true and a resolution timestamp")
    void resolveAlertMarksResolved() {
        Product product = TestData.product(1L, "Coffee", 1, 5);
        Alert open = TestData.alert(4L, Alert.AlertType.LOW_STOCK, product, false);
        when(alertRepository.findById(4L)).thenReturn(Optional.of(open));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertDto dto = alertService.resolveAlert(4L);

        assertThat(dto.isResolved()).isTrue();
        assertThat(open.getResolvedAt()).isNotNull();
        assertThat(dto.getProductName()).isEqualTo("Coffee");
    }

    @Test
    @DisplayName("resolveAlert reports a missing alert instead of silently succeeding")
    void resolveAlertFailsForUnknownId() {
        when(alertRepository.findById(eq(99L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.resolveAlert(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Alert not found");
    }
}
