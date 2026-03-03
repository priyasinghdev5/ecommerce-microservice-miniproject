package com.ecom.batch.repository;

import com.ecom.batch.entity.ImportJob;
import com.ecom.batch.entity.ImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {
    List<ImportJob> findByStatus(ImportStatus status);
}
