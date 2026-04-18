package com.bean.breaddiary.domain.bread.service;

import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadCatalogListResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileRecordResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileStatsResponse;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.entity.BreadType;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCatalogStats;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import com.bean.breaddiary.domain.breadrecord.service.BreadRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BreadComplexServiceTest {

    private BreadService breadService;
    private BreadRecordService breadRecordService;
    private BreadComplexService breadComplexService;

    @BeforeEach
    void setUp() {
        breadService = mock(BreadService.class);
        breadRecordService = mock(BreadRecordService.class);
        breadComplexService = new BreadComplexService(breadService, breadRecordService);
    }

    @Test
    void autocompleteBreadsCombinesBreadSearchAndUserEatCounts() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID breadId = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");
        Bread bread = Bread.builder()
                .id(breadId)
                .stickerNumber(6)
                .name("크루아상")
                .breadType(BreadType.PASTRY)
                .imageUrl("https://cdn.bread-diary.app/breads/croissant.webp")
                .build();
        BreadAutocompleteResponse expected = new BreadAutocompleteResponse(List.of());

        when(breadService.searchAutocompleteBreads("크루")).thenReturn(List.of(bread));
        when(breadRecordService.countActiveRecordsByBreadIds(userId, List.of(breadId)))
                .thenReturn(Map.of(breadId, 5L));
        when(breadService.createAutocompleteResponse(List.of(bread), Map.of(breadId, 5L)))
                .thenReturn(expected);

        BreadAutocompleteResponse actual = breadComplexService.autocompleteBreads("크루", userId);

        assertSame(expected, actual);
        verify(breadService).searchAutocompleteBreads("크루");
        verify(breadRecordService).countActiveRecordsByBreadIds(userId, List.of(breadId));
        verify(breadService).createAutocompleteResponse(List.of(bread), Map.of(breadId, 5L));
    }

    @Test
    void autocompleteBreadsSkipsEatCountLookupForAnonymousUser() {
        BreadAutocompleteResponse expected = new BreadAutocompleteResponse(List.of());

        when(breadService.searchAutocompleteBreads(null)).thenReturn(List.of());
        when(breadService.createAutocompleteResponse(List.of(), Map.of())).thenReturn(expected);

        BreadAutocompleteResponse actual = breadComplexService.autocompleteBreads(null, null);

        assertSame(expected, actual);
        verify(breadService).searchAutocompleteBreads(null);
        verifyNoInteractions(breadRecordService);
        verify(breadService).createAutocompleteResponse(List.of(), Map.of());
    }

    @Test
    void getBreadCatalogCreatesAnonymousStickerNumberPage() {
        Bread firstBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                1,
                "크루아상",
                BreadType.PASTRY
        );
        Bread secondBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                2,
                "베이글",
                BreadType.BAGEL
        );
        BreadCatalogListResponse expected = new BreadCatalogListResponse(List.of(), "1", true, 2L);

        when(breadService.findCatalogCandidates(null, null)).thenReturn(List.of(firstBread, secondBread));
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
        verify(breadService).findCatalogCandidates(null, null);
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
    void getBreadCatalogAppliesCollectedFilterWithUserStats() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Bread collectedBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                1,
                "크루아상",
                BreadType.PASTRY
        );
        Bread uncollectedBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                2,
                "베이글",
                BreadType.BAGEL
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

        when(breadService.findCatalogCandidates("크루", BreadType.PASTRY))
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
                BreadType.PASTRY,
                "크루",
                null,
                20,
                userId
        );

        assertSame(expected, actual);
        verify(breadService).findCatalogCandidates("크루", BreadType.PASTRY);
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
    void getBreadProfileCombinesBreadAndUserRecords() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID breadId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        Bread bread = createBread(
                breadId,
                6,
                "크루아상",
                BreadType.PASTRY
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

        when(breadService.getBreadById(breadId)).thenReturn(bread);
        when(breadRecordService.findActiveRecordsByUserAndBread(userId, bread))
                .thenReturn(List.of(record));
        when(breadRecordService.createProfileStats(List.of(record))).thenReturn(stats);
        when(breadRecordService.createProfileRecordResponses(List.of(record))).thenReturn(recordResponses);
        when(breadService.createProfileResponse(bread, stats, recordResponses)).thenReturn(expected);

        BreadProfileResponse actual = breadComplexService.getBreadProfile(breadId, userId);

        assertSame(expected, actual);
        verify(breadService).getBreadById(breadId);
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
