package uz.digitalwallet.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("""
            select u from User u
            where u.status in :statuses
              and (lower(u.email) like :pattern or lower(u.fullName) like :pattern or u.phone like :pattern)
            """)
    Page<User> search(String pattern, Collection<UserStatus> statuses, Pageable pageable);
}
