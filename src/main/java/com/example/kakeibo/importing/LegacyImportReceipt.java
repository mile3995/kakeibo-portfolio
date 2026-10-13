package com.example.kakeibo.importing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.example.kakeibo.account.Account;

@Entity
@Table(name = "legacy_import_receipts", uniqueConstraints = @UniqueConstraint(
        name = "uk_legacy_import_owner_type_hash",
        columnNames = { "owner_id", "data_type", "file_hash" }))
public class LegacyImportReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Account owner;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    protected LegacyImportReceipt() { }

    public LegacyImportReceipt(Account owner, String dataType, String fileHash) {
        this.owner = owner;
        this.dataType = dataType;
        this.fileHash = fileHash;
    }
}
