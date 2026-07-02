package com.bean.breaddiary.domain.user.service;

import com.bean.breaddiary.domain.auth.client.TossAuthClient;
import com.bean.breaddiary.domain.auth.dto.request.TossWebhookRequest;
import com.bean.breaddiary.domain.auth.dto.response.TossWebhookResponse;
import com.bean.breaddiary.domain.auth.entity.TossWebhookEventType;
import com.bean.breaddiary.domain.auth.service.UserSessionService;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import com.bean.breaddiary.domain.bread.repository.BreadRepository;
import com.bean.breaddiary.domain.breadrecord.repository.BreadRecordRepository;
import com.bean.breaddiary.domain.onboarding.service.OnboardingService;
import com.bean.breaddiary.domain.user.dto.response.UserWithdrawalResponse;
import com.bean.breaddiary.domain.user.entity.User;
import com.bean.breaddiary.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserWithdrawalServiceTest {

    private UserService userService;
    private UserWithdrawalService userWithdrawalService;
    private UserRepository userRepository;
    private BreadRepository breadRepository;
    private BreadRecordRepository breadRecordRepository;
    private UserSessionService userSessionService;
    private TossAuthClient tossAuthClient;
    private OnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        breadRepository = mock(BreadRepository.class);
        breadRecordRepository = mock(BreadRecordRepository.class);
        userSessionService = mock(UserSessionService.class);
        tossAuthClient = mock(TossAuthClient.class);
        onboardingService = mock(OnboardingService.class);
        userService = mock(UserService.class);
        userWithdrawalService = new UserWithdrawalService(
                userService,
                userRepository,
                breadRepository,
                breadRecordRepository,
                userSessionService,
                tossAuthClient,
                onboardingService
        );
        ReflectionTestUtils.setField(userWithdrawalService, "tossWebhookSecret", "webhook-secret");
    }

    @Test
    void withdrawCurrentUserUnlinksTossAndHardDeletesLocalData() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        User user = user(userId, "toss-user-key-12345678");
        Bread breadToKeep = bread(userId, 1, "keep-bread");
        Bread breadToDelete = bread(userId, 2, "delete-bread");

        when(userService.getUserById(userId)).thenReturn(user);
        when(breadRepository.findAllByCreatedBy(userId)).thenReturn(List.of(breadToKeep, breadToDelete));
        when(breadRecordRepository.existsByBread(breadToKeep)).thenReturn(true);
        when(breadRecordRepository.existsByBread(breadToDelete)).thenReturn(false);
        when(tossAuthClient.refreshAccessToken("stored-toss-refresh-token"))
                .thenReturn(new TossAuthClient.TossGenerateTokenSuccess(
                        "new-toss-access-token",
                        "new-toss-refresh-token",
                        "Bearer",
                        3600L,
                        "profile"
                ));

        UserWithdrawalResponse response = userWithdrawalService.withdrawCurrentUser(userId);

        assertTrue(response.isWithdrawn());
        assertNull(breadToKeep.getCreatedBy());
        assertEquals("new-toss-refresh-token", user.getTossRefreshToken());
        verify(tossAuthClient).refreshAccessToken("stored-toss-refresh-token");
        verify(tossAuthClient).unlinkByUserKey("new-toss-access-token", "toss-user-key-12345678");
        verify(breadRecordRepository).hardDeleteAllByUserId(userId);
        verify(breadRepository).delete(breadToDelete);
        verify(onboardingService).deleteAllByUserId(userId);
        verify(userSessionService).deleteAllSessions(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void withdrawCurrentUserFailsWhenTossRefreshTokenIsMissing() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        User user = user(userId, "toss-user-key-without-refresh");
        user.updateTossRefreshToken(null);

        when(userService.getUserById(userId)).thenReturn(user);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userWithdrawalService.withdrawCurrentUser(userId)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        verifyNoInteractions(breadRepository, breadRecordRepository, userSessionService, onboardingService);
    }

    @Test
    void handleTossWebhookUnlinkHardDeletesUserData() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
        User user = user(userId, "toss-user-key-unlink");

        when(userRepository.findByTossUserKey("toss-user-key-unlink"))
                .thenReturn(Optional.of(user));

        when(breadRepository.findAllByCreatedBy(userId)).thenReturn(List.of());

        TossWebhookResponse response = userWithdrawalService.handleTossWebhook(
                "webhook-secret",
                new TossWebhookRequest("toss-user-key-unlink", TossWebhookEventType.UNLINK.name())
        );

        assertTrue(response.isProcessed());
        assertEquals(TossWebhookEventType.UNLINK.name(), response.getReferrer());
        verify(breadRecordRepository).hardDeleteAllByUserId(userId);
        verify(onboardingService).deleteAllByUserId(userId);
        verify(userSessionService).deleteAllSessions(userId);
        verify(userRepository).delete(user);
        verifyNoInteractions(tossAuthClient);
    }

    @Test
    void handleTossWebhookWithdrawalHardDeletesUserData() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
        User user = user(userId, "toss-user-key-withdrawal");

        when(userRepository.findByTossUserKey("toss-user-key-withdrawal"))
                .thenReturn(Optional.of(user));
        when(breadRepository.findAllByCreatedBy(userId)).thenReturn(List.of());

        TossWebhookResponse response = userWithdrawalService.handleTossWebhook(
                "webhook-secret",
                new TossWebhookRequest("toss-user-key-withdrawal", TossWebhookEventType.WITHDRAWAL_TOSS.name())
        );

        assertTrue(response.isProcessed());
        assertEquals(TossWebhookEventType.WITHDRAWAL_TOSS.name(), response.getReferrer());
        verify(breadRecordRepository).hardDeleteAllByUserId(userId);
        verify(onboardingService).deleteAllByUserId(userId);
        verify(userSessionService).deleteAllSessions(userId);
        verify(userRepository).delete(user);
        verifyNoInteractions(tossAuthClient);
    }

    @Test
    void handleTossWebhookRejectsInvalidSecret() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userWithdrawalService.handleTossWebhook(
                        "wrong-secret",
                        new TossWebhookRequest("toss-user-key", TossWebhookEventType.UNLINK.name())
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(userRepository, breadRepository, breadRecordRepository, userSessionService, tossAuthClient, onboardingService);
    }

    private User user(UUID userId, String tossUserKey) {
        return User.builder()
                .id(userId)
                .tossUserKey(tossUserKey)
                .tossRefreshToken("stored-toss-refresh-token")
                .nickname("bread-lover")
                .email("bread@toss.im")
                .build();
    }

    private Bread bread(UUID createdBy, int stickerNumber, String name) {
        return Bread.builder()
                .id(UUID.randomUUID())
                .stickerNumber(stickerNumber)
                .name(name)
                .breadType(PASTRY)
                .imageUrl("https://cdn.bread-diary.app/catalog/default_user_bread.webp")
                .createdBy(createdBy)
                .build();
    }
}
