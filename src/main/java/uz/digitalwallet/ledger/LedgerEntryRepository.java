package uz.digitalwallet.ledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @EntityGraph(attributePaths = {"transaction", "transaction.sourceWallet", "transaction.targetWallet"})
    @Query("""
            select e from LedgerEntry e
            where e.wallet.id = :walletId
              and e.createdAt >= :from
              and e.createdAt < :to
            """)
    Page<LedgerEntry> findStatement(UUID walletId, Instant from, Instant to, Pageable pageable);

    List<LedgerEntry> findAllByTransactionId(UUID transactionId);
}
