package com.txlab.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.txlab.api.dto.OutboxEnqueueResponse;
import com.txlab.core.outbox.OutboxEvent;
import com.txlab.core.outbox.OutboxEventRepository;
import com.txlab.core.user.User;
import com.txlab.core.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Profile("dual-mysql")
public class OutboxService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSED = "PROCESSED";

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(UserRepository userRepository,
                         OutboxEventRepository outboxEventRepository,
                         ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "coreTransactionManager")
    public OutboxEnqueueResponse createUserWithOutbox(String name) {
        log.info("Outbox: iniciando transação CORE única (usuário + evento)");
        User user = userRepository.save(new User(name));
        String payload = buildPayload(user);
        OutboxEvent event = outboxEventRepository.save(
                new OutboxEvent("USER", user.getId(), payload, STATUS_PENDING, Instant.now()));
        log.info("Outbox: evento {} criado e aguardando despacho", event.getId());
        return new OutboxEnqueueResponse(user.getId(), event.getId(),
                "Usuário criado e evento pendente para auditoria");
    }

    private String buildPayload(User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", user.getId());
        body.put("name", user.getName());
        body.put("action", "USER_CREATED");
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível serializar payload do outbox", e);
        }
    }
}
