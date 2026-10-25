package com.unique.ims.backend.service;

import com.unique.ims.backend.dto.StockBatchDto;
import java.util.List;

public interface StockBatchService {
    StockBatchDto createStockBatch(StockBatchDto stockBatchDto);
    StockBatchDto updateStockBatch(Long id, StockBatchDto stockBatchDto);
    StockBatchDto getStockBatchById(Long id);
    List<StockBatchDto> getStockBatchesByProduct(Long productId);
    List<StockBatchDto> getAllStockBatches();
    void deleteStockBatch(Long id);
}