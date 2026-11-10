package uz.digitalwallet.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.transaction.Transaction;
import uz.digitalwallet.wallet.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Double-entry bookkeeping: every movement of money is written as debits and credits that cancel out
 * per currency, so money can never appear or disappear. This is the only place where balances change.
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository entries;

    /**
     * Must run inside the caller's transaction, after the caller has locked the personal wallets involved.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Transaction transaction, List<Posting> postings) {
        requireBalanced(postings);
        List<LedgerEntry> newEntries = new ArrayList<>(postings.size());
        for (Posting posting : postings) {
            newEntries.add(apply(transaction, posting));
        }
        entries.saveAll(newEntries);
    }

    private static LedgerEntry apply(Transaction transaction, Posting posting) {
        Wallet wallet = posting.wallet();
        BigDecimal balanceAfter = null;
        if (wallet.isPersonal()) {
            balanceAfter = switch (posting.direction()) {
                case DEBIT -> wallet.debit(posting.amount());
                case CREDIT -> wallet.credit(posting.amount());
            };
        }
        return new LedgerEntry(transaction, wallet, posting.direction(), posting.amount(), balanceAfter);
    }

    private static void requireBalanced(List<Posting> postings) {
        Map<Currency, BigDecimal> totals = new EnumMap<>(Currency.class);
        for (Posting posting : postings) {
            if (posting.amount().signum() <= 0) {
                throw new IllegalArgumentException("Posting amount must be positive: " + posting);
            }
            BigDecimal signed = posting.direction() == EntryDirection.CREDIT
                    ? posting.amount()
                    : posting.amount().negate();
            totals.merge(posting.wallet().getCurrency(), signed, BigDecimal::add);
        }
        totals.forEach((currency, total) -> {
            if (total.signum() != 0) {
                throw new IllegalStateException("Postings are not balanced in " + currency + ", difference " + total);
            }
        });
    }
}
