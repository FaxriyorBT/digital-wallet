package uz.digitalwallet.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    @EntityGraph(attributePaths = {"sourceWallet", "targetWallet"})
    Optional<Transaction> findByInitiatorIdAndIdempotencyKey(UUID initiatorId, String idempotencyKey);

    boolean existsByInitiatorIdAndIdempotencyKey(UUID initiatorId, String idempotencyKey);

    @EntityGraph(attributePaths = {"sourceWallet", "targetWallet"})
    Optional<Transaction> findWithWalletsById(UUID id);

    /**
     * Wallets are fetched in the same query; without the entity graph every row of the page
     * would trigger two more selects when the wallet numbers are mapped (the N+1 problem).
     */
    @Override
    @EntityGraph(attributePaths = {"sourceWallet", "targetWallet"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.sourceWallet.id = :walletId
              and t.type = :type
              and t.status = :status
              and t.createdAt >= :since
            """)
    BigDecimal sumAmount(UUID walletId, TransactionType type, TransactionStatus status, Instant since);

    @Query("""
            select new uz.digitalwallet.transaction.CurrencyTurnover(t.type, t.currency, count(t), sum(t.amount), sum(t.fee))
            from Transaction t
            where t.status = :status
              and t.createdAt >= :from
              and t.createdAt < :to
            group by t.type, t.currency
            order by t.type, t.currency
            """)
    List<CurrencyTurnover> turnover(TransactionStatus status, Instant from, Instant to);
}
