package com.coreledger.user.domain.model;

public enum UserRole {
    CUSTOMER, // can own accounts, make transfers
    EMPLOYEE, // bank staff, no personal accounts in this system
    ADMIN, // system administration
    ORGANIZATION // TODO: may move to its own module
}
