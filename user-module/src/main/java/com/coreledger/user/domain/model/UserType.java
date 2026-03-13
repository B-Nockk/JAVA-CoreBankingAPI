package com.coreledger.user.domain.model;

public enum UserType {
    CUSTOMER, // can own accounts, make transfers
    EMPLOYEE, // bank staff, no personal accounts in this system
    ADMIN, // system administration
    Organization, // TODO:: organization - maybe have it's own module as it can contain multiple
                  // users with account access?
}
