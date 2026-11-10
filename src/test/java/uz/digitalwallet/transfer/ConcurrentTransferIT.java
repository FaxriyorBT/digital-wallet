package uz.digitalwallet.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.ledger.LedgerReconciliationJob;
import uz.digitalwallet.support.IntegrationTest;
import uz.digitalwallet.transfer.dto.TransferRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static uz.digitalwallet.common.money.Currency.UZS;

/**
 * Fires many transfers at the same moment. Without the row locks some of them would read the same
 * old balance and the wallet would end up negative or with money created out of nothing.
 */
class ConcurrentTransferIT extends IntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private LedgerReconciliationJob reconciliation;

    @Test
    void parallelTransfersNeverOverdrawWallet() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "200000");

        TransferRequest request = transferRequest(aliceWallet, bobWallet, "10000");
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            tasks.add(() -> {
                try {
                    transferService.transfer(alice.id(), UUID.randomUUID().toString(), request);
                    return true;
                } catch (BusinessException e) {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
                    return false;
                }
            });
        }

        long succeeded = runAtTheSameTime(tasks).stream().filter(Boolean::booleanValue).count();

        // every transfer costs 10 050 (amount + 0.5% fee), so 200 000 covers exactly 19 of them
        assertThat(succeeded).isEqualTo(19);
        assertThat(balanceOf(alice, aliceWallet)).isEqualByComparingTo("9050");
        assertThat(balanceOf(bob, bobWallet)).isEqualByComparingTo("190000");
        assertThat(reconciliation.run().isClean()).isTrue();
    }

    @Test
    void oppositeTransfersDoNotDeadlock() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "1000000");
        topUp(bob, bobWallet, "1000000");

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> send(alice, transferRequest(aliceWallet, bobWallet, "10000")));
            tasks.add(() -> send(bob, transferRequest(bobWallet, aliceWallet, "10000")));
        }

        assertThat(runAtTheSameTime(tasks)).containsOnly(true);
        // each side sent 20 x 10 050 and received 20 x 10 000
        assertThat(balanceOf(alice, aliceWallet)).isEqualByComparingTo("999000");
        assertThat(balanceOf(bob, bobWallet)).isEqualByComparingTo("999000");
        assertThat(reconciliation.run().isClean()).isTrue();
    }

    private boolean send(TestUser sender, TransferRequest request) {
        transferService.transfer(sender.id(), UUID.randomUUID().toString(), request);
        return true;
    }

    private static List<Boolean> runAtTheSameTime(List<Callable<Boolean>> tasks) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Callable<Boolean> task : tasks) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }

    private static TransferRequest transferRequest(JsonNode source, JsonNode target, String amount) {
        return new TransferRequest(UUID.fromString(source.get("id").asString()), target.get("number").asString(),
                new BigDecimal(amount), null);
    }
}
