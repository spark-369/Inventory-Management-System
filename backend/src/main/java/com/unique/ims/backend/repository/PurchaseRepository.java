package com.unique.ims.backend.repository;

import com.unique.ims.backend.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    Optional<Purchase> findByPurchaseOrderNumber(String purchaseOrderNumber);
}