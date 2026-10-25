package com.unique.ims.backend.controller;

import com.unique.ims.backend.dto.PurchaseDto;
import com.unique.ims.backend.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ResponseEntity<PurchaseDto> createPurchase(@Valid @RequestBody PurchaseDto purchaseDto) {
        return ResponseEntity.ok(purchaseService.createPurchase(purchaseDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseDto> updatePurchase(@PathVariable Long id, @Valid @RequestBody PurchaseDto purchaseDto) {
        return ResponseEntity.ok(purchaseService.updatePurchase(id, purchaseDto));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseDto> receivePurchase(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.receivePurchase(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseDto> getPurchaseById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(id));
    }

    @GetMapping("/order/{orderNumber}")
    public ResponseEntity<PurchaseDto> getPurchaseByOrderNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(purchaseService.getPurchaseByOrderNumber(orderNumber));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseDto>> getAllPurchases() {
        return ResponseEntity.ok(purchaseService.getAllPurchases());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long id) {
        purchaseService.deletePurchase(id);
        return ResponseEntity.noContent().build();
    }
}
