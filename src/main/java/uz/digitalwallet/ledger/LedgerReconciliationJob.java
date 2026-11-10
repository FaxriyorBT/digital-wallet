package uz.digitalwallet.ledger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.digitalwallet.common.money.Currency;

import java.math.BigDecimal;
import java.util.List;

/**
 * Nightly self-check of the books. Two invariants must always hold:
 * the stored balance of every personal wallet equals the sum of its ledger entries,
 * and all entries of one currency add up to zero.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerReconciliationJob {

    private static final String BALANCE_MISMATCHES = """
            SELECT w.number,
                   w.balance,
                   COALESCE(SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END), 0) AS ledger_balance
            FROM wallets w
                     LEFT JOIN ledger_entries e ON e.wallet_id = w.id
            WHERE w.type = 'PERSONAL'
            GROUP BY w.id, w.number, w.balance
            HAVING w.balance <> COALESCE(SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END), 0)
            """;

    private static final String CURRENCY_IMBALANCES = """
            SELECT w.currency,
                   SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END) AS total
            FROM ledger_entries e
                     JOIN wallets w ON w.id = e.wallet_id
            GROUP BY w.currency
            HAVING SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END) <> 0
            """;

    private final JdbcClient jdbc;

    @Scheduled(cron = "${app.ledger.reconciliation-cron}", zone = "${app.time-zone}")
    public void scheduledRun() {
        run();
    }

    public Report run() {
        List<BalanceMismatch> mismatches = jdbc.sql(BALANCE_MISMATCHES).query(BalanceMismatch.class).list();
        List<CurrencyImbalance> imbalances = jdbc.sql(CURRENCY_IMBALANCES).query(CurrencyImbalance.class).list();

        if (mismatches.isEmpty() && imbalances.isEmpty()) {
            log.info("Ledger reconciliation passed");
        }
        mismatches.forEach(mismatch -> log.error("Wallet {} stores balance {} but its ledger adds up to {}",
                mismatch.number(), mismatch.balance(), mismatch.ledgerBalance()));
        imbalances.forEach(imbalance -> log.error("Ledger entries in {} do not add up to zero: {}",
                imbalance.currency(), imbalance.total()));
        return new Report(mismatches, imbalances);
    }

    public record BalanceMismatch(String number, BigDecimal balance, BigDecimal ledgerBalance) {
    }

    public record CurrencyImbalance(Currency currency, BigDecimal total) {
    }

    public record Report(List<BalanceMismatch> balanceMismatches, List<CurrencyImbalance> currencyImbalances) {

        public boolean isClean() {
            return balanceMismatches.isEmpty() && currencyImbalances.isEmpty();
        }
    }
}
