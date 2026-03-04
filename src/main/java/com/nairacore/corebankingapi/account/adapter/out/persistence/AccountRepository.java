// src/main/java/com/nairacore/corebankingapi/account/adapter/out/persistence/AccountRepository.java
package com.nairacore.corebankingapi.account.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository gives us save(), findById(), etc., for free
public interface AccountRepository extends JpaRepository<AccountJpaEntity, Long> {
    // Spring automatically writes the SQL to find an entity by the accountNumber
    // field!
    Optional<AccountJpaEntity> findByAccountNumber(String accountNumber);
}