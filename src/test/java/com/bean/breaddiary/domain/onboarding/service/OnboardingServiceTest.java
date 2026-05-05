package com.bean.breaddiary.domain.onboarding.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture;
import com.bean.breaddiary.domain.onboarding.repository.OnboardingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnboardingServiceTest {

    private OnboardingRepository onboardingRepository;
    private OnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        onboardingRepository = mock(OnboardingRepository.class);
        onboardingService = new OnboardingService(onboardingRepository);
    }

    @Test
    void findSelectedBreadIdsReturnsUniqueIds() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID breadId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        when(onboardingRepository.findSelectedBreadIdsByUserIdAndBreadIds(userId, List.of(breadId)))
                .thenReturn(List.of(breadId, breadId));

        Set<UUID> selectedBreadIds = onboardingService.findSelectedBreadIds(userId, List.of(breadId));

        assertEquals(Set.of(breadId), selectedBreadIds);
    }

    @Test
    void replaceSelectedBreadsDeletesExistingRowsAndSavesNewRows() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Bread bread = Bread.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"))
                .stickerNumber(1)
                .name("단팥빵")
                .breadType(BreadTypeTestFixture.PASTRY)
                .imageUrl("https://cdn.bread-diary.app/breads/redbean.webp")
                .build();

        onboardingService.replaceSelectedBreads(userId, List.of(bread));

        verify(onboardingRepository).deleteAllByUserId(userId);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(onboardingRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }
}
