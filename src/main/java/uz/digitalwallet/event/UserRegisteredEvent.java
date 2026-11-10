package uz.digitalwallet.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        UUID userId,
        String email,
        String fullName,
        Instant occurredAt) {
}
