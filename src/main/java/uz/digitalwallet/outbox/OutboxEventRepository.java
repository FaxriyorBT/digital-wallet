package uz.digitalwallet.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * SKIP LOCKED lets several application instances poll the table at the same time:
     * rows already taken by one instance are simply skipped by the others.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL
            ORDER BY created_at, id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch(int limit);

    @Modifying
    @Query("delete from OutboxEvent e where e.publishedAt < :threshold")
    int deletePublishedBefore(Instant threshold);
}
