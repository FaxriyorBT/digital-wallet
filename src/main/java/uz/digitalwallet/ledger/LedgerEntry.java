package uz.digitalwallet.ledger;

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
import uz.digitalwallet.transaction.Transaction;
import uz.digitalwallet.wallet.Wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    private EntryDirection direction;

    private BigDecimal amount;

    /**
     * Balance of a personal wallet right after this entry; null for system wallets.
     */
    private BigDecimal balanceAfter;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    LedgerEntry(Transaction transaction, Wallet wallet, EntryDirection direction, BigDecimal amount,
                BigDecimal balanceAfter) {
        this.transaction = transaction;
        this.wallet = wallet;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }
}
