package uz.digitalwallet.transfer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.transaction.TransactionRepository;
import uz.digitalwallet.transaction.TransactionStatus;
import uz.digitalwallet.transaction.TransactionType;
import uz.digitalwallet.wallet.Wallet;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TransferLimits {

    private final TransferProperties properties;
    private final TransactionRepository transactions;
    private final Clock clock;

    public void checkTransferAmount(Currency currency, BigDecimal amount) {
        TransferProperties.Limits limits = properties.limitsFor(currency);
        checkRange(amount, limits.minAmount(), limits.maxAmount(), currency);
    }

    public void checkTopUpAmount(Currency currency, BigDecimal amount) {
        TransferProperties.Limits limits = properties.limitsFor(currency);
        checkRange(amount, limits.minAmount(), limits.maxTopUp(), currency);
    }

    /**
     * Safe against races only because the caller holds the lock on the source wallet: a user has one wallet
     * per currency, so no other transfer can be counted between this check and the insert.
     */
    public void checkDailyLimit(Wallet source, BigDecimal amount) {
        Instant startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        BigDecimal sentToday = transactions.sumAmount(source.getId(), TransactionType.TRANSFER,
                TransactionStatus.COMPLETED, startOfDay);
        BigDecimal dailyLimit = properties.limitsFor(source.getCurrency()).dailyAmount();
        if (sentToday.add(amount).compareTo(dailyLimit) > 0) {
            throw new TransferDeclinedException(ErrorCode.DAILY_LIMIT_EXCEEDED);
        }
    }

    private static void checkRange(BigDecimal amount, BigDecimal min, BigDecimal max, Currency currency) {
        if (amount.compareTo(min) < 0 || amount.compareTo(max) > 0) {
            throw new BusinessException(ErrorCode.AMOUNT_OUT_OF_RANGE, "Amount must be between %s and %s %s"
                    .formatted(min.toPlainString(), max.toPlainString(), currency));
        }
    }
}
