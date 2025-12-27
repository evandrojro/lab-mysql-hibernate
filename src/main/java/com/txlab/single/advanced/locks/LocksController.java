package com.txlab.single.advanced.locks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/single/advanced/locks")
@Profile("single-mysql")
public class LocksController {

    private static final Logger log = LoggerFactory.getLogger(LocksController.class);

    private final WalletRepository walletRepository;

    public LocksController(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @PostMapping("/hold-for-update")
    @Transactional
    public ResponseEntity<String> holdForUpdate(@RequestParam(defaultValue = "15000") long sleepMs) throws InterruptedException {
        log.info("[LOCK] Tentando adquirir lock PESSIMISTIC_WRITE no wallet 1");
        Wallet wallet = walletRepository.findWithLockingById(1L)
                .orElseThrow(() -> new IllegalStateException("Wallet 1 não encontrada"));
        log.info("[LOCK] Lock adquirido. Dormindo {} ms", sleepMs);
        Thread.sleep(sleepMs);
        wallet.setBalance(wallet.getBalance().add(BigDecimal.ONE));
        log.info("[LOCK] Balance incrementado, commit irá liberar o lock");
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/try-update")
    @Transactional
    public ResponseEntity<String> tryUpdate() {
        Instant start = Instant.now();
        log.info("[TRY] Tentando atualizar wallet 1 (pode bloquear)");
        Wallet wallet = walletRepository.findWithLockingById(1L)
                .orElseThrow(() -> new IllegalStateException("Wallet 1 não encontrada"));
        log.info("[TRY] Lock obtido após {} ms", Duration.between(start, Instant.now()).toMillis());
        wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(10)));
        log.info("[TRY] Atualização concluída, commit liberará lock");
        return ResponseEntity.ok("updated after " + Duration.between(start, Instant.now()).toMillis() + " ms");
    }

    @PostMapping("/deadlock/start-a")
    @Transactional
    public ResponseEntity<String> deadlockA() throws InterruptedException {
        try {
            log.info("[DEADLOCK-A] Lockando wallet 1");
            Wallet w1 = walletRepository.findWithLockingById(1L).orElseGet(() -> walletRepository.save(new Wallet(1L, "demo-a", BigDecimal.valueOf(50))));
            Thread.sleep(5000);
            log.info("[DEADLOCK-A] Tentando lock em wallet 2");
            Wallet w2 = walletRepository.findWithLockingById(2L).orElseGet(() -> walletRepository.save(new Wallet(2L, "demo-b", BigDecimal.valueOf(60))));
            w1.setBalance(w1.getBalance().add(BigDecimal.valueOf(5)));
            w2.setBalance(w2.getBalance().add(BigDecimal.valueOf(5)));
            log.info("[DEADLOCK-A] Atualizações prontas, commit tentará concluir");
            return ResponseEntity.ok("deadlock-a finished");
        } catch (RuntimeException ex) {
            log.error("[DEADLOCK-A] Deadlock ou erro detectado: {}", ex.getMessage(), ex);
            return ResponseEntity.status(409).body("deadlock detectado no fluxo A: " + ex.getMessage());
        }
    }

    @PostMapping("/deadlock/start-b")
    @Transactional
    public ResponseEntity<String> deadlockB() throws InterruptedException {
        log.info("[DEADLOCK-B] Lockando wallet 2");
        Wallet w2 = walletRepository.findWithLockingById(2L).orElseGet(() -> walletRepository.save(new Wallet(2L, "demo-b", BigDecimal.valueOf(60))));
        Thread.sleep(5000);
        log.info("[DEADLOCK-B] Tentando lock em wallet 1");
        try {
            Wallet w1 = walletRepository.findWithLockingById(1L).orElseGet(() -> walletRepository.save(new Wallet(1L, "demo-a", BigDecimal.valueOf(50))));
            w1.setBalance(w1.getBalance().add(BigDecimal.valueOf(5)));
            w2.setBalance(w2.getBalance().add(BigDecimal.valueOf(5)));
            log.info("[DEADLOCK-B] Atualizações prontas, commit tentará concluir");
            return ResponseEntity.ok("deadlock-b finished");
        } catch (RuntimeException ex) {
            log.error("[DEADLOCK-B] Deadlock ou erro detectado: {}", ex.getMessage(), ex);
            return ResponseEntity.status(409).body("deadlock detectado no fluxo B: " + ex.getMessage());
        }
    }
}
