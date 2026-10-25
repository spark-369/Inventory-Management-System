package com.unique.ims.backend.repository;

import com.unique.ims.backend.model.Return;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReturnRepository extends JpaRepository<Return, Long> {
    Optional<Return> findByReturnNumber(String returnNumber);
}