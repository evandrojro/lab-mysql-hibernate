package com.txlab.outbox;

import com.txlab.audit.AuditLog;
import com.txlab.audit.AuditLogRepository;
import com.txlab.core.outbox.OutboxEvent;
import com.txlab.core.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Component
@Profile("dual-mysql")
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);

    private final OutboxEventRepository outboxEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final TransactionTemplate coreTxTemplate;
    private final TransactionTemplate auditTxTemplate;
    private final int pageSize;

    public OutboxWorker(OutboxEventRepository outboxEventRepository,
                        AuditLogRepository auditLogRepository,
                        @Qualifier("coreTransactionTemplate") TransactionTemplate coreTxTemplate,
                        @Qualifier("auditTransactionTemplate") TransactionTemplate auditTxTemplate,
                        @Value("${app.outbox.page-size:25}") int pageSize) {
        this.outboxEventRepository = outboxEventRepository;
        this.auditLogRepository = auditLogRepository;
        this.coreTxTemplate = coreTxTemplate;
        this.auditTxTemplate = auditTxTemplate;
        this.pageSize = pageSize;
    }

    @Scheduled(fixedDelay = 5000)
    public void dispatch() {
        List<OutboxEvent> pending = outboxEventRepository.findTop25ByStatusOrderByIdAsc(OutboxService.STATUS_PENDING)
                .stream()
                .limit(pageSize)
                .toList();

        if (pending.isEmpty()) {
            return;
        }

        log.info("Outbox worker iniciado: {} evento(s) pendentes", pending.size());
        pending.forEach(this::processEventSafely);
    }

    private void processEventSafely(OutboxEvent event) {
        try {
            Long auditId = auditTxTemplate.execute(status -> {
                log.info("Gravando audit para evento {} (aggregate {}:{})", event.getId(),
                        event.getAggregateType(), event.getAggregateId());
                AuditLog auditLog = auditLogRepository.save(
                        new AuditLog("OUTBOX:" + event.getAggregateId(), Instant.now()));
                log.info("Audit {} commitado para evento {}", auditLog.getId(), event.getId());
                return auditLog.getId();
            });

            coreTxTemplate.executeWithoutResult(status -> {
                event.setStatus(OutboxService.STATUS_PROCESSED);
                outboxEventRepository.save(event);
                log.info("Evento {} marcado como PROCESSED após audit {}", event.getId(), auditId);
            });
        } catch (Exception ex) {
            log.error("Falha ao processar evento {}. Mantendo como PENDING", event.getId(), ex);
        }
    }
}
