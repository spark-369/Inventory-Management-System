package com.unique.ims.backend.controller;

import com.unique.ims.backend.dto.StockBatchDto;
import com.unique.ims.backend.service.StockBatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-batches")
public class StockBatchController {

    private final StockBatchService stockBatchService;

    public StockBatchController(StockBatchService stockBatchService) {
        this.stockBatchService = stockBatchService;
    }

    @PostMapping
    public ResponseEntity<StockBatchDto> createStockBatch(@Valid @RequestBody StockBatchDto stockBatchDto) {
        return ResponseEntity.ok(stockBatchService.createStockBatch(stockBatchDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockBatchDto> updateStockBatch(@PathVariable Long id, @Valid @RequestBody StockBatchDto stockBatchDto) {
        return ResponseEntity.ok(stockBatchService.updateStockBatch(id, stockBatchDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockBatchDto> getStockBatchById(@PathVariable Long id) {
        return ResponseEntity.ok(stockBatchService.getStockBatchById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockBatchDto>> getStockBatchesByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockBatchService.getStockBatchesByProduct(productId));
    }

    @GetMapping
    public ResponseEntity<List<StockBatchDto>> getAllStockBatches() {
        return ResponseEntity.ok(stockBatchService.getAllStockBatches());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStockBatch(@PathVariable Long id) {
        stockBatchService.deleteStockBatch(id);
        return ResponseEntity.noContent().build();
    }
}
