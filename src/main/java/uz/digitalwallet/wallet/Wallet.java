package uz.digitalwallet.wallet;

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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.common.money.Money;
import uz.digitalwallet.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    /**
     * Null for system wallets.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    private String number;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    private WalletType type;

    @Enumerated(EnumType.STRING)
    private WalletStatus status;

    /**
     * Stored only for personal wallets. System wallets are touched by almost every transfer,
     * keeping a balance column for them would make that single row a bottleneck.
     */
    private BigDecimal balance;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private long version;

    public static Wallet open(User owner, Currency currency, String number) {
        Wallet wallet = new Wallet();
        wallet.owner = owner;
        wallet.currency = currency;
        wallet.number = number;
        wallet.type = WalletType.PERSONAL;
        wallet.status = WalletStatus.ACTIVE;
        wallet.balance = Money.zero();
        return wallet;
    }

    public boolean isPersonal() {
        return type == WalletType.PERSONAL;
    }

    public boolean isActive() {
        return status == WalletStatus.ACTIVE;
    }

    public boolean isOwnedBy(UUID userId) {
        // reading the id of a lazy proxy does not trigger a query
        return owner != null && owner.getId().equals(userId);
    }

    public BigDecimal credit(BigDecimal amount) {
        requirePersonal();
        balance = balance.add(amount);
        return balance;
    }

    public BigDecimal debit(BigDecimal amount) {
        requirePersonal();
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Wallet " + number + " would go below zero");
        }
        balance = balance.subtract(amount);
        return balance;
    }

    public void close() {
        if (status != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.WALLET_NOT_ACTIVE);
        }
        if (balance.signum() != 0) {
            throw new BusinessException(ErrorCode.WALLET_NOT_EMPTY);
        }
        status = WalletStatus.CLOSED;
    }

    public void changeStatus(WalletStatus newStatus) {
        if (status == WalletStatus.CLOSED || newStatus == WalletStatus.CLOSED) {
            throw new BusinessException(ErrorCode.STATUS_CHANGE_NOT_ALLOWED,
                    "Closed wallets cannot be changed and only the owner can close a wallet");
        }
        status = newStatus;
    }

    private void requirePersonal() {
        if (!isPersonal()) {
            throw new IllegalStateException("Balance is not tracked for system wallet " + number);
        }
    }
}
