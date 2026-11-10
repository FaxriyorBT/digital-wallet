package uz.digitalwallet.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.web.PageResponse;
import uz.digitalwallet.transaction.dto.TransactionFilter;
import uz.digitalwallet.transaction.dto.TransactionResponse;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionQueryService {

    private final TransactionRepository transactions;
    private final TransactionMapper transactionMapper;
    private final Clock clock;

    public PageResponse<TransactionResponse> findForUser(UUID userId, TransactionFilter filter, Pageable pageable) {
        Specification<Transaction> spec = TransactionSpecifications.visibleTo(userId)
                .and(TransactionSpecifications.matching(filter, clock.getZone()));
        return PageResponse.from(transactions.findAll(spec, pageable).map(transactionMapper::toResponse));
    }

    public TransactionResponse getForUser(UUID userId, UUID transactionId) {
        return transactions.findWithWalletsById(transactionId)
                .filter(transaction -> transaction.isVisibleTo(userId))
                .map(transactionMapper::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    public PageResponse<TransactionResponse> search(TransactionFilter filter, Pageable pageable) {
        Specification<Transaction> spec = TransactionSpecifications.matching(filter, clock.getZone());
        return PageResponse.from(transactions.findAll(spec, pageable).map(transactionMapper::toResponse));
    }
}
