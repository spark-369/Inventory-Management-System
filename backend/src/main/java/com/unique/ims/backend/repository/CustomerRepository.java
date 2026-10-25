package com.unique.ims.backend.repository;

import com.unique.ims.backend.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByName(String name);
    boolean existsByEmail(String email);
}