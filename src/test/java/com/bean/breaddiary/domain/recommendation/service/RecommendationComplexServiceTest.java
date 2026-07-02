package com.bean.breaddiary.domain.recommendation.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.recommendation.dto.mapper.RecommendationMapper;
import com.bean.breaddiary.domain.recommendation.dto.response.BreadTodayResponse;
import com.bean.breaddiary.domain.recommendation.entity.DailyRecommendation;
import com.bean.breaddiary.domain.bread.service.BreadService;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCountProjection;
import com.bean.breaddiary.domain.breadrecord.service.BreadRecordService;
import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class RecommendationComplexServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private RecommendationService recommendationService;
    private BreadService breadService;
    private BreadRecordService breadRecordService;
    private RecommendationComplexService recommendationComplexService;

    @BeforeEach
    void setUp() {
        recommendationService = mock(RecommendationService.class);
        breadService = mock(BreadService.class);
        breadRecordService = mock(BreadRecordService.class);
        RecommendationMapper recommendationMapper = Mappers.getMapper(RecommendationMapper.class);

        recommendationComplexService = new RecommendationComplexService(
                recommendationService,
                recommendationMapper,
                breadService,
                breadRecordService
        );
    }

    @Test
    void getTodayRecommendationsReturnsStoredRecommendationsWithCollectedState() {
        LocalDate today = LocalDate.now(KST);
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Bread firstBread = bread("소금빵", "PASTRY", 1);
        Bread secondBread = bread("베이글", "BREAD", 2);
        List<DailyRecommendation> recommendations = List.of(
                recommendation(today, firstBread, 1),
                recommendation(today, secondBread, 2)
        );

        when(recommendationService.findRecommendationsByDate(today)).thenReturn(recommendations);
        when(breadRecordService.countTotalActiveRecordsByBreadIds(List.of(firstBread.getId(), secondBread.getId())))
                .thenReturn(Map.of(
                        firstBread.getId(), 128L,
                        secondBread.getId(), 64L
                ));
        when(breadRecordService.countActiveRecordsByBreadIds(userId, List.of(firstBread.getId(), secondBread.getId())))
                .thenReturn(Map.of(firstBread.getId(), 3L));

        BreadTodayResponse response = recommendationComplexService.getTodayRecommendations(userId);

        assertEquals(today, response.getDate());
        assertEquals(2, response.getBreads().size());
        assertEquals("소금빵", response.getBreads().get(0).getName());
        assertEquals("https://cdn.bread-diary.app/breads/1.webp", response.getBreads().get(0).getImageUrl());
        assertTrue(response.getBreads().get(0).getIsCollected());
        assertEquals(128L, response.getBreads().get(0).getTotalRecordCount());

        assertEquals("베이글", response.getBreads().get(1).getName());
        assertEquals("https://cdn.bread-diary.app/breads/2_placeholder.webp", response.getBreads().get(1).getImageUrl());
        assertFalse(response.getBreads().get(1).getIsCollected());
        assertEquals(64L, response.getBreads().get(1).getTotalRecordCount());

        verifyNoInteractions(breadService);
        verify(recommendationService, never()).saveRecommendations(any(), any());
    }

    @Test
    void getTodayRecommendationsCreatesSharedRecommendationsWhenMissing() {
        LocalDate today = LocalDate.now(KST);
        Bread salty = bread("소금빵", "PASTRY", 1);
        Bread croissant = bread("크루아상", "PASTRY", 2);
        Bread baguette = bread("바게트", "BREAD", 3);
        Bread bagel = bread("베이글", "BREAD", 4);
        Bread donut = bread("도넛", "DONUT", 5);
        Bread cake = bread("카스테라", "CAKE", 6);

        List<Bread> systemBreads = List.of(salty, croissant, baguette, bagel, donut, cake);

        when(recommendationService.findRecommendationsByDate(today)).thenReturn(List.of());
        when(breadService.findAllSystemCatalogBreads()).thenReturn(systemBreads);
        when(recommendationService.findRecentlyRecommendedBreadIds(today.minusDays(3), today.minusDays(1)))
                .thenReturn(List.of(salty.getId()));
        when(breadRecordService.findRecentActiveSystemBreadCounts(today.minusDays(6), today))
                .thenReturn(List.of(
                        countProjection(salty.getId(), 30L),
                        countProjection(croissant.getId(), 20L),
                        countProjection(baguette.getId(), 10L)
                ));
        when(recommendationService.saveRecommendations(eq(today), any()))
                .thenAnswer(invocation -> {
                    List<Bread> selected = invocation.getArgument(1);
                    return List.of(
                            recommendation(today, selected.get(0), 1),
                            recommendation(today, selected.get(1), 2),
                            recommendation(today, selected.get(2), 3),
                            recommendation(today, selected.get(3), 4),
                            recommendation(today, selected.get(4), 5)
                    );
                });
        when(breadRecordService.countTotalActiveRecordsByBreadIds(any()))
                .thenReturn(Map.of());
        when(breadRecordService.countActiveRecordsByBreadIds(isNull(), any()))
                .thenReturn(Map.of());

        BreadTodayResponse response = recommendationComplexService.getTodayRecommendations(null);

        assertEquals(today, response.getDate());
        assertEquals(5, response.getBreads().size());
        assertFalse(response.getBreads().stream().anyMatch(item -> item.getId().equals(salty.getId())));
        assertTrue(response.getBreads().stream().anyMatch(item -> item.getId().equals(croissant.getId())));
        assertTrue(response.getBreads().stream().anyMatch(item -> item.getId().equals(baguette.getId())));
        assertEquals(2L, response.getBreads().stream()
                .filter(item -> "PASTRY".equals(item.getType()))
                .count());
        assertTrue(response.getBreads().stream().allMatch(item -> !item.getIsCollected()));
    }

    private Bread bread(String name, String breadTypeCode, int stickerNumber) {
        return Bread.builder()
                .id(UUID.randomUUID())
                .stickerNumber(stickerNumber)
                .name(name)
                .breadType(BreadType.builder()
                        .id((long) stickerNumber)
                        .code(breadTypeCode)
                        .name(breadTypeCode)
                        .build())
                .imageUrl("https://cdn.bread-diary.app/breads/" + stickerNumber + ".webp")
                .build();
    }

    private DailyRecommendation recommendation(LocalDate date, Bread bread, int order) {
        return DailyRecommendation.builder()
                .recommendationDate(date)
                .bread(bread)
                .displayOrder(order)
                .build();
    }

    private BreadRecordCountProjection countProjection(UUID breadId, Long count) {
        return new BreadRecordCountProjection() {
            @Override
            public UUID getBreadId() {
                return breadId;
            }

            @Override
            public Long getEatCount() {
                return count;
            }
        };
    }
}
