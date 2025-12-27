package com.txlab.single.advanced.propagation;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "local_audit", catalog = "txlab_core")
public class LocalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    protected LocalAudit() {
    }

    public LocalAudit(String message) {
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
