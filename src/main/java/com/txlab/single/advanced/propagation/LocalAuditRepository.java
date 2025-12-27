package com.txlab.single.advanced.propagation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalAuditRepository extends JpaRepository<LocalAudit, Long> {
}
