package uz.digitalwallet.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.event.TransferCompletedEvent;
import uz.digitalwallet.event.UserRegisteredEvent;

import java.math.BigDecimal;
import java.util.Locale;

@Component
@RequiredArgsConstructor
class EmailNotifier {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    void sendWelcome(UserRegisteredEvent event) {
        send(event.email(), "Welcome to Digital Wallet", """
                Hello, %s!

                Your account is ready. Open a wallet in UZS, USD or EUR to start sending money.

                Digital Wallet
                """.formatted(event.fullName()));
    }

    void sendTransferNotifications(TransferCompletedEvent event) {
        TransferCompletedEvent.Participant sender = event.sender();
        TransferCompletedEvent.Participant recipient = event.recipient();
        String sent = format(event.amount(), event.currency());

        send(sender.email(), "You sent " + sent, """
                Hello, %s!

                %s was sent from your wallet %s to wallet %s.
                Fee: %s
                Transaction: %s

                Digital Wallet
                """.formatted(sender.fullName(), sent, mask(sender.walletNumber()), mask(recipient.walletNumber()),
                format(event.fee(), event.currency()), event.transactionId()));

        // a transfer between two wallets of the same person needs only one email
        if (sender.email().equals(recipient.email())) {
            return;
        }
        String received = format(event.creditedAmount(), event.creditedCurrency());
        send(recipient.email(), "You received " + received, """
                Hello, %s!

                %s arrived in your wallet %s from %s.
                Transaction: %s

                Digital Wallet
                """.formatted(recipient.fullName(), received, mask(recipient.walletNumber()), sender.fullName(),
                event.transactionId()));
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    private static String format(BigDecimal amount, Currency currency) {
        return String.format(Locale.US, "%,.2f %s", amount, currency);
    }

    private static String mask(String walletNumber) {
        return "*" + walletNumber.substring(walletNumber.length() - 4);
    }
}
