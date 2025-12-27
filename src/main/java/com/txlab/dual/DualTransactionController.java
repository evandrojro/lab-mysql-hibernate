package com.txlab.dual;

import com.txlab.api.dto.CreateUserRequest;
import com.txlab.api.dto.DualOperationResponse;
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
@RequestMapping("/dual")
@Profile("dual-mysql")
public class DualTransactionController {

    private static final Logger log = LoggerFactory.getLogger(DualTransactionController.class);

    private final DualTransactionService dualTransactionService;

    public DualTransactionController(DualTransactionService dualTransactionService) {
        this.dualTransactionService = dualTransactionService;
    }

    @PostMapping("/users-with-audit")
    public ResponseEntity<DualOperationResponse> createUserWithAudit(@Valid @RequestBody CreateUserRequest request,
                                                                     @RequestParam(defaultValue = "false") boolean fail) {
        log.info("Recebida requisição para /dual/users-with-audit com fail={}", fail);
        DualOperationResponse response = dualTransactionService.createUserWithAudit(request.name(), fail);
        return ResponseEntity.ok(response);
    }
}
