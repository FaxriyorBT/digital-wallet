package uz.digitalwallet.notification;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import tools.jackson.databind.JsonNode;
import uz.digitalwallet.event.Topics;
import uz.digitalwallet.event.UserRegisteredEvent;
import uz.digitalwallet.support.IntegrationTest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uz.digitalwallet.common.money.Currency.UZS;

class NotificationIT extends IntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void transferEventGoesThroughOutboxAndKafkaToEmails() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "100000");

        String transactionId = body(transfer(alice, UUID.randomUUID().toString(), aliceWallet, bobWallet, "10000")
                .andExpect(status().isCreated())).get("id").asString();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, atLeastOnce()).send(sent.capture());
            assertThat(sent.getAllValues())
                    .extracting(message -> message.getTo()[0], SimpleMailMessage::getSubject)
                    .contains(tuple(alice.email(), "You sent 10,000.00 UZS"),
                            tuple(bob.email(), "You received 10,000.00 UZS"));
        });

        List<Instant> publishedAt = jdbc.sql("SELECT published_at FROM outbox_events WHERE aggregate_id = :id")
                .param("id", transactionId)
                .query(Instant.class)
                .list();
        assertThat(publishedAt).hasSize(1).doesNotContainNull();
    }

    @Test
    void duplicateEventIsHandledOnlyOnce() throws Exception {
        String email = UUID.randomUUID() + "@test.uz";
        var event = new UserRegisteredEvent(UUID.randomUUID(), UUID.randomUUID(), email, "Duplicate Test", Instant.now());
        String payload = jsonMapper.writeValueAsString(event);

        // the same message twice, as it happens when the outbox resends after a crash
        kafkaTemplate.send(Topics.USER_REGISTERED, event.userId().toString(), payload).get();
        kafkaTemplate.send(Topics.USER_REGISTERED, event.userId().toString(), payload).get();

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> verify(mailSender).send(argThat(isAddressedTo(email))));
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mailSender, times(1)).send(argThat(isAddressedTo(email))));
    }

    private static org.mockito.ArgumentMatcher<SimpleMailMessage> isAddressedTo(String email) {
        return message -> message != null && message.getTo() != null && email.equals(message.getTo()[0]);
    }
}
