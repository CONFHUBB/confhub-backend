package com.capstone.confhub.service.impl;

import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.Payment;
import com.capstone.confhub.entity.PaymentHistory;
import com.capstone.confhub.entity.Ticket;
import com.capstone.confhub.integration.payment.VnPayIntegrationService;
import com.capstone.confhub.repository.PaymentHistoryRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

	@Mock
	private VnPayIntegrationService vnPayIntegrationService;
	@Mock
	private TicketRepository ticketRepository;
	@Mock
	private RegistrationService registrationService;
	@Mock
	private PaymentHistoryRepository paymentHistoryRepository;

	@InjectMocks
	private PaymentServiceImpl paymentService;

	private Ticket ticket;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(paymentService, "frontendUrl", "https://frontend.local");

		Conference conference = new Conference();
		conference.setId(10);

		ticket = new Ticket();
		ticket.setId(123);
		ticket.setConference(conference);
	}

	@Test
	void shouldCreateService() {
		assertNotNull(paymentService);
	}

	@Test
	void createVnPayPaymentShouldForwardAmountAndIp() {
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
		when(request.getRemoteAddr()).thenReturn("127.0.0.1");
		when(vnPayIntegrationService.createPaymentUrl(150000L, "127.0.0.1", null)).thenReturn("https://pay.local");

		String result = paymentService.createVnPayPayment(150000L, request);

		assertEquals("https://pay.local", result);
		verify(vnPayIntegrationService).createPaymentUrl(150000L, "127.0.0.1", null);
	}

	@Test
	void createVnPayPaymentShouldPreferForwardedHeader() {
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		when(request.getHeader("X-FORWARDED-FOR")).thenReturn("10.0.0.5");
		when(vnPayIntegrationService.createPaymentUrl(100L, "10.0.0.5", null)).thenReturn("ok");

		String result = paymentService.createVnPayPayment(100L, request);

		assertEquals("ok", result);
	}

	@Test
	void processVnPayCallbackShouldReturnSuccessUrlAndPersistHistory() {
		Map<String, String> params = validPaidParams();
		HttpServletRequest request = mockRequest(params);

		when(vnPayIntegrationService.verifyPaymentSignature(anyMap(), eq("securehash"))).thenReturn(true);
		when(ticketRepository.findById(123)).thenReturn(Optional.of(ticket));

		Payment payment = new Payment();
		payment.setId(99);
		when(registrationService.completePaymentAndGet(123, "TXNREF1", "VNP123")).thenReturn(payment);
		when(paymentHistoryRepository.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String result = paymentService.processVnPayCallback(request);

		assertEquals("https://frontend.local/conference/10/my-ticket?status=success", result);
		verify(registrationService).completePaymentAndGet(123, "TXNREF1", "VNP123");
		verify(registrationService, never()).failPaymentAndGet(any(), any());

		ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
		verify(paymentHistoryRepository).save(captor.capture());
		PaymentHistory history = captor.getValue();
		assertEquals(ticket, history.getTicket());
		assertEquals("PAID", history.getOutcome());
		assertTrue(history.getSignatureValid());
		assertEquals(100000L, history.getAmount());
		assertNotNull(history.getPayDate());
		assertEquals(payment, history.getPayment());
	}

	@Test
	void processVnPayCallbackShouldFailWhenSignatureInvalid() {
		Map<String, String> params = validPaidParams();
		HttpServletRequest request = mockRequest(params);

		when(vnPayIntegrationService.verifyPaymentSignature(anyMap(), eq("securehash"))).thenReturn(false);
		when(ticketRepository.findById(123)).thenReturn(Optional.of(ticket));

		Payment failedPayment = new Payment();
		failedPayment.setId(12);
		when(registrationService.failPaymentAndGet(123, "TXNREF1")).thenReturn(failedPayment);
		when(paymentHistoryRepository.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String result = paymentService.processVnPayCallback(request);

		assertEquals("https://frontend.local/conference/10/register?status=failed", result);
		verify(registrationService).failPaymentAndGet(123, "TXNREF1");

		ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
		verify(paymentHistoryRepository).save(captor.capture());
		assertEquals("INVALID", captor.getValue().getOutcome());
		assertEquals(failedPayment, captor.getValue().getPayment());
		assertFalse(captor.getValue().getSignatureValid());
	}

	@Test
	void processVnPayCallbackShouldFailWhenStatusNotSuccessful() {
		Map<String, String> params = validPaidParams();
		params.put("vnp_TransactionStatus", "24");
		HttpServletRequest request = mockRequest(params);

		when(vnPayIntegrationService.verifyPaymentSignature(anyMap(), eq("securehash"))).thenReturn(true);
		when(ticketRepository.findById(123)).thenReturn(Optional.of(ticket));
		when(registrationService.failPaymentAndGet(123, "TXNREF1")).thenReturn(new Payment());
		when(paymentHistoryRepository.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String result = paymentService.processVnPayCallback(request);

		assertEquals("https://frontend.local/conference/10/register?status=failed", result);

		ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
		verify(paymentHistoryRepository).save(captor.capture());
		assertEquals("FAILED", captor.getValue().getOutcome());
		assertTrue(captor.getValue().getSignatureValid());
	}

	@Test
	void processVnPayCallbackShouldReturnInvalidWhenTicketIdCannotBeParsed() {
		Map<String, String> params = validPaidParams();
		params.put("vnp_OrderInfo", "BAD_VALUE");
		HttpServletRequest request = mockRequest(params);

		when(vnPayIntegrationService.verifyPaymentSignature(anyMap(), eq("securehash"))).thenReturn(true);
		when(paymentHistoryRepository.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String result = paymentService.processVnPayCallback(request);

		assertEquals("https://frontend.local/?status=invalid", result);
		verify(ticketRepository, never()).findById(any());

		ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
		verify(paymentHistoryRepository).save(captor.capture());
		assertNull(captor.getValue().getTicket());
	}

	@Test
	void processVnPayCallbackShouldReturnInvalidWhenTicketNotFound() {
		Map<String, String> params = validPaidParams();
		HttpServletRequest request = mockRequest(params);

		when(vnPayIntegrationService.verifyPaymentSignature(anyMap(), eq("securehash"))).thenReturn(true);
		when(ticketRepository.findById(123)).thenReturn(Optional.empty());
		when(paymentHistoryRepository.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String result = paymentService.processVnPayCallback(request);

		assertEquals("https://frontend.local/?status=invalid", result);
		verify(registrationService, never()).completePaymentAndGet(any(), any(), any());
		verify(registrationService, never()).failPaymentAndGet(any(), any());
	}

	@Test
	void processVnPayCallbackShouldHandleNonNumericAmountGracefully() {
		Map<String, String> params = validPaidParams();
		params.put("vnp_Amount", "abc");
		HttpServletRequest request = mockRequest(params);

		when(vnPayIntegrationService.verifyPaymentSignature(anyMap(), eq("securehash"))).thenReturn(true);
		when(ticketRepository.findById(123)).thenReturn(Optional.of(ticket));
		when(registrationService.completePaymentAndGet(123, "TXNREF1", "VNP123")).thenReturn(new Payment());
		when(paymentHistoryRepository.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentService.processVnPayCallback(request);

		ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
		verify(paymentHistoryRepository).save(captor.capture());
		assertEquals(0L, captor.getValue().getAmount());
	}

	@Test
	void processVnPayCallbackShouldHandleMissingPayDateGracefully() {
		Map<String, String> params = validPaidParams();
		params.remove("vnp_PayDate");
		HttpServletRequest request = mockRequest(params);

		when(vnPayIntegrationService.verifyPaymentSignature(anyMap(), eq("securehash"))).thenReturn(true);
		when(ticketRepository.findById(123)).thenReturn(Optional.of(ticket));
		when(registrationService.completePaymentAndGet(123, "TXNREF1", "VNP123")).thenReturn(new Payment());
		when(paymentHistoryRepository.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentService.processVnPayCallback(request);

		ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
		verify(paymentHistoryRepository).save(captor.capture());
		assertNull(captor.getValue().getPayDate());
	}

	@Test
	void processVnPayCallbackShouldExcludeSecureHashFieldsFromVerificationPayload() {
		Map<String, String> params = validPaidParams();
		params.put("vnp_SecureHashType", "SHA512");
		HttpServletRequest request = mockRequest(params);

		when(vnPayIntegrationService.verifyPaymentSignature(anyMap(), eq("securehash"))).thenReturn(true);
		when(ticketRepository.findById(123)).thenReturn(Optional.of(ticket));
		when(registrationService.completePaymentAndGet(123, "TXNREF1", "VNP123")).thenReturn(new Payment());
		when(paymentHistoryRepository.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentService.processVnPayCallback(request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass((Class) Map.class);
		verify(vnPayIntegrationService).verifyPaymentSignature(mapCaptor.capture(), eq("securehash"));
		assertFalse(mapCaptor.getValue().containsKey("vnp_SecureHash"));
		assertFalse(mapCaptor.getValue().containsKey("vnp_SecureHashType"));
	}

	@Test
	void processVnPayCallbackShouldBuildRawParamsWithAvailableFields() {
		Map<String, String> params = validPaidParams();
		HttpServletRequest request = mockRequest(params);

		when(vnPayIntegrationService.verifyPaymentSignature(anyMap(), eq("securehash"))).thenReturn(true);
		when(ticketRepository.findById(123)).thenReturn(Optional.of(ticket));
		when(registrationService.completePaymentAndGet(123, "TXNREF1", "VNP123")).thenReturn(new Payment());
		when(paymentHistoryRepository.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentService.processVnPayCallback(request);

		ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
		verify(paymentHistoryRepository).save(captor.capture());
		String rawParams = captor.getValue().getRawParams();
		assertTrue(rawParams.contains("vnp_OrderInfo=TICKET_123"));
		assertTrue(rawParams.contains("vnp_TransactionStatus=00"));
	}

	private HttpServletRequest mockRequest(Map<String, String> params) {
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Enumeration<String> names = Collections.enumeration(params.keySet());
		when(request.getParameterNames()).thenReturn(names);
		for (Map.Entry<String, String> entry : params.entrySet()) {
			when(request.getParameter(entry.getKey())).thenReturn(entry.getValue());
		}
		return request;
	}

	private Map<String, String> validPaidParams() {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("vnp_OrderInfo", "TICKET_123");
		params.put("vnp_TxnRef", "TXNREF1");
		params.put("vnp_TransactionNo", "VNP123");
		params.put("vnp_TransactionStatus", "00");
		params.put("vnp_ResponseCode", "00");
		params.put("vnp_BankCode", "NCB");
		params.put("vnp_PayDate", "20260320121212");
		params.put("vnp_Amount", "10000000");
		params.put("vnp_SecureHash", "securehash");
		return params;
	}
}

