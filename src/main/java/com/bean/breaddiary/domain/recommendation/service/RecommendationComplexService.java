package com.bean.breaddiary.domain.recommendation.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.recommendation.dto.mapper.RecommendationMapper;
import com.bean.breaddiary.domain.recommendation.dto.response.BreadTodayResponse;
import com.bean.breaddiary.domain.recommendation.entity.DailyRecommendation;
import com.bean.breaddiary.domain.bread.service.BreadService;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCountProjection;
import com.bean.breaddiary.domain.breadrecord.service.BreadRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationComplexService {

    private static final int RECOMMENDATION_SIZE = 5;
    private static final int MAX_PER_TYPE = 2;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RecommendationService recommendationService;
    private final RecommendationMapper recommendationMapper;
    private final BreadService breadService;
    private final BreadRecordService breadRecordService;

    @Transactional
    public BreadTodayResponse getTodayRecommendations(UUID userId) {
        LocalDate recommendationDate = LocalDate.now(KST);
        List<DailyRecommendation> recommendations = recommendationService.findRecommendationsByDate(recommendationDate);

        if (recommendations.isEmpty()) {
            recommendations = createTodayRecommendations(recommendationDate);
        }

        List<UUID> breadIds = recommendations.stream()
                .map(recommendation -> recommendation.getBread().getId())
                .toList();

        Map<UUID, Long> totalRecordCounts = breadRecordService.countTotalActiveRecordsByBreadIds(breadIds);
        Map<UUID, Long> userEatCounts = breadRecordService.countActiveRecordsByBreadIds(userId, breadIds);

        return recommendationMapper.mapToTodayResponse(
                recommendationDate,
                recommendations,
                totalRecordCounts,
                userEatCounts
        );
    }

    protected List<DailyRecommendation> createTodayRecommendations(LocalDate recommendationDate) {
        List<Bread> systemBreads = breadService.findAllSystemCatalogBreads();
        if (systemBreads.isEmpty()) {
            return List.of();
        }

        Map<UUID, Bread> breadById = systemBreads.stream()
                .collect(Collectors.toMap(Bread::getId, Function.identity()));

        List<UUID> recentlyRecommendedBreadIds = recommendationService.findRecentlyRecommendedBreadIds(
                recommendationDate.minusDays(3),
                recommendationDate.minusDays(1)
        );
        Set<UUID> excludedBreadIds = new LinkedHashSet<>(recentlyRecommendedBreadIds);

        List<BreadRecordCountProjection> recentCounts = breadRecordService.findRecentActiveSystemBreadCounts(
                recommendationDate.minusDays(6),
                recommendationDate
        );

        List<Bread> popularCandidates = recentCounts.stream()
                .map(BreadRecordCountProjection::getBreadId)
                .map(breadById::get)
                .filter(Objects::nonNull)
                .toList();

        List<Bread> fallbackCandidates = createDeterministicFallbackCandidates(systemBreads, recommendationDate);
        List<Bread> selected = selectRecommendations(popularCandidates, fallbackCandidates, excludedBreadIds);

        return recommendationService.saveRecommendations(recommendationDate, selected);
    }

    private List<Bread> selectRecommendations(
            List<Bread> popularCandidates,
            List<Bread> fallbackCandidates,
            Set<UUID> excludedBreadIds
    ) {
        List<Bread> selected = new ArrayList<>();

        addCandidates(selected, popularCandidates, excludedBreadIds, true);
        addCandidates(selected, fallbackCandidates, excludedBreadIds, true);
        addCandidates(selected, popularCandidates, Collections.emptySet(), true);
        addCandidates(selected, fallbackCandidates, Collections.emptySet(), true);
        addCandidates(selected, fallbackCandidates, Collections.emptySet(), false);

        return selected.stream()
                .limit(RECOMMENDATION_SIZE)
                .toList();
    }

    private void addCandidates(
            List<Bread> selected,
            List<Bread> candidates,
            Set<UUID> excludedBreadIds,
            boolean enforceTypeLimit
    ) {
        for (Bread candidate : candidates) {
            if (selected.size() >= RECOMMENDATION_SIZE) {
                return;
            }

            UUID breadId = candidate.getId();
            if (excludedBreadIds.contains(breadId)) {
                continue;
            }

            if (containsBread(selected, breadId)) {
                continue;
            }

            if (enforceTypeLimit && countType(selected, candidate.getBreadType().getCode()) >= MAX_PER_TYPE) {
                continue;
            }

            selected.add(candidate);
        }
    }

    private List<Bread> createDeterministicFallbackCandidates(List<Bread> breads, LocalDate recommendationDate) {
        List<Bread> fallbackCandidates = new ArrayList<>(breads);
        fallbackCandidates.sort(Comparator.comparing(Bread::getStickerNumber).thenComparing(Bread::getId));
        Collections.shuffle(fallbackCandidates, new Random(recommendationDate.toEpochDay()));
        return fallbackCandidates;
    }

    private boolean containsBread(List<Bread> selected, UUID breadId) {
        return selected.stream().anyMatch(bread -> bread.getId().equals(breadId));
    }

    private long countType(List<Bread> selected, String breadTypeCode) {
        return selected.stream()
                .filter(bread -> bread.getBreadType().getCode().equals(breadTypeCode))
                .count();
    }
}
