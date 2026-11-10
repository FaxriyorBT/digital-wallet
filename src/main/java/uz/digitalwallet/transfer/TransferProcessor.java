package uz.digitalwallet.transfer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Money;
import uz.digitalwallet.event.Topics;
import uz.digitalwallet.event.TransferCompletedEvent;
import uz.digitalwallet.ledger.LedgerService;
import uz.digitalwallet.ledger.Posting;
import uz.digitalwallet.outbox.OutboxService;
import uz.digitalwallet.transaction.Transaction;
import uz.digitalwallet.transaction.TransactionMapper;
import uz.digitalwallet.transaction.TransactionRepository;
import uz.digitalwallet.transaction.dto.TransactionResponse;
import uz.digitalwallet.transfer.dto.TransferRequest;
import uz.digitalwallet.user.User;
import uz.digitalwallet.wallet.SystemWallets;
import uz.digitalwallet.wallet.Wallet;
import uz.digitalwallet.wallet.WalletRepository;
import uz.digitalwallet.wallet.WalletType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Moves the money of one transfer inside a single database transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class TransferProcessor {

    private final WalletRepository wallets;
    private final TransactionRepository transactions;
    private final SystemWallets systemWallets;
    private final TransferPricing pricing;
    private final TransferLimits limits;
    private final LedgerService ledger;
    private final OutboxService outbox;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse process(UUID userId, String idempotencyKey, TransferRequest request) {
        UUID sourceId = request.sourceWalletId();
        if (!wallets.existsByIdAndOwnerId(sourceId, userId)) {
            throw new BusinessException(ErrorCode.WALLET_NOT_FOUND);
        }
        UUID targetId = wallets.findIdByNumberAndType(request.targetWalletNumber(), WalletType.PERSONAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND, "Recipient wallet not found"));
        if (sourceId.equals(targetId)) {
            throw new BusinessException(ErrorCode.SAME_WALLET_TRANSFER);
        }

        Map<UUID, Wallet> locked = lockInIdOrder(sourceId, targetId);
        Wallet source = locked.get(sourceId);
        Wallet target = locked.get(targetId);

        // a request with the same key may have completed while we were waiting for the locks
        Optional<Transaction> previous = transactions.findByInitiatorIdAndIdempotencyKey(userId, idempotencyKey);
        if (previous.isPresent()) {
            return TransferReplay.replay(previous.get(), request, transactionMapper);
        }

        BigDecimal amount = Money.round(request.amount());
        limits.checkTransferAmount(source.getCurrency(), amount);
        checkParticipants(source, target);
        TransferQuote quote = pricing.quote(source, target, amount);
        limits.checkDailyLimit(source, quote.amount());
        if (source.getBalance().compareTo(quote.totalDebit()) < 0) {
            throw new TransferDeclinedException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        Transaction transaction = save(Transaction.transfer(userId, idempotencyKey, source, target, quote.amount(),
                quote.fee(), quote.creditedAmount(), quote.exchangeRate(), request.description()));
        ledger.record(transaction, postings(source, target, quote));
        outbox.enqueue(Topics.TRANSFER_COMPLETED, "Transaction", transaction.getId().toString(),
                completedEvent(transaction, source, target));

        log.info("Transfer {} completed: {} {} from {} to {}", transaction.getId(), quote.amount(), quote.currency(),
                source.getNumber(), target.getNumber());
        return transactionMapper.toResponse(transaction);
    }

    /**
     * Locks are always taken in the same order (by id). Otherwise A->B and B->A running at the same time
     * could each grab one wallet and wait for the other one forever.
     */
    private Map<UUID, Wallet> lockInIdOrder(UUID... walletIds) {
        UUID[] ordered = walletIds.clone();
        Arrays.sort(ordered);
        Map<UUID, Wallet> locked = new HashMap<>();
        for (UUID walletId : ordered) {
            Wallet wallet = wallets.findByIdForUpdate(walletId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
            locked.put(walletId, wallet);
        }
        return locked;
    }

    private static void checkParticipants(Wallet source, Wallet target) {
        if (!source.getOwner().isActive()) {
            throw new TransferDeclinedException(ErrorCode.ACCOUNT_BLOCKED);
        }
        if (!source.isActive()) {
            throw new TransferDeclinedException(ErrorCode.WALLET_NOT_ACTIVE);
        }
        if (!target.isActive() || !target.getOwner().isActive()) {
            throw new TransferDeclinedException(ErrorCode.RECIPIENT_UNAVAILABLE);
        }
    }

    private Transaction save(Transaction transaction) {
        try {
            return transactions.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException e) {
            // a parallel request with the same key (but a different source wallet) got there first
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_IN_USE);
        }
    }

    private List<Posting> postings(Wallet source, Wallet target, TransferQuote quote) {
        List<Posting> postings = new ArrayList<>();
        postings.add(Posting.debit(source, quote.totalDebit()));
        if (quote.isCrossCurrency()) {
            // the system buys the source currency and sells the target currency
            postings.add(Posting.credit(systemWallets.exchange(source.getCurrency()), quote.amount()));
            postings.add(Posting.debit(systemWallets.exchange(target.getCurrency()), quote.creditedAmount()));
        }
        postings.add(Posting.credit(target, quote.creditedAmount()));
        if (quote.hasFee()) {
            postings.add(Posting.credit(systemWallets.fee(source.getCurrency()), quote.fee()));
        }
        return postings;
    }

    private static TransferCompletedEvent completedEvent(Transaction transaction, Wallet source, Wallet target) {
        return new TransferCompletedEvent(UUID.randomUUID(), transaction.getId(), participant(source),
                participant(target), transaction.getAmount(), transaction.getCurrency(), transaction.getFee(),
                transaction.getCreditedAmount(), target.getCurrency(), transaction.getCreatedAt());
    }

    private static TransferCompletedEvent.Participant participant(Wallet wallet) {
        User owner = wallet.getOwner();
        return new TransferCompletedEvent.Participant(owner.getEmail(), owner.getFullName(), wallet.getNumber());
    }
}
