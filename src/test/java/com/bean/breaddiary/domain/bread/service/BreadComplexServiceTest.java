package com.bean.breaddiary.domain.bread.service;

import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadCatalogListResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileRecordResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileStatsResponse;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import com.bean.breaddiary.domain.breadtype.service.BreadTypeService;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCatalogStats;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import com.bean.breaddiary.domain.breadrecord.service.BreadRecordService;
import com.bean.breaddiary.domain.onboarding.service.OnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture.BAGEL;
import static com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture.PASTRY;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BreadComplexServiceTest {

    private BreadService breadService;
    private BreadTypeService breadTypeService;
    private BreadRecordService breadRecordService;
    private OnboardingService onboardingService;
    private BreadComplexService breadComplexService;

    @BeforeEach
    void setUp() {
        breadService = mock(BreadService.class);
        breadTypeService = mock(BreadTypeService.class);
        breadRecordService = mock(BreadRecordService.class);
        onboardingService = mock(OnboardingService.class);
        breadComplexService = new BreadComplexService(breadService, breadTypeService, breadRecordService, onboardingService);
    }

    @Test
    void autocompleteBreadsCombinesBreadSearchEatCountsAndOnboardingSelections() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID breadId = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");
        Bread bread = createBread(breadId, 6, "크루아상", PASTRY);
        BreadService.AutocompleteSlice autocompleteSlice = new BreadService.AutocompleteSlice(List.of(bread), "6", true);
        BreadAutocompleteResponse expected = new BreadAutocompleteResponse(List.of(), "6", true);

        when(breadService.searchAutocompleteBreads("크루", "3", 10, userId)).thenReturn(autocompleteSlice);
        when(breadRecordService.countActiveRecordsByBreadIds(userId, List.of(breadId)))
                .thenReturn(Map.of(breadId, 5L));
        when(onboardingService.findSelectedBreadIds(userId, List.of(breadId)))
                .thenReturn(Set.of(breadId));
        when(breadService.createAutocompleteResponse(autocompleteSlice, Map.of(breadId, 5L), Set.of(breadId)))
                .thenReturn(expected);

        BreadAutocompleteResponse actual = breadComplexService.autocompleteBreads("크루", "3", 10, userId);

        assertSame(expected, actual);
        verify(breadService).searchAutocompleteBreads("크루", "3", 10, userId);
        verify(breadRecordService).countActiveRecordsByBreadIds(userId, List.of(breadId));
        verify(onboardingService).findSelectedBreadIds(userId, List.of(breadId));
        verify(breadService).createAutocompleteResponse(autocompleteSlice, Map.of(breadId, 5L), Set.of(breadId));
    }

    @Test
    void autocompleteBreadsSkipsUserLookupsForAnonymousUser() {
        BreadService.AutocompleteSlice autocompleteSlice = new BreadService.AutocompleteSlice(List.of(), null, false);
        BreadAutocompleteResponse expected = new BreadAutocompleteResponse(List.of(), null, false);

        when(breadService.searchAutocompleteBreads(null, null, null, null)).thenReturn(autocompleteSlice);
        when(breadService.createAutocompleteResponse(autocompleteSlice, Map.of(), Set.of())).thenReturn(expected);

        BreadAutocompleteResponse actual = breadComplexService.autocompleteBreads(null, null, null, null);

        assertSame(expected, actual);
        verify(breadService).searchAutocompleteBreads(null, null, null, null);
        verifyNoInteractions(breadRecordService, onboardingService);
        verify(breadService).createAutocompleteResponse(autocompleteSlice, Map.of(), Set.of());
    }

    @Test
    void getBreadCatalogSortsRecordedThenOnboardingThenUncollectedForAllFilter() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Bread recordedBread = createBread(UUID.fromString("10000000-0000-0000-0000-000000000001"), 3, "크루아상", PASTRY);
        Bread onboardingBread = createBread(UUID.fromString("10000000-0000-0000-0000-000000000002"), 1, "단팥빵", PASTRY);
        Bread uncollectedBread = createBread(UUID.fromString("10000000-0000-0000-0000-000000000003"), 2, "베이글", BAGEL);
        Map<UUID, BreadRecordCatalogStats> statsMap = Map.of(
                recordedBread.getId(),
                new BreadRecordCatalogStats(recordedBread.getId(), 1L, 4.5, null, LocalDate.of(2026, 3, 14))
        );
        Set<UUID> onboardingSelectedBreadIds = Set.of(onboardingBread.getId());
        BreadCatalogListResponse expected = new BreadCatalogListResponse(List.of(), null, false, 3L);

        when(breadService.findCatalogCandidates(null, null, userId))
                .thenReturn(List.of(uncollectedBread, onboardingBread, recordedBread));
        when(breadRecordService.findCatalogStatsByBreadIds(userId, List.of(uncollectedBread.getId(), onboardingBread.getId(), recordedBread.getId())))
                .thenReturn(statsMap);
        when(onboardingService.findSelectedBreadIds(userId, List.of(uncollectedBread.getId(), onboardingBread.getId(), recordedBread.getId())))
                .thenReturn(onboardingSelectedBreadIds);
        when(breadService.createCatalogListResponse(
                List.of(recordedBread, onboardingBread, uncollectedBread),
                statsMap,
                onboardingSelectedBreadIds,
                null,
                false,
                3L
        )).thenReturn(expected);

        BreadCatalogListResponse actual = breadComplexService.getBreadCatalog(
                "sticker_number",
                "all",
                null,
                null,
                null,
                20,
                userId
        );

        assertSame(expected, actual);
        verify(breadService).createCatalogListResponse(
                List.of(recordedBread, onboardingBread, uncollectedBread),
                statsMap,
                onboardingSelectedBreadIds,
                null,
                false,
                3L
        );
    }


    @Test
    void getBreadCatalogLatestSortKeepsOnboardingOnlyBreadsAfterRecordedBreads() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Bread recordedBread = createBread(UUID.fromString("10000000-0000-0000-0000-000000000011"), 3, "크루아상", PASTRY);
        Bread onboardingBread = createBread(UUID.fromString("10000000-0000-0000-0000-000000000012"), 1, "단팥빵", PASTRY);
        Bread uncollectedBread = createBread(UUID.fromString("10000000-0000-0000-0000-000000000013"), 2, "베이글", BAGEL);
        Map<UUID, BreadRecordCatalogStats> statsMap = Map.of(
                recordedBread.getId(),
                new BreadRecordCatalogStats(recordedBread.getId(), 1L, 4.5, null, LocalDate.of(2026, 3, 14))
        );
        Set<UUID> onboardingSelectedBreadIds = Set.of(onboardingBread.getId());
        BreadCatalogListResponse expected = new BreadCatalogListResponse(List.of(), null, false, 3L);

        when(breadService.findCatalogCandidates(null, null, userId))
                .thenReturn(List.of(uncollectedBread, onboardingBread, recordedBread));
        when(breadRecordService.findCatalogStatsByBreadIds(userId, List.of(uncollectedBread.getId(), onboardingBread.getId(), recordedBread.getId())))
                .thenReturn(statsMap);
        when(onboardingService.findSelectedBreadIds(userId, List.of(uncollectedBread.getId(), onboardingBread.getId(), recordedBread.getId())))
                .thenReturn(onboardingSelectedBreadIds);
        when(breadService.createCatalogListResponse(
                List.of(recordedBread, onboardingBread),
                statsMap,
                onboardingSelectedBreadIds,
                null,
                false,
                3L
        )).thenReturn(expected);

        BreadCatalogListResponse actual = breadComplexService.getBreadCatalog(
                "latest",
                "all",
                null,
                null,
                null,
                20,
                userId
        );

        assertSame(expected, actual);
        verify(breadService).createCatalogListResponse(
                List.of(recordedBread, onboardingBread),
                statsMap,
                onboardingSelectedBreadIds,
                null,
                false,
                3L
        );
    }


    @Test
    void getBreadCatalogRatingSortKeepsOnboardingOnlyBreadsAfterRecordedBreads() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Bread highRatedBread = createBread(UUID.fromString("10000000-0000-0000-0000-000000000021"), 3, "크루아상", PASTRY);
        Bread onboardingBread = createBread(UUID.fromString("10000000-0000-0000-0000-000000000022"), 1, "단팥빵", PASTRY);
        Bread lowerRatedBread = createBread(UUID.fromString("10000000-0000-0000-0000-000000000023"), 2, "베이글", BAGEL);
        Map<UUID, BreadRecordCatalogStats> statsMap = Map.of(
                highRatedBread.getId(),
                new BreadRecordCatalogStats(highRatedBread.getId(), 1L, 4.8, null, LocalDate.of(2026, 3, 14)),
                lowerRatedBread.getId(),
                new BreadRecordCatalogStats(lowerRatedBread.getId(), 1L, 4.2, null, LocalDate.of(2026, 3, 12))
        );
        Set<UUID> onboardingSelectedBreadIds = Set.of(onboardingBread.getId());
        BreadCatalogListResponse expected = new BreadCatalogListResponse(List.of(), null, false, 3L);

        when(breadService.findCatalogCandidates(null, null, userId))
                .thenReturn(List.of(onboardingBread, lowerRatedBread, highRatedBread));
        when(breadRecordService.findCatalogStatsByBreadIds(userId, List.of(onboardingBread.getId(), lowerRatedBread.getId(), highRatedBread.getId())))
                .thenReturn(statsMap);
        when(onboardingService.findSelectedBreadIds(userId, List.of(onboardingBread.getId(), lowerRatedBread.getId(), highRatedBread.getId())))
                .thenReturn(onboardingSelectedBreadIds);
        when(breadService.createCatalogListResponse(
                List.of(highRatedBread, lowerRatedBread, onboardingBread),
                statsMap,
                onboardingSelectedBreadIds,
                null,
                false,
                3L
        )).thenReturn(expected);

        BreadCatalogListResponse actual = breadComplexService.getBreadCatalog(
                "rating",
                "all",
                null,
                null,
                null,
                20,
                userId
        );

        assertSame(expected, actual);
        verify(breadService).createCatalogListResponse(
                List.of(highRatedBread, lowerRatedBread, onboardingBread),
                statsMap,
                onboardingSelectedBreadIds,
                null,
                false,
                3L
        );
    }

    @Test
    void getBreadCatalogCollectedFilterIncludesOnboardingSelectedBreadWithoutRecords() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Bread onboardingBread = createBread(UUID.fromString("10000000-0000-0000-0000-000000000001"), 1, "단팥빵", PASTRY);
        Bread uncollectedBread = createBread(UUID.fromString("10000000-0000-0000-0000-000000000002"), 2, "베이글", BAGEL);
        Set<UUID> onboardingSelectedBreadIds = Set.of(onboardingBread.getId());
        BreadCatalogListResponse expected = new BreadCatalogListResponse(List.of(), null, false, 1L);

        when(breadService.findCatalogCandidates(null, null, userId)).thenReturn(List.of(onboardingBread, uncollectedBread));
        when(breadRecordService.findCatalogStatsByBreadIds(userId, List.of(onboardingBread.getId(), uncollectedBread.getId())))
                .thenReturn(Map.of());
        when(onboardingService.findSelectedBreadIds(userId, List.of(onboardingBread.getId(), uncollectedBread.getId())))
                .thenReturn(onboardingSelectedBreadIds);
        when(breadService.createCatalogListResponse(
                List.of(onboardingBread),
                Map.of(),
                onboardingSelectedBreadIds,
                null,
                false,
                1L
        )).thenReturn(expected);

        BreadCatalogListResponse actual = breadComplexService.getBreadCatalog(
                "sticker_number",
                "collected",
                null,
                null,
                null,
                20,
                userId
        );

        assertSame(expected, actual);
    }

    @Test
    void getBreadProfileCombinesBreadRecordsAndOnboardingSelection() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID breadId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        Bread bread = createBread(breadId, 6, "크루아상", PASTRY);
        BreadRecord record = BreadRecord.builder()
                .userId(userId)
                .bread(bread)
                .photoUrl("https://cdn.bread-diary.app/bread-photos/record.webp")
                .rating(5)
                .eatenDate(LocalDate.of(2026, 3, 14))
                .build();
        BreadProfileStatsResponse stats = new BreadProfileStatsResponse(
                1L,
                5.0,
                LocalDate.of(2026, 3, 14).atStartOfDay()
        );
        List<BreadProfileRecordResponse> recordResponses = List.of(new BreadProfileRecordResponse());
        BreadProfileResponse expected = new BreadProfileResponse();

        when(breadService.getBreadById(breadId, userId)).thenReturn(bread);
        when(breadRecordService.findActiveRecordsByUserAndBread(userId, bread))
                .thenReturn(List.of(record));
        when(breadRecordService.createProfileStats(List.of(record))).thenReturn(stats);
        when(breadRecordService.createProfileRecordResponses(List.of(record))).thenReturn(recordResponses);
        when(onboardingService.isSelected(userId, breadId)).thenReturn(true);
        when(breadService.createProfileResponse(bread, stats, recordResponses, true)).thenReturn(expected);

        BreadProfileResponse actual = breadComplexService.getBreadProfile(breadId, userId);

        assertSame(expected, actual);
        verify(breadService).createProfileResponse(bread, stats, recordResponses, true);
    }

    private Bread createBread(UUID id, Integer stickerNumber, String name, BreadType breadType) {
        return Bread.builder()
                .id(id)
                .stickerNumber(stickerNumber)
                .name(name)
                .breadType(breadType)
                .imageUrl("https://cdn.bread-diary.app/breads/" + stickerNumber + ".webp")
                .build();
    }
}
