package com.ecom.batch.repository;

import com.ecom.batch.entity.ImportError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ImportErrorRepository extends JpaRepository<ImportError, UUID> {
    List<ImportError> findByJobId(UUID jobId);
}
