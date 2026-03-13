// user-module/src/main/java/com/coreledger/user/domain/model/UserStatus.java
package com.coreledger.user.domain.model;

public enum UserStatus {
    ACTIVE,
    IN_ACTIVE,
    SUSPEND,
    FLAGGED, // restricted due to fraud suspicion, regulatory issues, or compliance flags.
    PENDING_VERIFICATION,
    CLOSED
}
