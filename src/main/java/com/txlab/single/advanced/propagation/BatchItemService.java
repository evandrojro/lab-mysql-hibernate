package com.txlab.single.advanced.propagation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchItemService {

    private static final Logger log = LoggerFactory.getLogger(BatchItemService.class);

    private final BatchItemRepository batchItemRepository;
    private final LocalAuditRepository localAuditRepository;

    public BatchItemService(BatchItemRepository batchItemRepository, LocalAuditRepository localAuditRepository) {
        this.batchItemRepository = batchItemRepository;
        this.localAuditRepository = localAuditRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processItemRequiresNew(Long batchId, String userName, String itemKey, String failOnItem) {
        log.info("[REQUIRES_NEW] Iniciando processamento de item {} (batch {})", itemKey, batchId);
        BatchItem item = batchItemRepository.save(new BatchItem(batchId, itemKey, "PROCESSED"));
        log.info("[REQUIRES_NEW] Item {} salvo com status PROCESSED (id={})", itemKey, item.getId());
        localAuditRepository.save(new LocalAudit("processed item " + itemKey + " for user " + userName));
        log.info("[REQUIRES_NEW] Audit local registrado para item {}", itemKey);

        if (failOnItem != null && failOnItem.equals(itemKey)) {
            log.error("[REQUIRES_NEW] Simulando falha para item {} — rollback apenas deste bloco", itemKey);
            throw new RuntimeException("Falha simulada no item " + itemKey);
        }

        log.info("[REQUIRES_NEW] Commit será executado para item {}", itemKey);
    }
}
