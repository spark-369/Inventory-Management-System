package com.unique.ims.backend.controller;

import com.unique.ims.backend.dto.StockMovementDto;
import com.unique.ims.backend.service.StockMovementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @PostMapping
    public ResponseEntity<StockMovementDto> createStockMovement(@Valid @RequestBody StockMovementDto stockMovementDto) {
        return ResponseEntity.ok(stockMovementService.createStockMovement(stockMovementDto));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockMovementDto>> getStockMovementsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockMovementService.getStockMovementsByProduct(productId));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<StockMovementDto>> getStockMovementsByWarehouse(@PathVariable Long warehouseId) {
        return ResponseEntity.ok(stockMovementService.getStockMovementsByWarehouse(warehouseId));
    }

    @GetMapping
    public ResponseEntity<List<StockMovementDto>> getAllStockMovements() {
        return ResponseEntity.ok(stockMovementService.getAllStockMovements());
    }
}
