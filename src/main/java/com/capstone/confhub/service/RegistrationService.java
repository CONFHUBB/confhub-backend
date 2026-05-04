package com.capstone.confhub.service;

import com.capstone.confhub.dto.response.CheckInResponse;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.dto.request.RegistrationRequest;
import com.capstone.confhub.dto.response.RegistrationResponse;
import com.capstone.confhub.dto.response.TicketResponse;
import com.capstone.confhub.entity.Payment;

import java.util.List;

public interface RegistrationService {
    RegistrationResponse register(Integer conferenceId, Integer userId,
                                  RegistrationRequest request, String clientIp);
    TicketResponse getMyTicket(Integer conferenceId, Integer userId);
    List<TicketResponse> getMyTickets(Integer userId);
    List<TicketResponse> getAttendees(Integer conferenceId);
    PagedResponse<TicketResponse> getAttendeesPageable(Integer conferenceId, int page, int size, String search, String status);
    void completePayment(Integer ticketId, String vnpTxnRef, String providerTransactionId);
    Payment completePaymentAndGet(Integer ticketId, String vnpTxnRef, String providerTransactionId);
    void failPayment(Integer ticketId, String vnpTxnRef);
    Payment failPaymentAndGet(Integer ticketId, String vnpTxnRef);
    CheckInResponse checkIn(String code);
    CheckInResponse checkInForConference(String code, Integer conferenceId, Integer actorUserId);
    CheckInResponse checkInForMobileChair(String code, Integer actorUserId);
    RegistrationResponse retryPayment(Integer conferenceId, Integer userId, String clientIp);
    void cancelPendingTicket(Integer conferenceId, Integer userId);
    void refundTicket(Integer conferenceId, Integer ticketId);
}
