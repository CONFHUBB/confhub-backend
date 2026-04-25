package com.capstone.confhub.service.impl;

import com.capstone.confhub.entity.PaymentHistory;
import com.capstone.confhub.entity.Ticket;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.integration.payment.VnPayIntegrationService;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.PaymentHistoryRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.service.PaymentService;
import com.capstone.confhub.service.RegistrationService;
import com.capstone.confhub.utils.VnPayUtil;
import com.capstone.confhub.utils.enums.ConferenceStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final VnPayIntegrationService vnPayIntegrationService;
    private final TicketRepository ticketRepository;
    private final RegistrationService registrationService;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final ConferenceRepository conferenceRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private static final DateTimeFormatter VNP_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String createVnPayPayment(Long amount, HttpServletRequest request) {
        String ipAddr = VnPayUtil.getIpAddress(request);
        return vnPayIntegrationService.createPaymentUrl(amount, ipAddr, null);
    }

    @Override
    @Transactional
    public String processVnPayCallback(HttpServletRequest request) {
        // ── 1. Collect all params ──
        Map<String, String> fields = new HashMap<>();
        StringBuilder rawParamsBuilder = new StringBuilder();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String name = params.nextElement();
            String value = request.getParameter(name);
            if (value != null && !value.isEmpty()) {
                fields.put(name, value);
                rawParamsBuilder.append(name).append("=").append(value).append("&");
            }
        }
        String rawParams = rawParamsBuilder.toString();

        // ── 2. Verify signature ──
        String vnpSecureHash = fields.get("vnp_SecureHash");
        Map<String, String> fieldsForVerify = new HashMap<>(fields);
        fieldsForVerify.remove("vnp_SecureHashType");
        fieldsForVerify.remove("vnp_SecureHash");
        boolean signatureValid = vnPayIntegrationService.verifyPaymentSignature(fieldsForVerify, vnpSecureHash);

        // ── 3. Extract key fields ──
        String orderInfo = fields.getOrDefault("vnp_OrderInfo", "");
        String vnpTxnRef = fields.getOrDefault("vnp_TxnRef", "");
        String vnpTransactionNo = fields.getOrDefault("vnp_TransactionNo", "");
        String vnpTransactionStatus = fields.getOrDefault("vnp_TransactionStatus", "");
        String vnpResponseCode = fields.getOrDefault("vnp_ResponseCode", "");
        String vnpBankCode = fields.getOrDefault("vnp_BankCode", "");
        String vnpPayDate = fields.getOrDefault("vnp_PayDate", "");
        long amount = 0;
        try {
            amount = Long.parseLong(fields.getOrDefault("vnp_Amount", "0")) / 100; // VNPay sends ×100
        } catch (NumberFormatException ignored) {}

        LocalDateTime payDate = null;
        if (vnpPayDate.length() >= 14) {
            try { payDate = LocalDateTime.parse(vnpPayDate.substring(0, 14), VNP_DATE_FMT); }
            catch (Exception ignored) {}
        }

        // ── 4. Parse ticketId or conferenceId from orderInfo ──
        Integer ticketId = null;
        Integer confSubId = null;
        try {
            if (orderInfo.startsWith("TICKET_")) {
                ticketId = Integer.parseInt(orderInfo.replace("TICKET_", "").trim());
            } else if (orderInfo.startsWith("CONF_SUB_")) {
                confSubId = Integer.parseInt(orderInfo.replace("CONF_SUB_", "").trim());
            }
        } catch (NumberFormatException e) {
            log.warn("Cannot parse ID from orderInfo: {}", orderInfo);
        }

        // ── 5. Determine outcome ──
        boolean isPaid = signatureValid && "00".equals(vnpTransactionStatus);
        String outcome = !signatureValid ? "INVALID" : isPaid ? "PAID" : "FAILED";

        // ── 6A. Conference Subscription Payment ──
        if (confSubId != null) {
            var confOpt = conferenceRepository.findById(confSubId);
            PaymentHistory history = new PaymentHistory();
            if (confOpt.isPresent()) {
                history.setConference(confOpt.get());
            }
            history.setVnpTxnRef(vnpTxnRef);
            history.setVnpTransactionNo(vnpTransactionNo);
            history.setVnpTransactionStatus(vnpTransactionStatus);
            history.setVnpResponseCode(vnpResponseCode);
            history.setAmount(amount);
            history.setBankCode(vnpBankCode);
            history.setPayDate(payDate);
            history.setSignatureValid(signatureValid);
            history.setOutcome(outcome);
            history.setRawParams(rawParams);
            history.setRecordedAt(LocalDateTime.now());
            paymentHistoryRepository.save(history);

            if (confOpt.isPresent()) {
                Conference conference = confOpt.get();
                if (isPaid) {
                    conference.setStatus(ConferenceStatus.SETUP);
                    conferenceRepository.save(conference);
                    log.info("VNPay PAID for conference subscription: confId={} txnRef={}", confSubId, vnpTxnRef);
                    return frontendUrl + "/conference/" + confSubId + "/subscription?status=success";
                } else {
                    conference.setStatus(ConferenceStatus.APPROVED);
                    conference.setSubscriptionPlan(null);
                    conferenceRepository.save(conference);
                    log.warn("VNPay FAILED for conference subscription: confId={} txnRef={}", confSubId, vnpTxnRef);
                    return frontendUrl + "/conference/" + confSubId + "/subscription?status=failed";
                }
            }
            log.error("VNPay callback for unknown conference: {}", confSubId);
            return frontendUrl + "/?status=invalid";
        }

        // ── 6B. Ticket Payment ──
        Ticket ticket = null;
        String conferenceId = "";
        if (ticketId != null) {
            final Integer tid = ticketId;
            var ticketOpt = ticketRepository.findById(tid);
            if (ticketOpt.isPresent()) {
                ticket = ticketOpt.get();
                conferenceId = ticket.getConference().getId().toString();
            }
        }

        // Always save audit record — even for invalid/duplicate callbacks
        PaymentHistory history = new PaymentHistory();
        history.setTicket(ticket); // may be null if ticketId unparseable
        history.setVnpTxnRef(vnpTxnRef);
        history.setVnpTransactionNo(vnpTransactionNo);
        history.setVnpTransactionStatus(vnpTransactionStatus);
        history.setVnpResponseCode(vnpResponseCode);
        history.setAmount(amount);
        history.setBankCode(vnpBankCode);
        history.setPayDate(payDate);
        history.setSignatureValid(signatureValid);
        history.setOutcome(outcome);
        history.setRawParams(rawParams);
        history.setRecordedAt(LocalDateTime.now());

        // ── 7. Update ticket/payment status ──
        if (ticket != null) {
            if (isPaid) {
                history.setPayment(registrationService.completePaymentAndGet(ticketId, vnpTxnRef, vnpTransactionNo));
                paymentHistoryRepository.save(history);
                log.info("VNPay PAID: ticket={} txnRef={} bank={}", ticketId, vnpTxnRef, vnpBankCode);
                return frontendUrl + "/conference/" + conferenceId + "/my-ticket?status=success";
            } else {
                history.setPayment(registrationService.failPaymentAndGet(ticketId, vnpTxnRef));
                paymentHistoryRepository.save(history);
                log.warn("VNPay FAILED/INVALID: ticket={} txnRef={} status={} sig={}", ticketId, vnpTxnRef, vnpTransactionStatus, signatureValid);
                return frontendUrl + "/conference/" + conferenceId + "/register?status=failed";
            }
        } else {
            // Unknown ticket — still save what we have for debugging
            paymentHistoryRepository.save(history);
            log.error("VNPay callback with unknown orderInfo: {}", orderInfo);
            return frontendUrl + "/?status=invalid";
        }
    }
}