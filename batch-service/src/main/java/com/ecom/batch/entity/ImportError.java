package com.ecom.batch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_errors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportError {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID jobId;

    private int rowNumber;

    @Column(columnDefinition = "TEXT")
    private String rawData;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String errorMsg;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
