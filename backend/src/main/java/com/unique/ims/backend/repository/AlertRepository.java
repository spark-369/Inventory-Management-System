package com.unique.ims.backend.repository;

import com.unique.ims.backend.model.Alert;
import com.unique.ims.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByResolvedFalse();
    List<Alert> findByProduct(Product product);
    
    @Query("SELECT a FROM Alert a WHERE a.product = :product AND a.type = :type AND a.resolved = false")
    List<Alert> findByProductAndTypeAndResolvedFalse(@Param("product") Product product, @Param("type") Alert.AlertType type);
    
    @Query("SELECT a FROM Alert a WHERE a.product = :product AND a.batchNumber = :batchNumber AND a.type = :type AND a.resolved = false")
    List<Alert> findByProductAndBatchNumberAndTypeAndResolvedFalse(
        @Param("product") Product product, 
        @Param("batchNumber") String batchNumber, 
        @Param("type") Alert.AlertType type);
}