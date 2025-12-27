package com.txlab.outbox;

import com.txlab.api.dto.CreateUserRequest;
import com.txlab.api.dto.OutboxEnqueueResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/outbox")
@Profile("dual-mysql")
public class OutboxController {

    private static final Logger log = LoggerFactory.getLogger(OutboxController.class);
    private final OutboxService outboxService;

    public OutboxController(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @PostMapping("/users")
    public ResponseEntity<OutboxEnqueueResponse> createUserWithOutbox(@Valid @RequestBody CreateUserRequest request) {
        log.info("Recebida requisição para /outbox/users");
        OutboxEnqueueResponse response = outboxService.createUserWithOutbox(request.name());
        return ResponseEntity.ok(response);
    }
}
