package uz.digitalwallet.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import uz.digitalwallet.common.money.Currency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findAllByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

    Optional<Wallet> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByOwnerIdAndCurrencyAndStatusNot(UUID ownerId, Currency currency, WalletStatus status);

    boolean existsByNumber(String number);

    Optional<Wallet> findByNumberAndType(String number, WalletType type);

    Optional<Wallet> findByTypeAndCurrency(WalletType type, Currency currency);

    /**
     * Returns only the id, without loading the entity into the persistence context.
     * The wallet is loaded later together with the row lock, so it is read in its latest state.
     */
    @Query("select w.id from Wallet w where w.number = :number and w.type = :type")
    Optional<UUID> findIdByNumberAndType(String number, WalletType type);

    /**
     * SELECT ... FOR UPDATE: other transactions that want to change this wallet wait until we commit.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> findByIdForUpdate(UUID id);
}
