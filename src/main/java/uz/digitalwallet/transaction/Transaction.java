package uz.digitalwallet.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.common.money.Money;
import uz.digitalwallet.wallet.Wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A money operation as the user sees it. The actual balance changes are recorded as ledger entries.
 * Rows are never updated: a correction would be a new transaction.
 */
@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private UUID initiatorId;

    private String idempotencyKey;

    /**
     * Null for top-ups: that money comes from outside the system.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_wallet_id")
    private Wallet sourceWallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_wallet_id")
    private Wallet targetWallet;

    /**
     * Amount in the source currency, without the fee.
     */
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private BigDecimal fee;

    /**
     * Amount that reached the target wallet, in its currency.
     */
    private BigDecimal creditedAmount;

    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    private ErrorCode declineReason;

    private String description;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    public static Transaction topUp(UUID initiatorId, String idempotencyKey, Wallet wallet, BigDecimal amount) {
        Transaction transaction = base(TransactionType.TOP_UP, TransactionStatus.COMPLETED, initiatorId, idempotencyKey);
        transaction.targetWallet = wallet;
        transaction.amount = amount;
        transaction.currency = wallet.getCurrency();
        transaction.fee = Money.zero();
        transaction.creditedAmount = amount;
        return transaction;
    }

    public static Transaction transfer(UUID initiatorId, String idempotencyKey, Wallet source, Wallet target,
                                       BigDecimal amount, BigDecimal fee, BigDecimal creditedAmount,
                                       BigDecimal exchangeRate, String description) {
        Transaction transaction = base(TransactionType.TRANSFER, TransactionStatus.COMPLETED, initiatorId, idempotencyKey);
        transaction.sourceWallet = source;
        transaction.targetWallet = target;
        transaction.amount = amount;
        transaction.currency = source.getCurrency();
        transaction.fee = fee;
        transaction.creditedAmount = creditedAmount;
        transaction.exchangeRate = exchangeRate;
        transaction.description = description;
        return transaction;
    }

    public static Transaction declinedTransfer(UUID initiatorId, String idempotencyKey, Wallet source, Wallet target,
                                               BigDecimal amount, ErrorCode reason, String description) {
        Transaction transaction = base(TransactionType.TRANSFER, TransactionStatus.DECLINED, initiatorId, idempotencyKey);
        transaction.sourceWallet = source;
        transaction.targetWallet = target;
        transaction.amount = amount;
        transaction.currency = source.getCurrency();
        transaction.fee = Money.zero();
        transaction.declineReason = reason;
        transaction.description = description;
        return transaction;
    }

    private static Transaction base(TransactionType type, TransactionStatus status, UUID initiatorId,
                                    String idempotencyKey) {
        Transaction transaction = new Transaction();
        transaction.type = type;
        transaction.status = status;
        transaction.initiatorId = initiatorId;
        transaction.idempotencyKey = idempotencyKey;
        return transaction;
    }

    public boolean isDeclined() {
        return status == TransactionStatus.DECLINED;
    }

    /**
     * The sender sees everything they started; the receiver sees only transfers that actually completed.
     */
    public boolean isVisibleTo(UUID userId) {
        return initiatorId.equals(userId) || (status == TransactionStatus.COMPLETED && targetWallet.isOwnedBy(userId));
    }

    public boolean isSameTransfer(UUID sourceWalletId, String targetWalletNumber, BigDecimal transferAmount) {
        return type == TransactionType.TRANSFER
                && sourceWallet.getId().equals(sourceWalletId)
                && targetWallet.getNumber().equals(targetWalletNumber)
                && amount.compareTo(transferAmount) == 0;
    }

    public boolean isSameTopUp(UUID walletId, BigDecimal topUpAmount) {
        return type == TransactionType.TOP_UP
                && targetWallet.getId().equals(walletId)
                && amount.compareTo(topUpAmount) == 0;
    }
}
