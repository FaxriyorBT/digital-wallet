package uz.digitalwallet.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.web.PageResponse;
import uz.digitalwallet.ledger.dto.StatementEntryResponse;
import uz.digitalwallet.transaction.Transaction;
import uz.digitalwallet.wallet.Wallet;
import uz.digitalwallet.wallet.WalletRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatementService {

    private static final int DEFAULT_PERIOD_DAYS = 30;

    private final WalletRepository wallets;
    private final LedgerEntryRepository entries;
    private final Clock clock;

    public PageResponse<StatementEntryResponse> getStatement(UUID userId, UUID walletId, LocalDate from, LocalDate to,
                                                             Pageable pageable) {
        if (!wallets.existsByIdAndOwnerId(walletId, userId)) {
            throw new BusinessException(ErrorCode.WALLET_NOT_FOUND);
        }
        LocalDate end = to != null ? to : LocalDate.now(clock);
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_PERIOD_DAYS);
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "'from' must not be after 'to'");
        }

        ZoneId zone = clock.getZone();
        Instant periodStart = start.atStartOfDay(zone).toInstant();
        Instant periodEnd = end.plusDays(1).atStartOfDay(zone).toInstant();
        return PageResponse.from(entries.findStatement(walletId, periodStart, periodEnd, pageable).map(this::toResponse));
    }

    private StatementEntryResponse toResponse(LedgerEntry entry) {
        Transaction transaction = entry.getTransaction();
        Wallet counterparty = entry.getDirection() == EntryDirection.DEBIT
                ? transaction.getTargetWallet()
                : transaction.getSourceWallet();
        String counterpartyNumber = counterparty != null && counterparty.isPersonal() ? counterparty.getNumber() : null;
        return new StatementEntryResponse(transaction.getId(), transaction.getType(), entry.getDirection(),
                entry.getAmount(), entry.getBalanceAfter(), counterpartyNumber, transaction.getDescription(),
                entry.getCreatedAt());
    }
}
