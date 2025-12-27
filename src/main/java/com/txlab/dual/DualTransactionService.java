package com.txlab.dual;

import com.txlab.api.dto.DualOperationResponse;
import com.txlab.audit.AuditLog;
import com.txlab.audit.AuditLogRepository;
import com.txlab.core.user.User;
import com.txlab.core.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Service
@Profile("dual-mysql")
public class DualTransactionService {

    private static final Logger log = LoggerFactory.getLogger(DualTransactionService.class);

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final TransactionTemplate coreTxTemplate;
    private final TransactionTemplate auditTxTemplate;

    public DualTransactionService(UserRepository userRepository,
                                  AuditLogRepository auditLogRepository,
                                  @Qualifier("coreTransactionTemplate") TransactionTemplate coreTxTemplate,
                                  @Qualifier("auditTransactionTemplate") TransactionTemplate auditTxTemplate) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.coreTxTemplate = coreTxTemplate;
        this.auditTxTemplate = auditTxTemplate;
    }

    public DualOperationResponse createUserWithAudit(String name, boolean failAudit) {
        log.info("DUAL profile: iniciando transação lógica (core + audit) sem XA");

        Long userId = coreTxTemplate.execute(status -> {
            log.info("Abrindo transação CORE");
            User saved = userRepository.save(new User(name));
            log.info("Preparando commit CORE para usuário {}", saved.getId());
            return saved.getId();
        });

        log.info("Commit físico CORE executado para usuário {}", userId);

        Long auditId = null;
        try {
            auditId = auditTxTemplate.execute(status -> {
                log.info("Abrindo transação AUDIT para usuário {}", userId);
                if (failAudit) {
                    log.error("Simulando falha antes do commit AUDIT");
                    throw new RuntimeException("Falha simulada ao gravar audit");
                }
                AuditLog auditLog = auditLogRepository.save(new AuditLog("USER_CREATED:" + userId, Instant.now()));
                log.info("Audit {} gravado, commit físico ocorrerá", auditLog.getId());
                return auditLog.getId();
            });
        } catch (RuntimeException ex) {
            log.error("Audit falhou após commit do core. Sistema ficou inconsistente para usuário {}", userId, ex);
            throw ex;
        }

        log.info("Transação lógica concluída com sucesso para usuário {} e audit {}", userId, auditId);
        return new DualOperationResponse(userId, auditId, "core commitado, audit commitado");
    }
}
