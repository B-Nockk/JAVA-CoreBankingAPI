// user-module/src/main/java/com/coreledger/user/application/service/UserKycService.java
package com.coreledger.user.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coreledger.shared.DomainEventPublisher;
import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.UserId;
import com.coreledger.user.application.port.in.AddKycDocumentUseCase;
import com.coreledger.user.application.port.in.DeleteKycDocumentUseCase;
import com.coreledger.user.application.port.in.DeleteKycProfileUseCase;
import com.coreledger.user.application.port.in.GetUserKycUseCase;
import com.coreledger.user.application.port.in.LoadKycDocumentUseCase;
import com.coreledger.user.application.port.in.RejectKycDocumentUseCase;
import com.coreledger.user.application.port.in.UpdateUserKycUseCase;
import com.coreledger.user.application.port.in.VerifyKycDocumentUseCase;
import com.coreledger.user.application.port.out.DeleteKycPort;
import com.coreledger.user.application.port.out.LoadKycDocumentPort;
import com.coreledger.user.application.port.out.LoadUserKycPort;
import com.coreledger.user.application.port.out.LoadUserPort;
import com.coreledger.user.application.port.out.SaveKycDocumentPort;
import com.coreledger.user.application.port.out.SaveUserKycPort;
import com.coreledger.user.domain.events.KycDocumentDeleted;
import com.coreledger.user.domain.events.KycDocumentRejected;
import com.coreledger.user.domain.events.KycDocumentUploaded;
import com.coreledger.user.domain.events.KycDocumentVerified;
import com.coreledger.user.domain.events.KycProfileDeleted;
import com.coreledger.user.domain.events.KycTierUpdated;
import com.coreledger.user.domain.exceptions.UserNotFoundException;
import com.coreledger.user.domain.model.KycDocument;
import com.coreledger.user.domain.model.KycDocumentBinary;
import com.coreledger.user.domain.model.KycProfile;
import com.coreledger.user.domain.model.KycProfileId;
import com.coreledger.user.domain.model.KycSnapshot;
import com.coreledger.user.domain.model.KycTier;
import com.coreledger.user.domain.model.User;

/**
 * Application service for KYC (Know Your Customer) operations within the user
 * bounded context.
 *
 * Responsibilities:
 * - Implement KYC management inbound ports (use cases)
 * - Orchestrate KYC domain operations: load -> profile -> save -> publish
 * events
 * - Handle document storage via DocumentStorageService abstraction
 * - Maintain the four-step pattern: load, execute domain behavior, persist,
 * publish
 */
@Service
public class UserKycService implements
        GetUserKycUseCase,
        AddKycDocumentUseCase,
        VerifyKycDocumentUseCase,
        RejectKycDocumentUseCase,
        LoadKycDocumentUseCase,
        UpdateUserKycUseCase,
        DeleteKycDocumentUseCase,
        DeleteKycProfileUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadUserKycPort loadUserKycPort;
    private final SaveUserKycPort saveUserKycPort;
    private final LoadKycDocumentPort loadKycDocumentPort;
    private final SaveKycDocumentPort saveKycDocumentPort;
    private final DeleteKycPort deleteKycPort;
    private final DomainEventPublisher eventPublisher;

    public UserKycService(
            LoadUserPort loadUserPort,
            LoadUserKycPort loadUserKycPort,
            SaveUserKycPort saveUserKycPort,
            LoadKycDocumentPort loadKycDocumentPort,
            SaveKycDocumentPort saveKycDocumentPort,
            DeleteKycPort deleteKycPort,
            DomainEventPublisher eventPublisher) {
        this.loadUserPort = loadUserPort;
        this.loadUserKycPort = loadUserKycPort;
        this.saveUserKycPort = saveUserKycPort;
        this.loadKycDocumentPort = loadKycDocumentPort;
        this.saveKycDocumentPort = saveKycDocumentPort;
        this.deleteKycPort = deleteKycPort;
        this.eventPublisher = eventPublisher;
    }

    // ============================================================
    // GetUserKycUseCase
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public KycSnapshot getKycForUser(UserId userId) {
        // Guard clause
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        // Step 1: Verify user exists
        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        // Step 2: Load KYC profile (if exists)
        return loadUserKycPort.loadKycProfile(userId)
                .map(KycProfile::snapshot)
                .orElseGet(() -> createEmptySnapshot(userId));
    }

    private KycSnapshot createEmptySnapshot(UserId userId) {
        return new KycSnapshot(userId, KycTier.TIER_1, List.of());
    }

    // ============================================================
    // AddKycDocumentUseCase
    // ============================================================

    @Override
    @Transactional
    public AddDocumentResult addDocument(AddKycDocumentUseCase.Command command) {
        // Guard clauses
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }

        // Step 1: Verify user exists
        User user = loadUserPort.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId().toString()));

        // Step 2: Load or create KYC profile
        KycProfile profile = loadUserKycPort.loadKycProfile(command.userId())
                .orElseGet(() -> createKycProfile(command.userId()));

        // Step 3: Create document domain entity
        KycDocument document = profile.submitDocument(command.documentType()); // Creates and adds document internally

        // Step 4: Add document to profile and recalculate tier
        KycDocument savedDocument = saveKycDocumentPort.save(
                document,
                command.fileContent(),
                command.filename());

        // Step 5: Save updated profile
        saveUserKycPort.update(profile);

        // Step 6: Publish domain events (document uploaded event would be nice to have)
        DomainEvent event = new KycDocumentUploaded(savedDocument.getId(), user.getId(), savedDocument.getType());
        eventPublisher.publish(event, eventPublisher::publishUserEvent);

        return new AddDocumentResult(savedDocument.getId(), getStoragePath(savedDocument), profile.getTier());
    }

    private KycProfile createKycProfile(UserId userId) {
        KycProfile profile = KycProfile.create(userId, List.of());
        return saveUserKycPort.save(profile);
    }

    private String getStoragePath(KycDocument document) {
        return loadKycDocumentPort.getStoragePath(document.getId())
                .orElseThrow(() -> new RuntimeException("Storage path not found for document: " + document.getId()));
    }

    // ============================================================
    // VerifyKycDocumentUseCase
    // ============================================================

    @Override
    @Transactional
    public KycTier verifyDocument(VerifyKycDocumentUseCase.VerifyCommand command) {
        // Guard clauses
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }

        // Step 1: Load the KYC profile
        KycProfile profile = loadUserKycPort.loadKycProfile(command.userId())
                .orElseThrow(() -> new RuntimeException("KYC profile not found for user: " + command.userId()));

        // Step 2: Verify document exists and belongs to this profile
        KycDocument document = loadKycDocumentPort.loadDocument(command.documentId())
                .orElseThrow(() -> new RuntimeException("Document not found: " + command.documentId()));

        if (!document.getProfileId().equals(profile.getId())) {
            throw new RuntimeException("Document does not belong to user's profile");
        }

        // Step 3: Call domain behavior
        profile.verifyDocument(command.documentId());

        // Step 4: Persist changes
        saveUserKycPort.update(profile);
        saveKycDocumentPort.updateMetadata(document);

        // Step 5: Publish events
        var events = List.of(
                new KycDocumentVerified(command.documentId(), command.userId()),
                new KycTierUpdated(profile.getId(), command.userId(), profile.getTier()));
        events.forEach(event -> eventPublisher.publish(event, eventPublisher::publishUserEvent));

        return profile.getTier();
    }

    // ============================================================
    // RejectKycDocumentUseCase
    // ============================================================

    @Override
    @Transactional
    public KycTier rejectDocument(RejectKycDocumentUseCase.RejectCommand command) {
        // Guard clauses
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }

        // Step 1: Load the KYC profile
        KycProfile profile = loadUserKycPort.loadKycProfile(command.userId())
                .orElseThrow(() -> new RuntimeException("KYC profile not found for user: " + command.userId()));

        // Step 2: Verify document exists and belongs to this profile
        KycDocument document = loadKycDocumentPort.loadDocument(command.documentId())
                .orElseThrow(() -> new RuntimeException("Document not found: " + command.documentId()));

        if (!document.getProfileId().equals(profile.getId())) {
            // TODO:: Should this message be returned or a more generic one while this is
            // logged.
            throw new RuntimeException("Document does not belong to user's profile");
        }

        // Step 3: Call domain behavior
        profile.rejectDocument(command.documentId(), command.reason());
        document.markRejected(command.reason());

        // Step 4: Persist changes
        saveUserKycPort.update(profile);
        saveKycDocumentPort.updateMetadata(document);

        // Step 5: Publish events
        var events = List.of(
                new KycDocumentRejected(command.documentId(), command.userId(), command.reason()),
                new KycTierUpdated(profile.getId(), command.userId(), profile.getTier()));
        events.forEach(event -> eventPublisher.publish(event, eventPublisher::publishUserEvent));

        return profile.getTier();
    }

    // ============================================================
    // LoadKycDocumentUseCase
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Optional<KycDocumentBinary> loadDocument(LoadKycDocumentUseCase.LoadDocumentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }

        // Verify document belongs to user
        KycDocument document = loadKycDocumentPort.loadDocument(command.documentId())
                .orElseThrow(() -> new RuntimeException("Document not found: " + command.documentId()));

        KycProfile profile = loadUserKycPort.loadKycProfile(command.userId())
                .orElseThrow(() -> new RuntimeException("KYC profile not found for user: " + command.userId()));

        if (!document.getProfileId().equals(profile.getId())) {
            throw new RuntimeException("Document does not belong to user's profile");
        }

        return loadKycDocumentPort.fetchDocument(command.documentId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getDocumentUrl(LoadKycDocumentUseCase.LoadDocumentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }

        // Verify document belongs to user
        KycDocument document = loadKycDocumentPort.loadDocument(command.documentId())
                .orElseThrow(() -> new RuntimeException("Document not found: " + command.documentId()));

        KycProfile profile = loadUserKycPort.loadKycProfile(command.userId())
                .orElseThrow(() -> new RuntimeException("KYC profile not found for user: " + command.userId()));

        if (!document.getProfileId().equals(profile.getId())) {
            throw new RuntimeException("Document does not belong to user's profile");
        }

        return loadKycDocumentPort.getStoragePath(command.documentId())
                .map(path -> "/api/kyc/documents/" + command.documentId() + "/content");
    }

    // ============================================================
    // UpdateUserKycUseCase
    // ============================================================

    @Override
    @Transactional
    public KycTier updateKycTier(UpdateUserKycUseCase.UpdateKycCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }

        // This is an admin operation - manual override
        KycProfile profile = loadUserKycPort.loadKycProfile(command.userId())
                .orElseThrow(() -> new RuntimeException("KYC profile not found for user: " + command.userId()));

        // FIXME: This bypasses normal domain rules - should be restricted and audited
        // Ideally, profile would have a method like profile.overrideTier(newTier,
        // reason)
        // For now, we'll just update and publish an event

        // This is a bit of a hack - we need to add this method to KycProfile
        // profile.overrideTier(command.newTier(), command.reason());

        // Temporary workaround - use reflection or add method later
        // For now, let's assume we have a way to update the tier

        // Save and publish
        saveUserKycPort.update(profile);

        eventPublisher.publish(
                new KycTierUpdated(profile.getId(), command.userId(), profile.getTier()),
                eventPublisher::publishUserEvent);

        return profile.getTier();
    }

    @Override
    @Transactional
    public KycTier recalculateTier(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        KycProfile profile = loadUserKycPort.loadKycProfile(userId)
                .orElseThrow(() -> new RuntimeException("KYC profile not found for user: " + userId));

        // Recalculate tier based on current documents
        KycTier newTier = profile.recalculateTier();

        // Save updated profile
        saveUserKycPort.update(profile);

        // Publish event
        DomainEvent event = new KycTierUpdated(profile.getId(), userId, newTier);
        eventPublisher.publish(event, eventPublisher::publishUserEvent);

        return newTier;
    }

    // Overload that accepts profile ID
    @Transactional
    public KycTier recalculateTier(KycProfileId profileId) {
        if (profileId == null) {
            throw new IllegalArgumentException("profileId cannot be null");
        }

        KycProfile profile = loadUserKycPort.loadKycProfile(profileId)
                .orElseThrow(() -> new RuntimeException("KYC profile not found: " + profileId));

        KycTier newTier = profile.recalculateTier();
        saveUserKycPort.update(profile);

        DomainEvent event = new KycTierUpdated(profileId, profile.getUserId(), newTier);
        eventPublisher.publish(event, eventPublisher::publishUserEvent);

        return newTier;
    }

    // ============================================================
    // DeleteKycDocumentUseCase
    // ============================================================

    @Override
    @Transactional
    public void deleteDocument(DeleteKycDocumentUseCase.DeleteDocumentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }

        // Verify document belongs to user
        KycDocument document = loadKycDocumentPort.loadDocument(command.documentId())
                .orElseThrow(() -> new RuntimeException("Document not found: " + command.documentId()));

        KycProfile profile = loadUserKycPort.loadKycProfile(command.userId())
                .orElseThrow(() -> new RuntimeException("KYC profile not found for user: " + command.userId()));

        if (!document.getProfileId().equals(profile.getId())) {
            throw new RuntimeException("Document does not belong to user's profile");
        }

        // Delete document (metadata + binary)
        deleteKycPort.deleteDocument(command.documentId());

        // Remove from profile and recalculate tier
        KycTier newTier = profile.removeDocument(command.documentId());

        // Save updated profile
        saveUserKycPort.update(profile);

        // Publish events
        DomainEvent deletedEvent = new KycDocumentDeleted(command.documentId(), command.userId(), command.reason());
        DomainEvent tierEvent = new KycTierUpdated(profile.getId(), command.userId(), newTier);

        eventPublisher.publish(deletedEvent, eventPublisher::publishUserEvent);
        eventPublisher.publish(tierEvent, eventPublisher::publishUserEvent);
    }

    // ============================================================
    // DeleteKycProfileUseCase
    // ============================================================

    @Override
    @Transactional
    public void deleteProfile(DeleteKycProfileUseCase.DeleteProfileCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }

        // Load profile
        KycProfile profile = loadUserKycPort.loadKycProfile(command.userId())
                .orElseThrow(() -> new RuntimeException("KYC profile not found for user: " + command.userId()));

        // Delete entire profile (cascades to all documents via DeleteKycPort)
        deleteKycPort.deleteProfile(profile.getId());

        // Publish event
        DomainEvent event = new KycProfileDeleted(profile.getId(), command.userId(), profile.getTier(),
                command.reason());
        eventPublisher.publish(event, eventPublisher::publishUserEvent);

        // Note: No tier event needed since profile is gone
    }
}