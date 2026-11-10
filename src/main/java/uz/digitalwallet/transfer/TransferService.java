package uz.digitalwallet.transfer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Money;
import uz.digitalwallet.transaction.Transaction;
import uz.digitalwallet.transaction.TransactionMapper;
import uz.digitalwallet.transaction.TransactionRepository;
import uz.digitalwallet.transaction.dto.TransactionResponse;
import uz.digitalwallet.transfer.dto.TransferRequest;
import uz.digitalwallet.wallet.Wallet;
import uz.digitalwallet.wallet.WalletRepository;
import uz.digitalwallet.wallet.WalletType;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferProcessor processor;
    private final TransferDeclineRecorder declineRecorder;
    private final TransactionRepository transactions;
    private final WalletRepository wallets;
    private final TransferPricing pricing;
    private final TransferLimits limits;
    private final TransactionMapper transactionMapper;

    /**
     * Deliberately not @Transactional. The money moves in its own transaction inside {@link TransferProcessor};
     * if it is declined, that transaction rolls back and the decline is stored in a second, separate one.
     * With one shared transaction the stored decline would be rolled back too.
     */
    public TransactionResponse transfer(UUID userId, String idempotencyKey, TransferRequest request) {
        Optional<Transaction> previous = transactions.findByInitiatorIdAndIdempotencyKey(userId, idempotencyKey);
        if (previous.isPresent()) {
            return TransferReplay.replay(previous.get(), request, transactionMapper);
        }
        try {
            return processor.process(userId, idempotencyKey, request);
        } catch (TransferDeclinedException e) {
            recordDecline(userId, idempotencyKey, request, e.getErrorCode());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public TransferQuote quote(UUID userId, TransferRequest request) {
        Wallet source = wallets.findByIdAndOwnerId(request.sourceWalletId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        Wallet target = wallets.findByNumberAndType(request.targetWalletNumber(), WalletType.PERSONAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND, "Recipient wallet not found"));
        if (source.getId().equals(target.getId())) {
            throw new BusinessException(ErrorCode.SAME_WALLET_TRANSFER);
        }
        BigDecimal amount = Money.round(request.amount());
        limits.checkTransferAmount(source.getCurrency(), amount);
        return pricing.quote(source, target, amount);
    }

    private void recordDecline(UUID userId, String idempotencyKey, TransferRequest request, ErrorCode reason) {
        try {
            declineRecorder.record(userId, idempotencyKey, request, reason);
        } catch (DataAccessException e) {
            // the client must still get the real decline reason even if saving the attempt failed
            log.warn("Could not save declined transfer of user {}: {}", userId, e.getMessage());
        }
    }
}
