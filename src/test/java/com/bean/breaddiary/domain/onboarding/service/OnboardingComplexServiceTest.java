package com.bean.breaddiary.domain.onboarding.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.service.BreadService;
import com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture;
import com.bean.breaddiary.domain.onboarding.entity.OnboardingBreadCatalog;
import com.bean.breaddiary.domain.onboarding.dto.mapper.OnboardingMapper;
import com.bean.breaddiary.domain.onboarding.dto.response.OnboardingBreadListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnboardingComplexServiceTest {

    private OnboardingService onboardingService;
    private BreadService breadService;
    private OnboardingMapper onboardingMapper;
    private OnboardingComplexService onboardingComplexService;

    @BeforeEach
    void setUp() {
        onboardingService = mock(OnboardingService.class);
        breadService = mock(BreadService.class);
        onboardingMapper = Mappers.getMapper(OnboardingMapper.class);
        onboardingComplexService = new OnboardingComplexService(onboardingService, breadService, onboardingMapper);
    }

    @Test
    void getOnboardingBreadsReturnsFixedThirtyItemsInCatalogOrder() {
        when(breadService.findAllSystemCatalogBreadsByNames(OnboardingBreadCatalog.catalogNames()))
                .thenReturn(createAllOnboardingBreads());

        OnboardingBreadListResponse response = onboardingComplexService.getOnboardingBreads();

        assertEquals(30, response.getItems().size());
        assertEquals("생식빵", response.getItems().get(0).getName());
        assertEquals("마늘바게트", response.getItems().get(12).getName());
        assertEquals(true, response.getItems().get(0).getImageUrl().contains("생식빵"));
    }

    @Test
    void synchronizeSelectedBreadsReplacesSavedRowsWhenIdsAreAllowed() {
        List<Bread> onboardingBreads = createAllOnboardingBreads();
        Bread firstBread = onboardingBreads.get(0);
        Bread secondBread = onboardingBreads.get(1);
        when(breadService.findAllSystemCatalogBreadsByNames(OnboardingBreadCatalog.catalogNames()))
                .thenReturn(onboardingBreads);

        onboardingComplexService.synchronizeSelectedBreads(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                List.of(secondBread.getId(), firstBread.getId(), secondBread.getId())
        );

        verify(onboardingService).replaceSelectedBreads(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                List.of(secondBread, firstBread)
        );
    }

    @Test
    void synchronizeSelectedBreadsRejectsIdOutsideOnboardingCatalog() {
        when(breadService.findAllSystemCatalogBreadsByNames(OnboardingBreadCatalog.catalogNames()))
                .thenReturn(createAllOnboardingBreads());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> onboardingComplexService.synchronizeSelectedBreads(
                        UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                        List.of(UUID.fromString("550e8400-e29b-41d4-a716-446655449999"))
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(onboardingService, never()).replaceSelectedBreads(any(), any());
    }

    @Test
    void synchronizeSelectedBreadsRejectsNullId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> onboardingComplexService.synchronizeSelectedBreads(
                        UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                        Arrays.asList((UUID) null)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(onboardingService, never()).replaceSelectedBreads(any(), any());
        verify(breadService, never()).findAllSystemCatalogBreadsByNames(any());
    }

    @Test
    void getOnboardingBreadsFailsWhenCatalogBreadIsMissing() {
        when(breadService.findAllSystemCatalogBreadsByNames(OnboardingBreadCatalog.catalogNames()))
                .thenReturn(createAllOnboardingBreads().subList(0, 29));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> onboardingComplexService.getOnboardingBreads()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

    private List<Bread> createAllOnboardingBreads() {
        return Arrays.stream(OnboardingBreadCatalog.values())
                .map(this::createBread)
                .collect(Collectors.toList());
    }

    private Bread createBread(OnboardingBreadCatalog catalog) {
        int stickerNumber = catalog.ordinal() + 1;
        return Bread.builder()
                .id(UUID.nameUUIDFromBytes(catalog.getCatalogName().getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .stickerNumber(stickerNumber)
                .name(catalog.getCatalogName())
                .breadType(BreadTypeTestFixture.PASTRY)
                .imageUrl("https://du4zizlgiw14n.cloudfront.net/images/%03d_%s.png".formatted(stickerNumber, catalog.getCatalogName()))
                .build();
    }
}
