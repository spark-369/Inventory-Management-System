package com.unique.ims.backend.repository;

import com.unique.ims.backend.model.SaleItem;
import com.unique.ims.backend.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    List<SaleItem> findBySale(Sale sale);
}