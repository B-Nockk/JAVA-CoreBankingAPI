// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/TokenReuseDetectedException.java
package com.coreledger.authentication.domain.exceptions;

import java.util.UUID;
import com.coreledger.shared.domain.UserId;
import lombok.Getter;

@Getter
public class TokenReuseDetectedException extends RuntimeException {

    private final UUID tokenId;
    private final UserId userId;

    public TokenReuseDetectedException(UUID tokenId, UserId userId) {
        // String.format is cleaner and prevents missing space bugs
        super(String.format("Security Alert: Token reuse detected for User: %s, TokenId: %s", userId, tokenId));
        this.tokenId = tokenId;
        this.userId = userId;
    }
}