package com.capstone.confms.repository;

import com.capstone.confms.entity.Ticket;
import com.capstone.confms.entity.User;
import com.capstone.confms.utils.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    Optional<Ticket> findByUserAndConferenceId(User user, Integer conferenceId);
    boolean existsByUserAndConferenceIdAndPaymentStatus(User user, Integer conferenceId, PaymentStatus paymentStatus);
    List<Ticket> findByConferenceId(Integer conferenceId);
    Optional<Ticket> findByQrCode(String qrCode);
    Optional<Ticket> findByRegistrationNumber(String registrationNumber);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.conference.id = :conferenceId")
    long countByConference_Id(@Param("conferenceId") Integer conferenceId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.conference.id = :conferenceId AND t.isCheckedIn = :isCheckedIn")
    long countCheckedInByConferenceId(@Param("conferenceId") Integer conferenceId, @Param("isCheckedIn") Boolean isCheckedIn);
}