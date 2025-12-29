package com.txlab.single.advanced.propagation;

import com.txlab.api.dto.NestedInvoiceRequest;
import com.txlab.api.dto.PropagationRequiresNewRequest;
import com.txlab.api.dto.UserInvoiceResponse;
import com.txlab.core.user.User;
import com.txlab.core.user.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/single/advanced/propagation")
@Profile("single-mysql")
public class PropagationController {

    private static final Logger log = LoggerFactory.getLogger(PropagationController.class);

    private final UserRepository userRepository;
    private final BatchItemService batchItemService;
    private final InvoiceNestedService invoiceNestedService;
    private final AtomicLong batchSequence = new AtomicLong(1L);

    public PropagationController(UserRepository userRepository,
                                 BatchItemService batchItemService,
                                 InvoiceNestedService invoiceNestedService) {
        this.userRepository = userRepository;
        this.batchItemService = batchItemService;
        this.invoiceNestedService = invoiceNestedService;
    }

    @PostMapping("/requires-new")
    @Transactional
    public ResponseEntity<String> requiresNew(@Valid @RequestBody PropagationRequiresNewRequest request) {
        long batchId = batchSequence.getAndIncrement();
        log.info("[OUTER] Abrindo transação REQUIRED (core) para usuário {} em batch {}", request.name(), batchId);
        User user = userRepository.save(new User(request.name()));
        log.info("[OUTER] Usuário {} criado (REQUIRED)", user.getId());

        List<String> items = request.items();
        for (String item : items) {
            try {
                batchItemService.processItemRequiresNew(batchId, user.getName(), item, request.failOnItem());
            } catch (RuntimeException ex) {
                log.error("[OUTER] Item {} falhou dentro do REQUIRES_NEW: {}", item, ex.getMessage());
            }
        }

        if (Boolean.TRUE.equals(request.failOuter())) {
            log.error("[OUTER] Simulando falha após loop para forçar rollback do usuário {}", user.getId());
            throw new RuntimeException("Falha simulada no outer");
        }

        log.info("[OUTER] Concluindo transação REQUIRED para usuário {}", user.getId());
        return ResponseEntity.ok("Batch " + batchId + " concluído para usuário " + user.getId());
    }

    @PostMapping("/nested-savepoint")
    @Transactional
    public ResponseEntity<UserInvoiceResponse> nested(@Valid @RequestBody NestedInvoiceRequest request) {
        log.info("[OUTER] Abrindo transação REQUIRED (core) para usuário {} com nested invoice", request.name());
        User user = userRepository.save(new User(request.name()));
        log.info("[OUTER] Usuário {} criado", user.getId());
        UserInvoiceResponse response;
        try {
            var invoice = invoiceNestedService.createInvoiceNested(user.getId(), request.failInner());
            response = new UserInvoiceResponse(user.getId(), invoice.getId(), "user commitado, invoice commitado no nested");
        } catch (RuntimeException ex) {
            log.error("[OUTER] Falha no nested: {}. Outer deve seguir e commitar o usuário {}", ex.getMessage(), user.getId());
            response = new UserInvoiceResponse(user.getId(), null, "user commitado; nested rollback via savepoint");
        }

        log.info("[OUTER] Finalizando transação REQUIRED para usuário {} (nested test)", user.getId());
        return ResponseEntity.ok(response);
    }
}
