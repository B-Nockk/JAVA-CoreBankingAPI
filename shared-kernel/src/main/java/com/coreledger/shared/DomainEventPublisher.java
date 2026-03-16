// shared-kernel/src/main/java/com/coreledger/shared/DomainEventPublisher.java
package com.coreledger.shared;

import java.util.Collection;
import java.util.function.Consumer;

import com.coreledger.shared.domain.DomainEvent;

/**
 * Publishes domain events to the messaging infrastructure (Kafka).
 *
 * <p>
 * This interface provides multiple publishing strategies to balance
 * backward compatibility with type safety and explicitness:
 *
 * <h2>1. Direct Topic Methods (Backward Compatible)</h2>
 *
 * <pre>{@code
 * // Traditional approach - event determines topic by method name
 * eventPublisher.publishAccountEvent(event);
 * eventPublisher.publishTransferEvent(event);
 * eventPublisher.publishUserEvent(event);
 * }</pre>
 *
 * <h2>2. Explicit Consumer Pattern (New - Recommended)</h2>
 *
 * <pre>{@code
 * // Single event - explicitly state which publisher to use
 * eventPublisher.publish(event, eventPublisher::publishUserEvent);
 *
 * // Multiple events - collection handling
 * List<DomainEvent> events = user.flag(reason);
 * eventPublisher.publishAll(events, eventPublisher::publishUserEvent);
 *
 * // Or with method reference directly in forEach
 * events.forEach(eventPublisher::publishUserEvent);
 * }</pre>
 *
 * <h2>3. Combined with Service-Level Wrappers (Cleanest)</h2>
 *
 * <pre>{@code
 * @Service
 * public class UserService {
 *     private void publishUserEvents(List<? extends DomainEvent> events) {
 *         eventPublisher.publishAll(events, eventPublisher::publishUserEvent);
 *     }
 *
 *     public void flagUser(UserId userId, String reason) {
 *         var events = user.flag(reason);
 *         saveUserPort.save(user);
 *         publishUserEvents(events); // Clean one-liner
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Examples by Context:</h2>
 *
 * <h3>User Context:</h3>
 *
 * <pre>{@code
 * // In UserService
 * events.forEach(eventPublisher::publishUserEvent);
 * // or
 * eventPublisher.publishAll(events, eventPublisher::publishUserEvent);
 * }</pre>
 *
 * <h3>Account Context:</h3>
 *
 * <pre>{@code
 * // In AccountService
 * events.forEach(eventPublisher::publishAccountEvent);
 * // or
 * eventPublisher.publishAll(events, eventPublisher::publishAccountEvent);
 * }</pre>
 *
 * <h3>Transfer Context:</h3>
 *
 * <pre>{@code
 * // In TransferService
 * events.forEach(eventPublisher::publishTransferEvent);
 * // or
 * eventPublisher.publishAll(events, eventPublisher::publishTransferEvent);
 * }</pre>
 *
 * <h2>Design Notes:</h2>
 * <ul>
 * <li><b>Backward Compatible:</b> Existing code using direct methods continues
 * to work</li>
 * <li><b>Explicit:</b> The consumer pattern makes the destination clear at the
 * call site</li>
 * <li><b>Type-Safe:</b> Method references are checked at compile time</li>
 * <li><b>No Routing Logic:</b> Events don't need to know their own type - the
 * service decides</li>
 * <li><b>Testable:</b> Easy to mock and verify which publisher was used</li>
 * </ul>
 *
 * <h2>Adding a New Event Type:</h2>
 * <ol>
 * <li>Add new method to this interface:
 * {@code publishNewContextEvent(DomainEvent event)}</li>
 * <li>Implement in {@code EventPublisher} with appropriate Kafka topic</li>
 * <li>Use in services:
 * {@code events.forEach(eventPublisher::publishNewContextEvent)}</li>
 * </ol>
 *
 * @see com.coreledger.config.EventPublisher
 * @see com.coreledger.shared.domain.DomainEvent
 */
public interface DomainEventPublisher {
    void publishAccountEvent(DomainEvent event);

    void publishTransferEvent(DomainEvent event);

    void publishUserEvent(DomainEvent event);

    // NEW: Accept a function that knows how to publish
    default void publish(DomainEvent event, Consumer<DomainEvent> publisher) {
        publisher.accept(event);
    }

    default void publishAll(Collection<? extends DomainEvent> events,
            Consumer<DomainEvent> publisher) {
        if (events == null)
            return;
        events.forEach(publisher);
    }
}