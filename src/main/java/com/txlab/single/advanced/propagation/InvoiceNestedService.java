package com.txlab.single.advanced.propagation;

import com.txlab.billing.invoice.Invoice;
import com.txlab.billing.invoice.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceNestedService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceNestedService.class);

    private final InvoiceRepository invoiceRepository;

    public InvoiceNestedService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(propagation = Propagation.NESTED)
    public Invoice createInvoiceNested(Long userId, boolean failInner) {
        log.info("[NESTED] Iniciando bloco nested para invoice do usuário {}", userId);
        Invoice invoice = invoiceRepository.save(new Invoice(userId, "CREATED"));
        log.info("[NESTED] Invoice {} criada, savepoint ativo", invoice.getId());
        if (failInner) {
            log.error("[NESTED] Simulando falha dentro do nested, savepoint deve ser revertido");
            throw new RuntimeException("Falha simulada no nested");
        }
        log.info("[NESTED] Concluindo nested, savepoint será confirmado");
        return invoice;
    }
}
