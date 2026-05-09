package com.capstone.confhub.repository;

import com.capstone.confhub.entity.Ticket;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.utils.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    Optional<Ticket> findByUserAndConferenceId(User user, Integer conferenceId);
    Optional<Ticket> findByUser_IdAndConference_Id(Integer userId, Integer conferenceId);
    Optional<Ticket> findByUser_IdAndPaperId(Integer userId, Integer paperId);
    List<Ticket> findByUser(User user);
    boolean existsByUserAndConferenceIdAndPaymentStatus(User user, Integer conferenceId, PaymentStatus paymentStatus);
    List<Ticket> findByConferenceId(Integer conferenceId);
    Optional<Ticket> findByQrCode(String qrCode);
    Optional<Ticket> findByRegistrationNumber(String registrationNumber);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.conference.id = :conferenceId")
    long countByConference_Id(@Param("conferenceId") Integer conferenceId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.conference.id = :conferenceId AND t.isCheckedIn = :isCheckedIn")
    long countCheckedInByConferenceId(@Param("conferenceId") Integer conferenceId, @Param("isCheckedIn") Boolean isCheckedIn);

    /**
     * Paginated attendee query with optional search (name/email/regNumber) and optional status filter.
     * Pass null for status to return all payment statuses.
     * Pass null or blank string for search to skip text filtering.
     */
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.conference.id = :conferenceId
          AND (:status IS NULL OR t.paymentStatus = :status)
          AND (:search IS NULL OR :search = '' OR
               LOWER(CONCAT(t.user.firstName, ' ', t.user.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(t.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(t.registrationNumber) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.createdAt DESC
        """)
    Page<Ticket> findAttendees(
            @Param("conferenceId") Integer conferenceId,
            @Param("status") PaymentStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.conference.id = :conferenceId AND t.paymentStatus = 'COMPLETED'")
    long countPaidByConferenceId(@Param("conferenceId") Integer conferenceId);

    List<Ticket> findByConference_IdAndPaymentStatus(Integer conferenceId, PaymentStatus paymentStatus);

    /**
     * Returns the maximum numeric suffix from registration numbers matching the current year pattern (e.g. CONF2026-NNNNN).
     * Used to seed the AtomicInteger counter on startup to avoid duplicate key collisions after restarts.
     */
    @Query("SELECT MAX(CAST(SUBSTRING(t.registrationNumber, 10) AS int)) FROM Ticket t " +
           "WHERE t.registrationNumber LIKE CONCAT('CONF', :year, '-%')")
    Integer findMaxRegistrationSuffix(@Param("year") String year);
}