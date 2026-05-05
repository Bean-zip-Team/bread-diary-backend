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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BreadComplexService {

    private static final String SORT_STICKER_NUMBER = "sticker_number";
    private static final String SORT_LATEST = "latest";
    private static final String SORT_RATING = "rating";
    private static final String FILTER_ALL = "all";
    private static final String FILTER_COLLECTED = "collected";
    private static final String FILTER_UNCOLLECTED = "uncollected";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final BreadService breadService;
    private final BreadTypeService breadTypeService;
    private final BreadRecordService breadRecordService;
    private final OnboardingService onboardingService;

    public BreadAutocompleteResponse autocompleteBreads(String query, String cursor, Integer limit, UUID userId) {
        BreadService.AutocompleteSlice autocompleteSlice = breadService.searchAutocompleteBreads(query, cursor, limit, userId);
        List<Bread> breads = autocompleteSlice.getItems();

        Map<UUID, Long> eatCounts = resolveEatCounts(userId, breads);
        Set<UUID> onboardingSelectedBreadIds = resolveOnboardingSelectedBreadIds(userId, breads);

        return breadService.createAutocompleteResponse(autocompleteSlice, eatCounts, onboardingSelectedBreadIds);
    }

    public BreadCatalogListResponse getBreadCatalog(
            String sort,
            String filter,
            String breadTypeCode,
            String search,
            String cursor,
            Integer limit,
            UUID userId
    ) {
        String normalizedSort = normalizeSort(sort);
        String normalizedFilter = normalizeFilter(filter);
        int normalizedLimit = normalizeLimit(limit);
        BreadType breadType = resolveBreadType(breadTypeCode);

        List<Bread> candidates = breadService.findCatalogCandidates(search, breadType, userId);
        Map<UUID, BreadRecordCatalogStats> statsMap = resolveCatalogStats(userId, candidates);
        Set<UUID> onboardingSelectedBreadIds = resolveOnboardingSelectedBreadIds(userId, candidates);

        List<Bread> filteredBreads = applyFilter(candidates, statsMap, onboardingSelectedBreadIds, userId, normalizedFilter);
        List<Bread> sortedBreads = applySort(filteredBreads, statsMap, onboardingSelectedBreadIds, normalizedSort, normalizedFilter);
        List<Bread> cursorAppliedBreads = applyCursor(sortedBreads, normalizedSort, normalizedFilter, cursor);
        List<Bread> pageBreads = takePage(cursorAppliedBreads, normalizedLimit);

        boolean hasMore = cursorAppliedBreads.size() > normalizedLimit;
        String nextCursor = hasMore && !pageBreads.isEmpty()
                ? createNextCursor(pageBreads.get(pageBreads.size() - 1), statsMap, normalizedSort, normalizedFilter)
                : null;

        return breadService.createCatalogListResponse(
                pageBreads,
                statsMap,
                onboardingSelectedBreadIds,
                nextCursor,
                hasMore,
                filteredBreads.size()
        );
    }

    public BreadProfileResponse getBreadProfile(UUID breadId, UUID userId) {
        Bread bread = breadService.getBreadById(breadId, userId);
        List<BreadRecord> records = breadRecordService.findActiveRecordsByUserAndBread(
                userId,
                bread
        );
        BreadProfileStatsResponse stats = breadRecordService.createProfileStats(records);
        List<BreadProfileRecordResponse> recordResponses = breadRecordService.createProfileRecordResponses(records);
        boolean onboardingSelected = userId != null && onboardingService.isSelected(userId, breadId);

        return breadService.createProfileResponse(
                bread,
                stats,
                recordResponses,
                onboardingSelected
        );
    }

    private Map<UUID, Long> resolveEatCounts(UUID userId, List<Bread> breads) {
        if (userId == null || breads.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> breadIds = breads.stream()
                .map(Bread::getId)
                .toList();

        return breadRecordService.countActiveRecordsByBreadIds(userId, breadIds);
    }

    private BreadType resolveBreadType(String breadTypeCode) {
        if (breadTypeCode == null || breadTypeCode.isBlank()) {
            return null;
        }

        return breadTypeService.getBreadTypeByCode(breadTypeCode);
    }

    private Map<UUID, BreadRecordCatalogStats> resolveCatalogStats(UUID userId, List<Bread> breads) {
        if (userId == null || breads.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> breadIds = breads.stream()
                .map(Bread::getId)
                .toList();

        return breadRecordService.findCatalogStatsByBreadIds(userId, breadIds);
    }

    private Set<UUID> resolveOnboardingSelectedBreadIds(UUID userId, List<Bread> breads) {
        if (userId == null || breads.isEmpty()) {
            return Set.of();
        }

        List<UUID> breadIds = breads.stream()
                .map(Bread::getId)
                .toList();

        return onboardingService.findSelectedBreadIds(userId, breadIds);
    }

    private List<Bread> applyFilter(
            List<Bread> breads,
            Map<UUID, BreadRecordCatalogStats> statsMap,
            Set<UUID> onboardingSelectedBreadIds,
            UUID userId,
            String filter
    ) {
        if (FILTER_ALL.equals(filter)) {
            return breads;
        }

        return breads.stream()
                .filter(bread -> {
                    boolean isCollected = isCollected(bread, statsMap, onboardingSelectedBreadIds);

                    if (FILTER_COLLECTED.equals(filter)) {
                        return userId != null && isCollected;
                    }

                    if (FILTER_UNCOLLECTED.equals(filter)) {
                        return !isCollected;
                    }

                    return true;
                })
                .toList();
    }

    private List<Bread> applySort(
            List<Bread> breads,
            Map<UUID, BreadRecordCatalogStats> statsMap,
            Set<UUID> onboardingSelectedBreadIds,
            String sort,
            String filter
    ) {
        List<Bread> sortedBreads = new ArrayList<>(breads);

        if (SORT_LATEST.equals(sort)) {
            return sortLatestBreads(sortedBreads, statsMap, onboardingSelectedBreadIds);
        }

        if (SORT_RATING.equals(sort)) {
            return sortRatingBreads(sortedBreads, statsMap, onboardingSelectedBreadIds);
        }

        if (FILTER_ALL.equals(filter)) {
            sortedBreads.sort(Comparator
                    .comparingInt((Bread bread) -> collectionPriority(bread, statsMap, onboardingSelectedBreadIds))
                    .reversed()
                    .thenComparing(Bread::getStickerNumber)
                    .thenComparing(bread -> bread.getId().toString()));
            return sortedBreads;
        }

        sortedBreads.sort(Comparator.comparing(Bread::getStickerNumber));
        return sortedBreads;
    }

    private List<Bread> sortLatestBreads(
            List<Bread> breads,
            Map<UUID, BreadRecordCatalogStats> statsMap,
            Set<UUID> onboardingSelectedBreadIds
    ) {
        List<Bread> recordedBreads = breads.stream()
                .filter(bread -> hasRecords(bread, statsMap))
                .sorted(Comparator
                        .comparing((Bread bread) -> statsMap.get(bread.getId()).getLatestEatenDate(), Comparator.reverseOrder())
                        .thenComparing(bread -> bread.getId().toString()))
                .toList();
        List<Bread> onboardingOnlyBreads = breads.stream()
                .filter(bread -> !hasRecords(bread, statsMap) && onboardingSelectedBreadIds.contains(bread.getId()))
                .sorted(Comparator.comparing(Bread::getStickerNumber)
                        .thenComparing(bread -> bread.getId().toString()))
                .toList();

        List<Bread> sorted = new ArrayList<>(recordedBreads);
        sorted.addAll(onboardingOnlyBreads);
        return sorted;
    }

    private List<Bread> sortRatingBreads(
            List<Bread> breads,
            Map<UUID, BreadRecordCatalogStats> statsMap,
            Set<UUID> onboardingSelectedBreadIds
    ) {
        List<Bread> recordedBreads = breads.stream()
                .filter(bread -> hasRecords(bread, statsMap))
                .sorted(Comparator
                        .comparing((Bread bread) -> statsMap.get(bread.getId()).getAvgRating(), Comparator.reverseOrder())
                        .thenComparing(bread -> bread.getId().toString()))
                .toList();
        List<Bread> onboardingOnlyBreads = breads.stream()
                .filter(bread -> !hasRecords(bread, statsMap) && onboardingSelectedBreadIds.contains(bread.getId()))
                .sorted(Comparator.comparing(Bread::getStickerNumber)
                        .thenComparing(bread -> bread.getId().toString()))
                .toList();

        List<Bread> sorted = new ArrayList<>(recordedBreads);
        sorted.addAll(onboardingOnlyBreads);
        return sorted;
    }

    private List<Bread> applyCursor(
            List<Bread> breads,
            String sort,
            String filter,
            String cursor
    ) {
        if (cursor == null || cursor.isBlank()) {
            return breads;
        }

        if (SORT_STICKER_NUMBER.equals(sort) && !FILTER_ALL.equals(filter)) {
            return applyStickerNumberCursor(breads, cursor);
        }

        return applyCompositeCursor(breads, cursor);
    }

    private List<Bread> applyStickerNumberCursor(List<Bread> breads, String cursor) {
        try {
            int stickerNumberCursor = Integer.parseInt(cursor);

            return breads.stream()
                    .filter(bread -> bread.getStickerNumber() > stickerNumberCursor)
                    .toList();
        } catch (NumberFormatException ignored) {
            return breads;
        }
    }

    private List<Bread> applyCompositeCursor(List<Bread> breads, String cursor) {
        int separatorIndex = cursor.indexOf('_');
        if (separatorIndex < 0 || separatorIndex == cursor.length() - 1) {
            return breads;
        }

        String breadId = cursor.substring(separatorIndex + 1);
        for (int index = 0; index < breads.size(); index++) {
            if (breads.get(index).getId().toString().equals(breadId)) {
                return breads.subList(index + 1, breads.size());
            }
        }

        return breads;
    }

    private List<Bread> takePage(List<Bread> breads, int limit) {
        return breads.stream()
                .limit(limit)
                .toList();
    }

    private String createNextCursor(
            Bread lastBread,
            Map<UUID, BreadRecordCatalogStats> statsMap,
            String sort,
            String filter
    ) {
        if (SORT_STICKER_NUMBER.equals(sort) && !FILTER_ALL.equals(filter)) {
            return String.valueOf(lastBread.getStickerNumber());
        }

        if (SORT_LATEST.equals(sort)) {
            return statsMap.get(lastBread.getId()).getLatestEatenDate() + "_" + lastBread.getId();
        }

        if (SORT_RATING.equals(sort)) {
            return statsMap.get(lastBread.getId()).getAvgRating() + "_" + lastBread.getId();
        }

        return lastBread.getStickerNumber() + "_" + lastBread.getId();
    }

    private boolean isCollected(Bread bread, Map<UUID, BreadRecordCatalogStats> statsMap, Set<UUID> onboardingSelectedBreadIds) {
        return hasRecords(bread, statsMap) || onboardingSelectedBreadIds.contains(bread.getId());
    }

    private boolean hasRecords(Bread bread, Map<UUID, BreadRecordCatalogStats> statsMap) {
        BreadRecordCatalogStats stats = statsMap.get(bread.getId());
        return stats != null && stats.getEatCount() != null && stats.getEatCount() > 0;
    }

    private int collectionPriority(Bread bread, Map<UUID, BreadRecordCatalogStats> statsMap, Set<UUID> onboardingSelectedBreadIds) {
        if (hasRecords(bread, statsMap)) {
            return 2;
        }

        if (onboardingSelectedBreadIds.contains(bread.getId())) {
            return 1;
        }

        return 0;
    }

    private String normalizeSort(String sort) {
        if (SORT_LATEST.equals(sort) || SORT_RATING.equals(sort)) {
            return sort;
        }

        return SORT_STICKER_NUMBER;
    }

    private String normalizeFilter(String filter) {
        if (FILTER_COLLECTED.equals(filter) || FILTER_UNCOLLECTED.equals(filter)) {
            return filter;
        }

        return FILTER_ALL;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }
}
