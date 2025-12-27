package com.txlab.single.advanced.propagation;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "batch_item", catalog = "txlab_core")
public class BatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "item_key", nullable = false)
    private String itemKey;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    protected BatchItem() {
    }

    public BatchItem(Long batchId, String itemKey, String status) {
        this.batchId = batchId;
        this.itemKey = itemKey;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public String getItemKey() {
        return itemKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
