// auth-module/src/main/java/com/coreledger/authentication/application/service/PasswordLoginService.java
package com.coreledger.authentication.application.service;

import java.time.Instant;

import com.coreledger.authentication.application.ports.in.PasswordLoginUseCase;
import com.coreledger.authentication.application.ports.out.LoadAuthUserPort;
import com.coreledger.authentication.application.ports.out.TokenGeneratorPort;
import com.coreledger.authentication.domain.events.AuthenticationFailedEvent;
import com.coreledger.authentication.domain.exceptions.InvalidCredentialsException;
import com.coreledger.authentication.domain.model.AccessToken;
import com.coreledger.authentication.domain.model.AuthFailureReason;
import com.coreledger.authentication.domain.model.AuthUser;
import com.coreledger.authentication.domain.model.RefreshToken;
import com.coreledger.authentication.domain.model.TokenPair;
import com.coreledger.shared.DomainEventPublisher;
import com.coreledger.shared.domain.DomainEvent;

public class PasswordLoginService implements PasswordLoginUseCase {

    private final LoadAuthUserPort loadAuthUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final DomainEventPublisher eventPublisher;

    public PasswordLoginService(
            LoadAuthUserPort loadAuthUserPort,
            TokenGeneratorPort tokenGeneratorPort,
            DomainEventPublisher eventPublisher) {
        this.loadAuthUserPort = loadAuthUserPort;
        this.tokenGeneratorPort = tokenGeneratorPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public TokenPair login(Command command) {
        AuthUser user = loadAuthUserPort.loadByEmail(command.userEmailAddress())
                .orElseThrow(() -> {
                    DomainEvent event = new AuthenticationFailedEvent(
                            command.userEmailAddress(),
                            AuthFailureReason.USER_NOT_FOUND,
                            Instant.now(),
                            command.clientIp(),
                            command.userAgent());

                    eventPublisher.publish(event, null); // TODO:: will add the publisher in place of null when done

                    return new InvalidCredentialsException();
                });

        if (!user.verifyPassword(command.password())) {
            DomainEvent event = new AuthenticationFailedEvent(
                    command.userEmailAddress(),
                    AuthFailureReason.INVALID_CREDENTIALS,
                    Instant.now(),
                    command.clientIp(),
                    command.userAgent());

            eventPublisher.publish(event, null); // TODO:: will add the publisher in place of null when done

            throw new InvalidCredentialsException();
        }

        AccessToken accessToken = tokenGeneratorPort.generateAccessToken(user.getUserId(), null);
        TokenGeneratorPort.GeneratedRefreshToken generatedRefreshToken = tokenGeneratorPort.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.createNew(
                generatedRefreshToken.tokenHash(),
                user.getUserId(),
                Instant.now(),
                null);

        // keep the raw token to return to the client
        String rawRefreshTokenString = generatedRefreshToken.rawToken().value();
        return TokenPair.create(accessToken, refreshToken, rawRefreshTokenString);
    }
}
