package uz.digitalwallet.transfer;

import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.transaction.Transaction;
import uz.digitalwallet.transaction.TransactionMapper;
import uz.digitalwallet.transaction.dto.TransactionResponse;
import uz.digitalwallet.transfer.dto.TransferRequest;

/**
 * Answers a repeated request with the result of the first one, the same way payment APIs like Stripe do.
 */
final class TransferReplay {

    private TransferReplay() {
    }

    static TransactionResponse replay(Transaction previous, TransferRequest request, TransactionMapper mapper) {
        if (!previous.isSameTransfer(request.sourceWalletId(), request.targetWalletNumber(), request.amount())) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        if (previous.isDeclined()) {
            throw new BusinessException(previous.getDeclineReason());
        }
        return mapper.toResponse(previous);
    }
}
