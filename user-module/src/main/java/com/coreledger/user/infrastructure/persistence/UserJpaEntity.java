package com.coreledger.user.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;

import com.coreledger.user.domain.model.UserRole;
import com.coreledger.user.domain.model.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserJpaEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "firstName", nullable = false)
    private String firstName;

    @Column(name = "lastName", nullable = false)
    private String lastName;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "dateOfBirth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "userRole", nullable = false, length = 30)
    private UserRole userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "userStatus", nullable = false, length = 30)
    private UserStatus userStatus;
}
