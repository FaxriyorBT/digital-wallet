package uz.digitalwallet.transfer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Money;
import uz.digitalwallet.ledger.LedgerService;
import uz.digitalwallet.ledger.Posting;
import uz.digitalwallet.transaction.Transaction;
import uz.digitalwallet.transaction.TransactionMapper;
import uz.digitalwallet.transaction.TransactionRepository;
import uz.digitalwallet.transaction.dto.TransactionResponse;
import uz.digitalwallet.transfer.dto.TopUpRequest;
import uz.digitalwallet.wallet.SystemWallets;
import uz.digitalwallet.wallet.Wallet;
import uz.digitalwallet.wallet.WalletRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Simulates paying money in from a bank card. In the books the money comes from the FUNDING system wallet,
 * so every top-up is still a balanced pair of entries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopUpService {

    private final WalletRepository wallets;
    private final TransactionRepository transactions;
    private final SystemWallets systemWallets;
    private final TransferLimits limits;
    private final LedgerService ledger;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse topUp(UUID userId, UUID walletId, String idempotencyKey, TopUpRequest request) {
        if (!wallets.existsByIdAndOwnerId(walletId, userId)) {
            throw new BusinessException(ErrorCode.WALLET_NOT_FOUND);
        }
        Wallet wallet = wallets.findByIdForUpdate(walletId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        Optional<Transaction> previous = transactions.findByInitiatorIdAndIdempotencyKey(userId, idempotencyKey);
        if (previous.isPresent()) {
            if (!previous.get().isSameTopUp(walletId, request.amount())) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            return transactionMapper.toResponse(previous.get());
        }

        if (!wallet.getOwner().isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_BLOCKED);
        }
        if (!wallet.isActive()) {
            throw new BusinessException(ErrorCode.WALLET_NOT_ACTIVE);
        }
        BigDecimal amount = Money.round(request.amount());
        limits.checkTopUpAmount(wallet.getCurrency(), amount);

        Transaction transaction = Transaction.topUp(userId, idempotencyKey, wallet, amount);
        try {
            transactions.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_IN_USE);
        }
        ledger.record(transaction, List.of(
                Posting.debit(systemWallets.funding(wallet.getCurrency()), amount),
                Posting.credit(wallet, amount)));

        log.info("Wallet {} topped up with {} {}", wallet.getNumber(), amount, wallet.getCurrency());
        return transactionMapper.toResponse(transaction);
    }
}
