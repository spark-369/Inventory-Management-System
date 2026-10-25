package com.unique.ims.backend.service;

import com.unique.ims.backend.dto.PurchaseDto;
import java.util.List;

public interface PurchaseService {
    PurchaseDto createPurchase(PurchaseDto purchaseDto);
    PurchaseDto updatePurchase(Long id, PurchaseDto purchaseDto);
    PurchaseDto getPurchaseById(Long id);
    PurchaseDto getPurchaseByOrderNumber(String orderNumber);
    List<PurchaseDto> getAllPurchases();
    void deletePurchase(Long id);
    PurchaseDto receivePurchase(Long id);
}