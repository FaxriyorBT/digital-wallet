package uz.digitalwallet.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;
import uz.digitalwallet.ledger.EntryDirection;
import uz.digitalwallet.ledger.LedgerEntry;
import uz.digitalwallet.ledger.LedgerEntryRepository;
import uz.digitalwallet.support.IntegrationTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uz.digitalwallet.common.money.Currency.USD;
import static uz.digitalwallet.common.money.Currency.UZS;

class TransferIT extends IntegrationTest {

    @Autowired
    private LedgerEntryRepository ledgerEntries;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void transferMovesMoneyChargesFeeAndWritesBalancedLedger() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "500000");

        JsonNode transaction = body(transfer(alice, key(), aliceWallet, bobWallet, "100000")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.fee").value(500.0))
                .andExpect(jsonPath("$.creditedAmount").value(100000.0)));

        assertThat(balanceOf(alice, aliceWallet)).isEqualByComparingTo("399500");
        assertThat(balanceOf(bob, bobWallet)).isEqualByComparingTo("100000");

        UUID transactionId = UUID.fromString(transaction.get("id").asString());
        List<LedgerEntry> entries = ledgerEntries.findAllByTransactionId(transactionId);
        // sender debit, recipient credit, fee credit
        assertThat(entries).hasSize(3);
        BigDecimal net = entries.stream()
                .map(e -> e.getDirection() == EntryDirection.CREDIT ? e.getAmount() : e.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(net).isEqualByComparingTo("0");

        long outboxEvents = jdbc.sql("SELECT count(*) FROM outbox_events WHERE aggregate_id = :id")
                .param("id", transactionId.toString())
                .query(Long.class)
                .single();
        assertThat(outboxEvents).isEqualTo(1);
    }

    @Test
    void sameIdempotencyKeyDoesNotSendTwice() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "100000");
        String key = key();

        String first = body(transfer(alice, key, aliceWallet, bobWallet, "10000")
                .andExpect(status().isCreated())).get("id").asString();
        String retried = body(transfer(alice, key, aliceWallet, bobWallet, "10000")
                .andExpect(status().isCreated())).get("id").asString();

        assertThat(retried).isEqualTo(first);
        assertThat(balanceOf(alice, aliceWallet)).isEqualByComparingTo("89950");

        transfer(alice, key, aliceWallet, bobWallet, "20000")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void transferWithoutIdempotencyKeyIsRejected() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);

        perform(post("/api/v1/transfers").header("Authorization", alice.bearer()), Map.of(
                "sourceWalletId", aliceWallet.get("id").asString(),
                "targetWalletNumber", bobWallet.get("number").asString(),
                "amount", new BigDecimal("10000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void declinedTransferIsVisibleOnlyToSender() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "1000");

        transfer(alice, key(), aliceWallet, bobWallet, "5000")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        mvc.perform(get("/api/v1/transactions").param("status", "DECLINED").header("Authorization", alice.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].declineReason").value("INSUFFICIENT_FUNDS"));
        mvc.perform(get("/api/v1/transactions").header("Authorization", bob.bearer()))
                .andExpect(jsonPath("$.totalElements").value(0));
        assertThat(balanceOf(alice, aliceWallet)).isEqualByComparingTo("1000");
    }

    @Test
    void convertsCurrencyBetweenOwnWalletsWithoutFee() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        JsonNode uzsWallet = openWallet(alice, UZS);
        JsonNode usdWallet = openWallet(alice, USD);
        topUp(alice, uzsWallet, "1200000");

        transfer(alice, key(), uzsWallet, usdWallet, "120000")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fee").value(0.0))
                .andExpect(jsonPath("$.creditedAmount").value(10.0))
                .andExpect(jsonPath("$.creditedCurrency").value("USD"));

        assertThat(balanceOf(alice, uzsWallet)).isEqualByComparingTo("1080000");
        assertThat(balanceOf(alice, usdWallet)).isEqualByComparingTo("10.00");
    }

    @Test
    void quoteShowsTheSameNumbersAsTheTransfer() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, USD);
        JsonNode bobWallet = openWallet(bob, UZS);

        perform(post("/api/v1/transfers/quote").header("Authorization", alice.bearer()), Map.of(
                "sourceWalletId", aliceWallet.get("id").asString(),
                "targetWalletNumber", bobWallet.get("number").asString(),
                "amount", new BigDecimal("50")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(0.25))
                .andExpect(jsonPath("$.totalDebit").value(50.25))
                .andExpect(jsonPath("$.creditedAmount").value(600000.0));
    }

    @Test
    void dailyLimitIsEnforced() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "50000000");
        topUp(alice, aliceWallet, "50000000");

        transfer(alice, key(), aliceWallet, bobWallet, "20000000").andExpect(status().isCreated());
        transfer(alice, key(), aliceWallet, bobWallet, "20000000").andExpect(status().isCreated());

        // 40 million already sent today, the limit is 50 million
        transfer(alice, key(), aliceWallet, bobWallet, "15000000")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("DAILY_LIMIT_EXCEEDED"));
    }

    @Test
    void amountOutsideLimitsIsRejected() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "100000");

        transfer(alice, key(), aliceWallet, bobWallet, "500")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("AMOUNT_OUT_OF_RANGE"));
    }

    @Test
    void cannotSendFromSomeoneElsesWallet() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser mallory = registerUser("Mallory Evil");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode malloryWallet = openWallet(mallory, UZS);
        topUp(alice, aliceWallet, "100000");

        transfer(mallory, key(), aliceWallet, malloryWallet, "10000")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"));
        assertThat(balanceOf(alice, aliceWallet)).isEqualByComparingTo("100000");
    }

    @Test
    void statementListsEntriesWithRunningBalance() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "100000");
        transfer(alice, key(), aliceWallet, bobWallet, "10000").andExpect(status().isCreated());

        mvc.perform(get("/api/v1/wallets/{id}/statement", aliceWallet.get("id").asString())
                        .header("Authorization", alice.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].direction").value("DEBIT"))
                .andExpect(jsonPath("$.content[0].amount").value(10050.0))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(89950.0))
                .andExpect(jsonPath("$.content[0].counterpartyWalletNumber").value(bobWallet.get("number").asString()))
                .andExpect(jsonPath("$.content[1].direction").value("CREDIT"))
                .andExpect(jsonPath("$.content[1].balanceAfter").value(100000.0));
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
