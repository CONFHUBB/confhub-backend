package com.capstone.confhub.service.impl;

import com.capstone.confhub.entity.DeviceToken;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.DeviceTokenRepository;
import com.capstone.confhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmPushServiceImplTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FcmPushServiceImpl fcmPushService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setEmail("user@test.com");
    }

    @Test
    void sendToUserShouldReturnWhenNoTokens() {
        when(deviceTokenRepository.findByUser_Id(1)).thenReturn(List.of());

        assertDoesNotThrow(() -> fcmPushService.sendToUser(1, "title", "body", "TYPE", "/link"));
        verify(deviceTokenRepository).findByUser_Id(1);
    }

    @Test
    void sendToUserShouldHandleNullTypeAndLinkWhenNoTokens() {
        when(deviceTokenRepository.findByUser_Id(1)).thenReturn(List.of());

        assertDoesNotThrow(() -> fcmPushService.sendToUser(1, "title", "body", null, null));
    }

    @Test
    void registerTokenShouldSkipWhenAlreadyRegistered() {
        DeviceToken existing = DeviceToken.builder().fcmToken("token-1").build();
        when(deviceTokenRepository.findByUser_IdAndFcmToken(1, "token-1")).thenReturn(Optional.of(existing));

        fcmPushService.registerToken(1, "token-1", "ANDROID");

        verify(deviceTokenRepository, never()).save(any(DeviceToken.class));
        verify(userRepository, never()).findById(anyInt());
    }

    @Test
    void registerTokenShouldSaveWhenNewToken() {
        when(deviceTokenRepository.findByUser_IdAndFcmToken(1, "token-2")).thenReturn(Optional.empty());
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        fcmPushService.registerToken(1, "token-2", "IOS");

        verify(deviceTokenRepository).save(any(DeviceToken.class));
    }

    @Test
    void registerTokenShouldDefaultDeviceTypeWhenNull() {
        when(deviceTokenRepository.findByUser_IdAndFcmToken(1, "token-3")).thenReturn(Optional.empty());
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        fcmPushService.registerToken(1, "token-3", null);

        verify(deviceTokenRepository).save(argThat(token -> "MOBILE".equals(token.getDeviceType())));
    }

    @Test
    void registerTokenShouldThrowWhenUserNotFound() {
        when(deviceTokenRepository.findByUser_IdAndFcmToken(1, "token-4")).thenReturn(Optional.empty());
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fcmPushService.registerToken(1, "token-4", "WEB"));
    }

    @Test
    void removeTokenShouldDeleteByToken() {
        fcmPushService.removeToken("token-remove");

        verify(deviceTokenRepository).deleteByFcmToken("token-remove");
    }

    @Test
    void registerMultipleTokensShouldSaveEachNewToken() {
        when(deviceTokenRepository.findByUser_IdAndFcmToken(eq(1), anyString())).thenReturn(Optional.empty());
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        fcmPushService.registerToken(1, "t1", "ANDROID");
        fcmPushService.registerToken(1, "t2", "ANDROID");
        fcmPushService.registerToken(1, "t3", "IOS");

        verify(deviceTokenRepository, times(3)).save(any(DeviceToken.class));
    }

    @Test
    void registerTokenShouldNotDeleteExistingTokens() {
        when(deviceTokenRepository.findByUser_IdAndFcmToken(1, "new-token")).thenReturn(Optional.empty());
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        fcmPushService.registerToken(1, "new-token", "ANDROID");

        verify(deviceTokenRepository, never()).delete(any(DeviceToken.class));
    }

    @Test
    void removeTokenShouldWorkForAnyString() {
        assertDoesNotThrow(() -> fcmPushService.removeToken("abc"));
        assertDoesNotThrow(() -> fcmPushService.removeToken("123"));
        assertDoesNotThrow(() -> fcmPushService.removeToken("token-with-long-value"));
    }

    @Test
    void sendToUserShouldInvokeTokenLookupEachCall() {
        when(deviceTokenRepository.findByUser_Id(anyInt())).thenReturn(List.of());

        fcmPushService.sendToUser(1, "t", "b", "TYPE", "/a");
        fcmPushService.sendToUser(2, "t", "b", "TYPE", "/b");

        verify(deviceTokenRepository, times(2)).findByUser_Id(anyInt());
    }

    @Test
    void registerTokenShouldPreserveUserReference() {
        when(deviceTokenRepository.findByUser_IdAndFcmToken(1, "keep-user")).thenReturn(Optional.empty());
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        fcmPushService.registerToken(1, "keep-user", "ANDROID");

        verify(deviceTokenRepository).save(argThat(token -> token.getUser() != null && token.getUser().getId().equals(1)));
    }
}

