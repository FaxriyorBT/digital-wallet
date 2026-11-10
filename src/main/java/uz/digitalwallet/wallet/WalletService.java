package uz.digitalwallet.wallet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.user.UserRepository;
import uz.digitalwallet.wallet.dto.WalletLookupResponse;
import uz.digitalwallet.wallet.dto.WalletResponse;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository wallets;
    private final UserRepository users;
    private final WalletNumberGenerator numberGenerator;
    private final WalletMapper walletMapper;

    @Transactional
    public WalletResponse open(UUID userId, Currency currency) {
        if (wallets.existsByOwnerIdAndCurrencyAndStatusNot(userId, currency, WalletStatus.CLOSED)) {
            throw new BusinessException(ErrorCode.WALLET_ALREADY_EXISTS);
        }
        Wallet wallet = Wallet.open(users.getReferenceById(userId), currency, numberGenerator.generate());
        try {
            wallets.saveAndFlush(wallet);
        } catch (DataIntegrityViolationException e) {
            // two parallel requests passed the check above, the partial unique index stopped the second one
            throw new BusinessException(ErrorCode.WALLET_ALREADY_EXISTS);
        }
        log.info("Wallet {} ({}) opened for user {}", wallet.getNumber(), currency, userId);
        return walletMapper.toResponse(wallet);
    }

    public List<WalletResponse> findAll(UUID userId) {
        return wallets.findAllByOwnerIdOrderByCreatedAtAsc(userId).stream()
                .map(walletMapper::toResponse)
                .toList();
    }

    public WalletResponse get(UUID userId, UUID walletId) {
        return walletMapper.toResponse(findOwned(userId, walletId));
    }

    /**
     * No row lock here: closing races with top-ups only rarely, so the @Version check is enough.
     * If a top-up commits first, this update fails with an optimistic locking error and the client retries.
     */
    @Transactional
    public WalletResponse close(UUID userId, UUID walletId) {
        Wallet wallet = findOwned(userId, walletId);
        wallet.close();
        wallets.flush();
        return walletMapper.toResponse(wallet);
    }

    public WalletLookupResponse lookup(String number) {
        Wallet wallet = wallets.findByNumberAndType(number, WalletType.PERSONAL)
                .filter(Wallet::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        return new WalletLookupResponse(wallet.getNumber(), wallet.getCurrency(),
                maskName(wallet.getOwner().getFullName()));
    }

    @Transactional
    public WalletResponse changeStatus(UUID walletId, WalletStatus status) {
        Wallet wallet = wallets.findById(walletId)
                .filter(Wallet::isPersonal)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        wallet.changeStatus(status);
        wallets.flush();
        log.info("Wallet {} status changed to {}", wallet.getNumber(), status);
        return walletMapper.toResponse(wallet);
    }

    private Wallet findOwned(UUID userId, UUID walletId) {
        // someone else's wallet gets the same 404 as a missing one, so ids cannot be probed
        return wallets.findByIdAndOwnerId(walletId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
    }

    static String maskName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
