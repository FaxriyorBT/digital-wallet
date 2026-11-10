package uz.digitalwallet.transaction;

import org.springframework.data.jpa.domain.Specification;
import uz.digitalwallet.transaction.dto.TransactionFilter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    static Specification<Transaction> visibleTo(UUID userId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("initiatorId"), userId),
                cb.and(
                        cb.equal(root.get("targetWallet").get("owner").get("id"), userId),
                        cb.equal(root.get("status"), TransactionStatus.COMPLETED)));
    }

    /**
     * Builds the WHERE clause only from the filters that were actually sent.
     */
    static Specification<Transaction> matching(TransactionFilter filter, ZoneId zone) {
        List<Specification<Transaction>> specs = new ArrayList<>();
        if (filter.walletId() != null) {
            specs.add(involvesWallet(filter.walletId()));
        }
        if (filter.type() != null) {
            specs.add(hasValue("type", filter.type()));
        }
        if (filter.status() != null) {
            specs.add(hasValue("status", filter.status()));
        }
        if (filter.currency() != null) {
            specs.add(hasValue("currency", filter.currency()));
        }
        if (filter.from() != null) {
            specs.add(createdAtOrAfter(filter.from().atStartOfDay(zone).toInstant()));
        }
        if (filter.to() != null) {
            specs.add(createdBefore(filter.to().plusDays(1).atStartOfDay(zone).toInstant()));
        }
        if (filter.minAmount() != null) {
            specs.add(amountAtLeast(filter.minAmount()));
        }
        if (filter.maxAmount() != null) {
            specs.add(amountAtMost(filter.maxAmount()));
        }
        return Specification.allOf(specs);
    }

    private static Specification<Transaction> involvesWallet(UUID walletId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("sourceWallet").get("id"), walletId),
                cb.equal(root.get("targetWallet").get("id"), walletId));
    }

    private static Specification<Transaction> hasValue(String attribute, Object value) {
        return (root, query, cb) -> cb.equal(root.get(attribute), value);
    }

    private static Specification<Transaction> createdAtOrAfter(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    private static Specification<Transaction> createdBefore(Instant to) {
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), to);
    }

    private static Specification<Transaction> amountAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    private static Specification<Transaction> amountAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), max);
    }
}
