package com.example.kakeibo.importing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyImportReceiptRepository extends JpaRepository<LegacyImportReceipt, Long> {
    boolean existsByOwner_IdAndDataTypeAndFileHash(Long ownerId, String dataType, String fileHash);
}
