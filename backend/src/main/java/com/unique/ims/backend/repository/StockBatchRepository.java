package com.unique.ims.backend.repository;

import com.unique.ims.backend.model.StockBatch;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {
    List<StockBatch> findByProduct(Product product);
    List<StockBatch> findByWarehouse(Warehouse warehouse);
    List<StockBatch> findByProductAndWarehouse(Product product, Warehouse warehouse);
    StockBatch findByProductAndBatchNumber(Product product, String batchNumber);
}