package com.unique.ims.backend.repository;

import com.unique.ims.backend.model.StockMovement;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByProduct(Product product);
    List<StockMovement> findByWarehouse(Warehouse warehouse);
    List<StockMovement> findByProductAndWarehouse(Product product, Warehouse warehouse);
}