package com.txlab.single;

import com.txlab.api.dto.UserInvoiceResponse;
import com.txlab.billing.invoice.Invoice;
import com.txlab.billing.invoice.InvoiceRepository;
import com.txlab.core.user.User;
import com.txlab.core.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("single-mysql")
public class SingleTransactionService {

    private static final Logger log = LoggerFactory.getLogger(SingleTransactionService.class);

    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    public SingleTransactionService(UserRepository userRepository, InvoiceRepository invoiceRepository) {
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public UserInvoiceResponse createUserWithInvoice(String name, boolean fail) {
        log.info("SINGLE profile: iniciando transação física única para core + billing");
        User user = userRepository.save(new User(name));
        log.info("Usuário {} criado (core)", user.getId());

        if (fail) {
            log.error("Simulando falha antes de criar invoice para provocar rollback completo");
            throw new RuntimeException("Simulação de falha no meio da transação");
        }

        Invoice invoice = invoiceRepository.save(new Invoice(user.getId(), "CREATED"));
        log.info("Invoice {} criada (billing)", invoice.getId());
        log.info("Transação será confirmada para usuário {} e invoice {}", user.getId(), invoice.getId());

        return new UserInvoiceResponse(user.getId(), invoice.getId(), "commit efetuado nas duas tabelas");
    }
}
