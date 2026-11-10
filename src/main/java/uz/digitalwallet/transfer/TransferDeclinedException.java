package uz.digitalwallet.transfer;

import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;

/**
 * A transfer refused for a business reason (insufficient funds, limit, blocked wallet).
 * Unlike plain validation errors, these attempts are stored in the sender's history with status DECLINED.
 */
public class TransferDeclinedException extends BusinessException {

    public TransferDeclinedException(ErrorCode reason) {
        super(reason);
    }
}
