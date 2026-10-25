package com.unique.ims.backend.service;

import com.unique.ims.backend.dto.WarehouseDto;
import java.util.List;

public interface WarehouseService {
    WarehouseDto createWarehouse(WarehouseDto warehouseDto);
    WarehouseDto updateWarehouse(Long id, WarehouseDto warehouseDto);
    WarehouseDto getWarehouseById(Long id);
    List<WarehouseDto> getAllWarehouses();
    void deleteWarehouse(Long id);
}