// src/main/java/com/nairacore/corebankingapi/account/adapter/out/persistence/AccountJpaEntity.java
package com.nairacore.corebankingapi.account.adapter.out.persistence;

import java.math.BigDecimal;
import jakarta.persistence.Version;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity // Tells Spring to make a DB table
@Table(name = "accounts")
@Data // Lombok: auto-generates getters, setters, toString
@NoArgsConstructor // Required by JPA
@AllArgsConstructor
public class AccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private String status; // We store the Enum as a String in the DB

    @Version
    private Integer version; // Hibernate handles this automatically!
}