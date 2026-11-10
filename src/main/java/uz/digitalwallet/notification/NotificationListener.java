package uz.digitalwallet.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import uz.digitalwallet.event.Topics;
import uz.digitalwallet.event.TransferCompletedEvent;
import uz.digitalwallet.event.UserRegisteredEvent;

/**
 * Notifications are sent asynchronously: a slow or unavailable mail server must never delay or fail a transfer.
 */
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final ProcessedEvents processedEvents;
    private final EmailNotifier emailNotifier;

    @KafkaListener(topics = Topics.USER_REGISTERED)
    public void onUserRegistered(UserRegisteredEvent event) {
        processedEvents.runOnce(event.eventId(), () -> emailNotifier.sendWelcome(event));
    }

    @KafkaListener(topics = Topics.TRANSFER_COMPLETED)
    public void onTransferCompleted(TransferCompletedEvent event) {
        processedEvents.runOnce(event.eventId(), () -> emailNotifier.sendTransferNotifications(event));
    }
}
