package uz.digitalwallet.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;
import uz.digitalwallet.support.IntegrationTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uz.digitalwallet.common.money.Currency.UZS;

class LedgerReconciliationIT extends IntegrationTest {

    @Autowired
    private LedgerReconciliationJob reconciliation;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void detectsBalanceChangedBehindTheLedgersBack() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        JsonNode wallet = openWallet(alice, UZS);
        topUp(alice, wallet, "1000");
        UUID walletId = UUID.fromString(wallet.get("id").asString());

        assertThat(reconciliation.run().isClean()).isTrue();

        // simulate a bug or a manual UPDATE that skipped the ledger
        jdbc.sql("UPDATE wallets SET balance = balance + 1 WHERE id = :id").param("id", walletId).update();
        try {
            LedgerReconciliationJob.Report report = reconciliation.run();

            assertThat(report.isClean()).isFalse();
            assertThat(report.balanceMismatches())
                    .extracting(LedgerReconciliationJob.BalanceMismatch::number)
                    .containsExactly(wallet.get("number").asString());
        } finally {
            jdbc.sql("UPDATE wallets SET balance = balance - 1 WHERE id = :id").param("id", walletId).update();
        }
    }
}
