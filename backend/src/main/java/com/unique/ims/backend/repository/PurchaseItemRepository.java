package com.unique.ims.backend.repository;

import com.unique.ims.backend.model.PurchaseItem;
import com.unique.ims.backend.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {
    List<PurchaseItem> findByPurchase(Purchase purchase);
}