package uz.digitalwallet.transfer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Money;
import uz.digitalwallet.transaction.Transaction;
import uz.digitalwallet.transaction.TransactionRepository;
import uz.digitalwallet.transfer.dto.TransferRequest;
import uz.digitalwallet.wallet.WalletRepository;
import uz.digitalwallet.wallet.WalletType;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class TransferDeclineRecorder {

    private final TransactionRepository transactions;
    private final WalletRepository wallets;

    /**
     * Runs in a new transaction: the transfer transaction has already been rolled back,
     * but the declined attempt should still show up in the sender's history.
     */
    @Transactional
    public void record(UUID userId, String idempotencyKey, TransferRequest request, ErrorCode reason) {
        if (transactions.existsByInitiatorIdAndIdempotencyKey(userId, idempotencyKey)) {
            return;
        }
        Optional<UUID> targetId = wallets.findIdByNumberAndType(request.targetWalletNumber(), WalletType.PERSONAL);
        if (targetId.isEmpty()) {
            return;
        }
        transactions.save(Transaction.declinedTransfer(userId, idempotencyKey,
                wallets.getReferenceById(request.sourceWalletId()), wallets.getReferenceById(targetId.get()),
                Money.round(request.amount()), reason, request.description()));
    }
}
