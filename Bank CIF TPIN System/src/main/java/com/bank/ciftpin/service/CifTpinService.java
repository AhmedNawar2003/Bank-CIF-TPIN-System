package com.bank.ciftpin.service;

import com.bank.ciftpin.dto.*;
import com.bank.ciftpin.enums.CifStatus;
import com.bank.ciftpin.exception.*;
import com.bank.ciftpin.model.CifAccount;
import com.bank.ciftpin.repository.CifAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CifTpinService {

    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final CifAccountRepository cifAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Register a new CIF account.
     */
    @Transactional
    public CifAccountResponse register(RegisterRequest request) {
        log.info("Registering new CIF: {}", request.getCifNumber());

        if (cifAccountRepository.existsByCifNumber(request.getCifNumber())) {
            log.warn("CIF already exists: {}", request.getCifNumber());
            throw new CifAlreadyExistsException("CIF number already registered: " + request.getCifNumber());
        }

        if (cifAccountRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already registered: {}", request.getEmail());
            throw new CifAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        CifAccount account = CifAccount.builder()
                .cifNumber(request.getCifNumber())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .status(CifStatus.PENDING_TPIN)
                .failedAttempts(0)
                .build();

        CifAccount savedAccount = cifAccountRepository.save(account);
        log.info("CIF registered successfully: {}", savedAccount.getCifNumber());
        return CifAccountResponse.from(savedAccount);
    }

    /**
     * Set TPIN for a CIF that is in PENDING_TPIN status.
     */
    @Transactional
    public CifAccountResponse setTpin(SetTpinRequest request) {
        log.info("Setting TPIN for CIF: {}", request.getCifNumber());

        CifAccount account = getCifOrThrow(request.getCifNumber());

        if (account.getStatus() == CifStatus.BLOCKED) {
            log.warn("Attempted to set TPIN on blocked CIF: {}", request.getCifNumber());
            throw new CifBlockedException(request.getCifNumber());
        }

        if (account.getStatus() == CifStatus.ACTIVE) {
            log.warn("TPIN already set for CIF: {}", request.getCifNumber());
            throw new IllegalStateException("TPIN is already set for this CIF. Use Reset TPIN to change it.");
        }

        account.setTpinHash(passwordEncoder.encode(request.getTpin()));
        account.setStatus(CifStatus.ACTIVE);
        account.setFailedAttempts(0);

        CifAccount updatedAccount = cifAccountRepository.save(account);
        log.info("TPIN set successfully for CIF: {}", updatedAccount.getCifNumber());
        return CifAccountResponse.from(updatedAccount);
    }

    /**
     * Authenticate a CIF using its TPIN.
     * Three consecutive failures will block the CIF.
     */

    @Transactional(noRollbackFor = {
            InvalidTpinException.class,
            CifBlockedException.class
    })
    public CifAccountResponse authenticate(AuthRequest request) {
        log.info("Authentication attempt for CIF: {}", request.getCifNumber());

        CifAccount account = getCifOrThrow(request.getCifNumber());

        if (account.getStatus() == CifStatus.BLOCKED) {
            log.warn("Authentication attempt on blocked CIF: {}", request.getCifNumber());
            throw new CifBlockedException(request.getCifNumber());
        }

        if (account.getStatus() == CifStatus.PENDING_TPIN || account.getTpinHash() == null) {
            log.warn("Authentication attempt on CIF with no TPIN set: {}", request.getCifNumber());
            throw new TpinNotSetException(request.getCifNumber());
        }

        boolean tpinMatches = passwordEncoder.matches(request.getTpin(), account.getTpinHash());

        if (!tpinMatches) {
            int newFailedAttempts = account.getFailedAttempts() + 1;
            account.setFailedAttempts(newFailedAttempts);

            if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {
                account.setStatus(CifStatus.BLOCKED);
                cifAccountRepository.save(account);
                log.warn("CIF blocked after {} failed attempts: {}", MAX_FAILED_ATTEMPTS, request.getCifNumber());
                throw new CifBlockedException(request.getCifNumber());
            }

            cifAccountRepository.save(account);
            int remaining = MAX_FAILED_ATTEMPTS - newFailedAttempts;
            log.warn("Failed authentication for CIF: {}. Failed attempts: {}. Remaining: {}",
                    request.getCifNumber(), newFailedAttempts, remaining);
            throw new InvalidTpinException(remaining);
        }

        // Success — reset failed attempts
        account.setFailedAttempts(0);
        CifAccount updatedAccount = cifAccountRepository.save(account);
        log.info("Authentication successful for CIF: {}", request.getCifNumber());
        return CifAccountResponse.from(updatedAccount);
    }

    /**
     * Reset TPIN: unblocks the CIF and sets a new TPIN.
     */
    @Transactional
    public CifAccountResponse resetTpin(ResetTpinRequest request) {
        log.info("TPIN reset requested for CIF: {}", request.getCifNumber());

        CifAccount account = getCifOrThrow(request.getCifNumber());

        if (account.getStatus() == CifStatus.PENDING_TPIN) {
            log.warn("Reset TPIN on PENDING_TPIN CIF (no previous TPIN): {}", request.getCifNumber());
            throw new IllegalStateException("No TPIN has been set previously. Please use the Set TPIN API.");
        }

        account.setTpinHash(passwordEncoder.encode(request.getNewTpin()));
        account.setStatus(CifStatus.ACTIVE);
        account.setFailedAttempts(0);

        CifAccount updatedAccount = cifAccountRepository.save(account);
        log.info("TPIN reset successfully for CIF: {}. Previous status was: {}",
                request.getCifNumber(), account.getStatus());
        return CifAccountResponse.from(updatedAccount);
    }

    private CifAccount getCifOrThrow(String cifNumber) {
        return cifAccountRepository.findByCifNumber(cifNumber)
                .orElseThrow(() -> new CifNotFoundException(cifNumber));
    }
}