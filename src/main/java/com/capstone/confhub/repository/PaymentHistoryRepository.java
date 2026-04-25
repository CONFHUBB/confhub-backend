package com.capstone.confhub.repository;

import com.capstone.confhub.entity.PaymentHistory;
import com.capstone.confhub.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findByTicketOrderByRecordedAtDesc(Ticket ticket);
    List<PaymentHistory> findByTicketInOrderByRecordedAtDesc(List<Ticket> tickets);
    List<PaymentHistory> findByVnpTxnRef(String vnpTxnRef);

    /** Find subscription payment history for a specific conference */
    List<PaymentHistory> findByConference_IdOrderByRecordedAtDesc(Integer conferenceId);

    /**
     * Find all subscription payment history where the user is the conference chair.
     * This allows subscription payments to show up in "My Payments".
     */
    @Query("SELECT ph FROM PaymentHistory ph " +
           "JOIN ph.conference c " +
           "JOIN ConferenceUserTrack cut ON cut.conference = c AND cut.user.id = :userId " +
           "WHERE ph.ticket IS NULL AND cut.assignedRole = com.capstone.confhub.utils.enums.ConferenceTrackRole.CONFERENCE_CHAIR " +
           "ORDER BY ph.recordedAt DESC")
    List<PaymentHistory> findSubscriptionPaymentsByChairUserId(@Param("userId") Integer userId);
}
