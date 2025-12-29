package com.txlab.single.advanced.isolation;

import com.txlab.core.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/single/advanced/isolation")
@Profile("single-mysql")
public class IsolationController {

    private static final Logger log = LoggerFactory.getLogger(IsolationController.class);

    private final UserRepository userRepository;

    public IsolationController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/repeatable-read")
    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public ResponseEntity<Map<String, Object>> repeatableRead(@RequestParam(defaultValue = "15000") long sleepMs) throws InterruptedException {
        return runIsolationTest("REPEATABLE_READ", sleepMs);
    }

    @GetMapping("/read-committed")
    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ResponseEntity<Map<String, Object>> readCommitted(@RequestParam(defaultValue = "15000") long sleepMs) throws InterruptedException {
        return runIsolationTest("READ_COMMITTED", sleepMs);
    }

    private ResponseEntity<Map<String, Object>> runIsolationTest(String isolation, long sleepMs) throws InterruptedException {
        log.info("[{}] Leitura inicial de usuários", isolation);
        long firstCount = userRepository.count();
        log.info("[{}] firstCount={} — dormindo {} ms", isolation, firstCount, sleepMs);
        Thread.sleep(sleepMs);
        long secondCount = userRepository.count();
        log.info("[{}] secondCount={}", isolation, secondCount);
        return ResponseEntity.ok(Map.of(
                "firstCount", firstCount,
                "secondCount", secondCount,
                "isolation", isolation
        ));
    }
}
