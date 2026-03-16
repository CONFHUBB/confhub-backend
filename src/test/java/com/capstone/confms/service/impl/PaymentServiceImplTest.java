// package com.capstone.confms.service.impl;

// import com.capstone.confms.integration.payment.VnPayIntegrationService;
// import com.capstone.confms.repository.PaymentRepository;
// import jakarta.servlet.http.HttpServletRequest;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.util.Collections;
// import java.util.Enumeration;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.mockito.ArgumentMatchers.anyLong;
// import static org.mockito.ArgumentMatchers.nullable;
// import static org.mockito.Mockito.when;

// @ExtendWith(MockitoExtension.class)
// class PaymentServiceImplTest {

//     @Mock
//     private VnPayIntegrationService vnPayIntegrationService;

//     @Mock
//     private PaymentRepository paymentRepository;

//     @InjectMocks
//     private PaymentServiceImpl paymentService;

//     @Test
//     void shouldCreateService() {
//         assertNotNull(paymentService);
//     }

//     @Test
//     void createVnPayPaymentShouldReturnUrl() {
//         HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
//         when(vnPayIntegrationService.createPaymentUrl(anyLong(), nullable(String.class))).thenReturn("https://pay-url");

//         String result = paymentService.createVnPayPayment(100000L, request);

//         assertEquals("https://pay-url", result);
//     }

//     @Test
//     void processVnPayCallbackShouldReturnSuccessUrl() {
//         HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
//         Enumeration<String> names = Collections.enumeration(java.util.List.of(
//                 "vnp_OrderInfo", "vnp_TransactionStatus", "vnp_SecureHash"
//         ));

//         when(request.getParameterNames()).thenReturn(names);
//         when(request.getParameter("vnp_OrderInfo")).thenReturn("123");
//         when(request.getParameter("vnp_TransactionStatus")).thenReturn("00");
//         when(request.getParameter("vnp_SecureHash")).thenReturn("sig");
//         when(vnPayIntegrationService.verifyPaymentSignature(org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.eq("sig")))
//                 .thenReturn(true);

//         String result = paymentService.processVnPayCallback(request);

//         assertEquals("https://bgss.onrender.com/payment-details/123?status=success", result);
//     }
// }
