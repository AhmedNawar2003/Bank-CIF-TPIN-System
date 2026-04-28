package com.bank.ciftpin.repository;

import com.bank.ciftpin.model.CifAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CifAccountRepository extends JpaRepository<CifAccount, Long> {

    Optional<CifAccount> findByCifNumber(String cifNumber);

    boolean existsByCifNumber(String cifNumber);

    boolean existsByEmail(String email);
}