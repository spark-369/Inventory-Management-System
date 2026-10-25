package com.unique.ims.backend.service;

import com.unique.ims.backend.dto.StockMovementDto;
import java.util.List;

public interface StockMovementService {
    StockMovementDto createStockMovement(StockMovementDto stockMovementDto);
    List<StockMovementDto> getStockMovementsByProduct(Long productId);
    List<StockMovementDto> getStockMovementsByWarehouse(Long warehouseId);
    List<StockMovementDto> getAllStockMovements();
}