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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture.*;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BreadComplexServiceTest {

    private BreadService breadService;
    private BreadTypeService breadTypeService;
    private BreadRecordService breadRecordService;
    private BreadComplexService breadComplexService;

    @BeforeEach
    void setUp() {
        breadService = mock(BreadService.class);
        breadTypeService = mock(BreadTypeService.class);
        breadRecordService = mock(BreadRecordService.class);
        breadComplexService = new BreadComplexService(breadService, breadTypeService, breadRecordService);
    }

    @Test
    void autocompleteBreadsCombinesBreadSearchAndUserEatCounts() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID breadId = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");
        Bread bread = Bread.builder()
                .id(breadId)
                .stickerNumber(6)
                .name("크루아상")
                .breadType(PASTRY)
                .imageUrl("https://cdn.bread-diary.app/breads/croissant.webp")
                .build();
        BreadService.AutocompleteSlice autocompleteSlice = new BreadService.AutocompleteSlice(List.of(bread), "6", true);
        BreadAutocompleteResponse expected = new BreadAutocompleteResponse(List.of(), "6", true);

        when(breadService.searchAutocompleteBreads("크루", "3", 10, userId)).thenReturn(autocompleteSlice);
        when(breadRecordService.countActiveRecordsByBreadIds(userId, List.of(breadId)))
                .thenReturn(Map.of(breadId, 5L));
        when(breadService.createAutocompleteResponse(autocompleteSlice, Map.of(breadId, 5L)))
                .thenReturn(expected);

        BreadAutocompleteResponse actual = breadComplexService.autocompleteBreads("크루", "3", 10, userId);

        assertSame(expected, actual);
        verify(breadService).searchAutocompleteBreads("크루", "3", 10, userId);
        verify(breadRecordService).countActiveRecordsByBreadIds(userId, List.of(breadId));
        verify(breadService).createAutocompleteResponse(autocompleteSlice, Map.of(breadId, 5L));
    }

    @Test
    void autocompleteBreadsSkipsEatCountLookupForAnonymousUser() {
        BreadService.AutocompleteSlice autocompleteSlice = new BreadService.AutocompleteSlice(List.of(), null, false);
        BreadAutocompleteResponse expected = new BreadAutocompleteResponse(List.of(), null, false);

        when(breadService.searchAutocompleteBreads(null, null, null, null)).thenReturn(autocompleteSlice);
        when(breadService.createAutocompleteResponse(autocompleteSlice, Map.of())).thenReturn(expected);

        BreadAutocompleteResponse actual = breadComplexService.autocompleteBreads(null, null, null, null);

        assertSame(expected, actual);
        verify(breadService).searchAutocompleteBreads(null, null, null, null);
        verifyNoInteractions(breadRecordService);
        verify(breadService).createAutocompleteResponse(autocompleteSlice, Map.of());
    }

    @Test
    void getBreadCatalogCreatesAnonymousStickerNumberPage() {
        Bread firstBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                1,
                "크루아상",
                PASTRY
        );
        Bread secondBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                2,
                "베이글",
                BAGEL
        );
        BreadCatalogListResponse expected = new BreadCatalogListResponse(List.of(), "1", true, 2L);

        when(breadService.findCatalogCandidates(null, null, null)).thenReturn(List.of(firstBread, secondBread));
        when(breadService.createCatalogListResponse(
                List.of(firstBread),
                Map.of(),
                "1",
                true,
                2L
        )).thenReturn(expected);

        BreadCatalogListResponse actual = breadComplexService.getBreadCatalog(
                "sticker_number",
                "all",
                null,
                null,
                null,
                1,
                null
        );

        assertSame(expected, actual);
        verify(breadService).findCatalogCandidates(null, null, null);
        verifyNoInteractions(breadRecordService);
        verify(breadService).createCatalogListResponse(
                List.of(firstBread),
                Map.of(),
                "1",
                true,
                2L
        );
    }

    @Test
    void getBreadCatalogSortsCollectedBreadsFirstForAllFilter() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Bread uncollectedBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                1,
                "소금빵",
                PASTRY
        );
        Bread collectedBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                2,
                "크루아상",
                PASTRY
        );
        Map<UUID, BreadRecordCatalogStats> statsMap = Map.of(
                collectedBread.getId(),
                new BreadRecordCatalogStats(
                        collectedBread.getId(),
                        1L,
                        4.5,
                        null,
                        LocalDate.of(2026, 3, 14)
                )
        );
        BreadCatalogListResponse expected = new BreadCatalogListResponse(List.of(), null, false, 2L);

        when(breadService.findCatalogCandidates(null, null, userId)).thenReturn(List.of(uncollectedBread, collectedBread));
        when(breadRecordService.findCatalogStatsByBreadIds(
                userId,
                List.of(uncollectedBread.getId(), collectedBread.getId())
        )).thenReturn(statsMap);
        when(breadService.createCatalogListResponse(
                List.of(collectedBread, uncollectedBread),
                statsMap,
                null,
                false,
                2L
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
                List.of(collectedBread, uncollectedBread),
                statsMap,
                null,
                false,
                2L
        );
    }

    @Test
    void getBreadCatalogUsesCompositeCursorForAllFilterOrdering() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Bread firstCollectedBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                2,
                "크루아상",
                PASTRY
        );
        Bread secondCollectedBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                3,
                "베이글",
                BAGEL
        );
        Bread uncollectedBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000003"),
                1,
                "소금빵",
                PASTRY
        );
        Map<UUID, BreadRecordCatalogStats> statsMap = Map.of(
                firstCollectedBread.getId(),
                new BreadRecordCatalogStats(firstCollectedBread.getId(), 1L, 4.0, null, LocalDate.of(2026, 3, 14)),
                secondCollectedBread.getId(),
                new BreadRecordCatalogStats(secondCollectedBread.getId(), 1L, 4.5, null, LocalDate.of(2026, 3, 15))
        );
        BreadCatalogListResponse expected = new BreadCatalogListResponse(List.of(), "2_" + firstCollectedBread.getId(), true, 3L);

        when(breadService.findCatalogCandidates(null, null, userId))
                .thenReturn(List.of(uncollectedBread, firstCollectedBread, secondCollectedBread));
        when(breadRecordService.findCatalogStatsByBreadIds(
                userId,
                List.of(uncollectedBread.getId(), firstCollectedBread.getId(), secondCollectedBread.getId())
        )).thenReturn(statsMap);
        when(breadService.createCatalogListResponse(
                List.of(firstCollectedBread),
                statsMap,
                "2_" + firstCollectedBread.getId(),
                true,
                3L
        )).thenReturn(expected);

        BreadCatalogListResponse actual = breadComplexService.getBreadCatalog(
                "sticker_number",
                "all",
                null,
                null,
                null,
                1,
                userId
        );

        assertSame(expected, actual);
        verify(breadService).createCatalogListResponse(
                List.of(firstCollectedBread),
                statsMap,
                "2_" + firstCollectedBread.getId(),
                true,
                3L
        );
    }

    @Test
    void getBreadCatalogAppliesCollectedFilterWithUserStats() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Bread collectedBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                1,
                "크루아상",
                PASTRY
        );
        Bread uncollectedBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                2,
                "베이글",
                BAGEL
        );
        BreadRecordCatalogStats stats = new BreadRecordCatalogStats(
                collectedBread.getId(),
                3L,
                4.666,
                "https://cdn.bread-diary.app/bread-photos/record.webp",
                LocalDate.of(2026, 3, 14)
        );
        Map<UUID, BreadRecordCatalogStats> statsMap = Map.of(collectedBread.getId(), stats);
        BreadCatalogListResponse expected = new BreadCatalogListResponse(List.of(), null, false, 1L);

        when(breadTypeService.getBreadTypeByCode("PASTRY")).thenReturn(PASTRY);
        when(breadService.findCatalogCandidates("크루", PASTRY, userId))
                .thenReturn(List.of(collectedBread, uncollectedBread));
        when(breadRecordService.findCatalogStatsByBreadIds(
                userId,
                List.of(collectedBread.getId(), uncollectedBread.getId())
        )).thenReturn(statsMap);
        when(breadService.createCatalogListResponse(
                List.of(collectedBread),
                statsMap,
                null,
                false,
                1L
        )).thenReturn(expected);

        BreadCatalogListResponse actual = breadComplexService.getBreadCatalog(
                "sticker_number",
                "collected",
                "PASTRY",
                "크루",
                null,
                20,
                userId
        );

        assertSame(expected, actual);
        verify(breadTypeService).getBreadTypeByCode("PASTRY");
        verify(breadService).findCatalogCandidates("크루", PASTRY, userId);
        verify(breadRecordService).findCatalogStatsByBreadIds(
                userId,
                List.of(collectedBread.getId(), uncollectedBread.getId())
        );
        verify(breadService).createCatalogListResponse(
                List.of(collectedBread),
                statsMap,
                null,
                false,
                1L
        );
    }

    @Test
    void getBreadProfileRejectsAnonymousAccessToUserCreatedBread() {
        UUID breadId = UUID.fromString("10000000-0000-0000-0000-000000000001");

        when(breadService.getBreadById(breadId, null)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> breadComplexService.getBreadProfile(breadId, null)
        );

        org.junit.jupiter.api.Assertions.assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(breadService).getBreadById(breadId, null);
        verifyNoInteractions(breadRecordService);
    }

    @Test
    void getBreadProfileCombinesBreadAndUserRecords() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID breadId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        Bread bread = createBread(
                breadId,
                6,
                "크루아상",
                PASTRY
        );
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
        when(breadService.createProfileResponse(bread, stats, recordResponses)).thenReturn(expected);

        BreadProfileResponse actual = breadComplexService.getBreadProfile(breadId, userId);

        assertSame(expected, actual);
        verify(breadService).getBreadById(breadId, userId);
        verify(breadRecordService).findActiveRecordsByUserAndBread(userId, bread);
        verify(breadRecordService).createProfileStats(List.of(record));
        verify(breadRecordService).createProfileRecordResponses(List.of(record));
        verify(breadService).createProfileResponse(bread, stats, recordResponses);
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
