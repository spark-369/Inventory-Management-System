package com.unique.ims.backend.repository;

import com.unique.ims.backend.model.JobMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobMessageLogRepository extends JpaRepository<JobMessageLog, Long> {

    Optional<JobMessageLog> findByCorrelationId(String correlationId);

    List<JobMessageLog> findTop50ByOrderByCreatedAtDesc();

    List<JobMessageLog> findByMessageTypeOrderByCreatedAtDesc(String messageType);
}
