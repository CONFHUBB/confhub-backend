package com.capstone.confms.service;

import com.capstone.confms.dto.response.CheckInResponse;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.dto.request.RegistrationRequest;
import com.capstone.confms.dto.response.RegistrationResponse;
import com.capstone.confms.dto.response.TicketResponse;
import com.capstone.confms.entity.Payment;

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
    RegistrationResponse retryPayment(Integer conferenceId, Integer userId, String clientIp);
}
