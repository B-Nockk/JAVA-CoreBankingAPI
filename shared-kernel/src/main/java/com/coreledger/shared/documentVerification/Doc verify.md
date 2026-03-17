# 1. The Bug Type - "Logic Duplication Bug"

This is called a **"Double Operation Bug"** or **"Redundant Operation Bug"** - where the code is syntactically correct but logically creates duplicate entities. It's a subset of **business logic bugs** where the implementation doesn't match the intended domain behavior.

The technical term is **"Idempotency Violation"** - you're performing the same operation twice when it should only happen once. The document was already created with `KycDocument.submit()`, so calling `profile.submitDocument()` creates another one.

## 2. Your Fix - Keep Your Original Approach!

Actually, your original approach in the domain model is BETTER! Let me explain why:

Looking at your `KycProfile` class:

```java
public void submitDocument(KycDocumentType type) {
    KycDocument document = KycDocument.submit(this.id, type);
    this.documents.add(document);
    this.tier = deriveTier(this.documents);
}
```

**This is actually the CORRECT domain design!** The profile should own document creation. So in your service, it should be:

```java
// Step 3: Let the profile create the document
profile.submitDocument(command.documentType());  // Creates and adds document internally

// Step 4: Get the newly created document from the profile
KycDocument document = profile.getDocuments().stream()
    .filter(d -> d.getType() == command.documentType())
    .reduce((first, second) -> second)  // Get the most recent one
    .orElseThrow(() -> new RuntimeException("Document not found after submission"));

// Step 5: Save document with binary
KycDocument savedDocument = saveKycDocumentPort.save(
    document,
    command.fileContent(),
    command.filename()
);
```

**OR even better**, add a method to get the last submitted document:

```java
// In KycProfile
public KycDocument getLastSubmittedDocument() {
    if (documents.isEmpty()) {
        throw new IllegalStateException("No documents in profile");
    }
    return documents.get(documents.size() - 1);
}
```

This way, the domain model maintains consistency and the service just orchestrates.

## 3. Verification Duplication

You're absolutely right! The service is doing double work:

```java
// In your service
profile.verifyDocument(command.documentId());  // ✅ This already marks it
document.markVerified();                        // ❌ This is redundant!
```

The fix - just remove the redundant line:

```java
@Override
@Transactional
public KycTier verifyDocument(VerifyKycDocumentUseCase.VerifyCommand command) {
    // ... loading code ...

    // Step 3: Call domain behavior - this handles everything!
    profile.verifyDocument(command.documentId());

    // Step 4: Persist changes (just save the profile, it contains the updated document)
    saveUserKycPort.update(profile);

    // No need to save document separately - it's part of the profile aggregate!

    // ... event publishing ...
}
```

## 4. Impressing Recruiters with Verification Strategy

Great question! Here are several impressive approaches that show architectural thinking:

### A. **Strategy Pattern with Pluggable Verification Providers**

```java
public interface DocumentVerificationStrategy {
    VerificationResult verify(KycDocument document, byte[] content);
    boolean supports(KycDocumentType documentType);
}

@Component
public class IdCardVerificationStrategy implements DocumentVerificationStrategy {
    private final GovernmentApiClient govClient;

    @Override
    public VerificationResult verify(KycDocument document, byte[] content) {
        // Extract data from ID
        String idNumber = extractIdNumber(content);

        // Call government API (mock for demo)
        GovernmentResponse response = govClient.verifyIdentity(idNumber);

        return new VerificationResult(
            response.isValid(),
            response.getFullName(),
            response.getDateOfBirth()
        );
    }

    @Override
    public boolean supports(KycDocumentType type) {
        return type == KycDocumentType.ID_CARD;
    }
}

@Component
public class AddressProofVerificationStrategy implements DocumentVerificationStrategy {
    private final GeocodingService geocodingService;

    @Override
    public VerificationResult verify(KycDocument document, byte[] content) {
        // Extract address from utility bill
        String address = extractAddress(content);

        // Geocode to verify it's real
        boolean exists = geocodingService.addressExists(address);

        return new VerificationResult(exists, address);
    }

    @Override
    public boolean supports(KycDocumentType type) {
        return type == KycDocumentType.ADDRESS_PROOF;
    }
}

@Service
public class VerificationOrchestrator {
    private final List<DocumentVerificationStrategy> strategies;

    public VerificationResult verify(KycDocument document, byte[] content) {
        return strategies.stream()
            .filter(s -> s.supports(document.getType()))
            .findFirst()
            .map(s -> s.verify(document, content))
            .orElseThrow(() -> new UnsupportedOperationException(
                "No verification strategy for: " + document.getType()
            ));
    }
}
```

### B. **Verification Status Machine with Events**

```java
public enum VerificationStatus {
    PENDING,
    AUTOMATED_CHECK_PASSED,
    AUTOMATED_CHECK_FAILED,
    MANUAL_REVIEW_REQUIRED,
    VERIFIED,
    REJECTED;

    public VerificationStatus next(AutomatedCheckResult result) {
        return switch(this) {
            case PENDING -> result.passed()
                ? AUTOMATED_CHECK_PASSED
                : MANUAL_REVIEW_REQUIRED;
            case AUTOMATED_CHECK_PASSED -> VERIFIED;
            case MANUAL_REVIEW_REQUIRED -> result.escalated()
                ? MANUAL_REVIEW_REQUIRED
                : REJECTED;
            default -> this;
        };
    }
}
```

### C. **AI/ML Ready Architecture**

```java
@Component
public class AIDocumentVerificationService {
    private final List<DocumentAnalyzer> analyzers;

    public VerificationScore analyze(KycDocument document, byte[] content) {
        FraudScore score = new FraudScore();

        // 1. OCR Quality Check
        score.addMetric("ocr_confidence", analyzeOcrQuality(content));

        // 2. Forgery Detection
        score.addMetric("forgery_risk", detectForgery(content));

        // 3. Face Matching (if selfie + ID)
        if (document.getType() == KycDocumentType.BIOMETRICS) {
            score.addMetric("face_match", compareWithIdDocument(content));
        }

        // 4. Pattern Recognition
        score.addMetric("anomaly_score", detectAnomalies(document, content));

        return score;
    }
}
```

### D. **The "Production-Ready" Approach (Most Impressive)**

```java
@Component
public class VerificationService {
    private final List<VerificationProvider> providers;
    private final VerificationMetrics metrics;
    private final AuditLogger auditLogger;

    @Transactional
    public VerificationResult verify(KycDocument document) {
        // Start timing for metrics
        var timer = metrics.startTimer();

        try {
            // 1. Try automated verification first
            Optional<VerificationResult> automated = tryAutomatedVerification(document);

            if (automated.isPresent()) {
                metrics.recordAutomatedSuccess(document.getType());
                auditLogger.logAutomatedVerification(document, automated.get());
                return automated.get();
            }

            // 2. Fall back to manual review queue
            metrics.recordManualReviewRequired(document.getType());
            auditLogger.logManualReviewQueued(document);

            return VerificationResult.requiresManualReview();

        } catch (Exception e) {
            metrics.recordVerificationError(document.getType(), e);
            auditLogger.logVerificationError(document, e);
            throw new VerificationException("Verification failed", e);
        } finally {
            timer.stop();
        }
    }

    private Optional<VerificationResult> tryAutomatedVerification(KycDocument document) {
        // Try providers in order of confidence/cost
        return providers.stream()
            .filter(provider -> provider.supports(document.getType()))
            .sorted(Comparator.comparing(VerificationProvider::getConfidence).reversed())
            .map(provider -> provider.verify(document))
            .filter(VerificationResult::isSuccessful)
            .findFirst();
    }
}
```

### E. **Simple but Smart: Mock Verification with Config**

```java
@Component
@Profile("demo")
public class MockVerificationService implements VerificationService {

    @Value("${verification.mock.success-rate:0.9}")
    private double successRate;

    @Value("${verification.mock.auto-verify-tiers:TIER_1,TIER_2}")
    private List<KycTier> autoVerifyTiers;

    @Override
    public VerificationResult verify(KycDocument document) {
        // Simulate real verification
        boolean passed = ThreadLocalRandom.current().nextDouble() < successRate;

        // Add realistic delay
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(500, 2000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return new VerificationResult(
            passed,
            "Mock verification " + (passed ? "passed" : "failed"),
            Map.of(
                "confidence", ThreadLocalRandom.current().nextDouble(0.7, 1.0),
                "verifiedBy", "MockSystem",
                "timestamp", Instant.now()
            )
        );
    }
}
```

## What Impresses Recruiters Most:

1. **Pluggable Architecture** - Easy to swap real providers
2. **Metrics & Monitoring** - Track success rates, failures
3. **Audit Trail** - Every verification is logged
4. **Fallback Strategies** - Automated → Manual review
5. **Configuration** - Toggle features without code changes
6. **Resilience** - Handle API failures gracefully

The **Strategy Pattern (A)** plus **Metrics (D)** shows you understand real-world production needs!
