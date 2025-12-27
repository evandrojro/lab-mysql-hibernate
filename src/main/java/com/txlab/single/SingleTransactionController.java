package com.txlab.single;

import com.txlab.api.dto.CreateUserRequest;
import com.txlab.api.dto.UserInvoiceResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/single")
@Profile("single-mysql")
public class SingleTransactionController {

    private static final Logger log = LoggerFactory.getLogger(SingleTransactionController.class);

    private final SingleTransactionService singleTransactionService;

    public SingleTransactionController(SingleTransactionService singleTransactionService) {
        this.singleTransactionService = singleTransactionService;
    }

    @PostMapping("/users-with-invoice")
    public ResponseEntity<UserInvoiceResponse> createUserWithInvoice(@Valid @RequestBody CreateUserRequest request,
                                                                     @RequestParam(defaultValue = "false") boolean fail) {
        log.info("Recebida requisição para /single/users-with-invoice com fail={}", fail);
        UserInvoiceResponse response = singleTransactionService.createUserWithInvoice(request.name(), fail);
        return ResponseEntity.ok(response);
    }
}
