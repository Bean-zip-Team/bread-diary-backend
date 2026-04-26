package com.bean.breaddiary.domain.bread.service;

import com.bean.breaddiary.domain.bread.dto.mapper.BreadMapper;
import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadCatalogListResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileRecordResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileStatsResponse;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import com.bean.breaddiary.domain.bread.repository.BreadRepository;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCatalogStats;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BreadService {

    private static final int DEFAULT_AUTOCOMPLETE_LIMIT = 20;
    private static final int MAX_AUTOCOMPLETE_LIMIT = 50;
    private static final String DEFAULT_USER_BREAD_IMAGE_URL =
            "https://cdn.bread-diary.app/catalog/default_user_bread.webp";

    private final BreadRepository breadRepository;
    private final BreadMapper breadMapper;

    public Bread getBreadById(UUID breadId) {
        return breadRepository.findById(breadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 빵을 카탈로그에서 찾을 수 없습니다."
                ));
    }

    public boolean existsByName(String name) {
        return breadRepository.existsByName(name);
    }

    public AutocompleteSlice searchAutocompleteBreads(String query, String cursor, Integer limit) {
        int normalizedLimit = normalizeAutocompleteLimit(limit);
        Pageable pageable = PageRequest.of(0, normalizedLimit + 1);

        if (query == null || query.isBlank()) {
            return searchPopularAutocompleteBreads(cursor, normalizedLimit, pageable);
        }

        Integer cursorStickerNumber = parseStickerNumberCursor(cursor);
        List<Bread> breads = breadRepository.findAutocompleteByNameContainingAfterStickerNumber(
                query.trim(),
                cursorStickerNumber,
                pageable
        );

        return createStickerNumberAutocompleteSlice(breads, normalizedLimit);
    }

    public BreadAutocompleteResponse createAutocompleteResponse(
            AutocompleteSlice autocompleteSlice,
            Map<UUID, Long> eatCounts
    ) {
        return breadMapper.mapToAutocompleteResponse(
                autocompleteSlice.getItems(),
                eatCounts,
                autocompleteSlice.getNextCursor(),
                autocompleteSlice.getHasMore()
        );
    }

    public List<Bread> findAllSystemCatalogBreads() {
        return breadRepository.findAllByCreatedByIsNullOrderByStickerNumberAsc();
    }

    public List<Bread> findCatalogCandidates(String search, BreadType breadType) {
        return breadRepository.findCatalogCandidates(
                normalizeSearch(search),
                breadType
        );
    }

    public BreadCatalogListResponse createCatalogListResponse(
            List<Bread> breads,
            Map<UUID, BreadRecordCatalogStats> statsMap,
            String nextCursor,
            boolean hasMore,
            long totalCount
    ) {
        return breadMapper.mapToCatalogListResponse(
                breads,
                statsMap,
                nextCursor,
                hasMore,
                totalCount
        );
    }

    public BreadProfileResponse createProfileResponse(
            Bread bread,
            BreadProfileStatsResponse stats,
            List<BreadProfileRecordResponse> records
    ) {
        return breadMapper.mapToProfileResponse(
                bread,
                stats,
                records
        );
    }

    @Transactional
    public void deleteIfUserCreated(Bread bread) {
        if (bread != null && bread.isUserCreated()) {
            breadRepository.delete(bread);
        }
    }

    @Transactional
    public Bread createUserBread(CreateNewBreadRecordRequest request, BreadType breadType, UUID userId) {
        validateBreadNameNotDuplicated(request.getName());

        Bread bread = breadMapper.mapToBread(
                request,
                breadType,
                getNextStickerNumber(),
                DEFAULT_USER_BREAD_IMAGE_URL,
                userId
        );

        return breadRepository.save(bread);
    }

    private Integer getNextStickerNumber() {
        return breadRepository.findTopByOrderByStickerNumberDesc()
                .map(Bread::getStickerNumber)
                .orElse(0) + 1;
    }

    private void validateBreadNameNotDuplicated(String name) {
        if (existsByName(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "같은 이름의 빵이 이미 카탈로그에 있습니다."
            );
        }
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        return search.trim();
    }

    private AutocompleteSlice searchPopularAutocompleteBreads(
            String cursor,
            int normalizedLimit,
            Pageable pageable
    ) {
        PopularAutocompleteCursor popularCursor = parsePopularAutocompleteCursor(cursor);
        List<Bread> breads = breadRepository.findPopularAutocompleteAfterCursor(
                popularCursor.getRecordCount(),
                popularCursor.getStickerNumber(),
                pageable
        );

        if (breads.size() <= normalizedLimit) {
            return new AutocompleteSlice(breads, null, false);
        }

        List<Bread> pageItems = breads.subList(0, normalizedLimit);
        Bread lastBread = pageItems.get(pageItems.size() - 1);
        Long recordCount = breadRepository.countActiveRecordsByBreadId(lastBread.getId());
        String nextCursor = recordCount + "_" + lastBread.getStickerNumber();

        return new AutocompleteSlice(pageItems, nextCursor, true);
    }

    private AutocompleteSlice createStickerNumberAutocompleteSlice(List<Bread> breads, int normalizedLimit) {
        if (breads.size() <= normalizedLimit) {
            return new AutocompleteSlice(breads, null, false);
        }

        List<Bread> pageItems = breads.subList(0, normalizedLimit);
        String nextCursor = String.valueOf(pageItems.get(pageItems.size() - 1).getStickerNumber());

        return new AutocompleteSlice(pageItems, nextCursor, true);
    }

    private int normalizeAutocompleteLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_AUTOCOMPLETE_LIMIT;
        }

        return Math.min(limit, MAX_AUTOCOMPLETE_LIMIT);
    }

    private Integer parseStickerNumberCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(cursor.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private PopularAutocompleteCursor parsePopularAutocompleteCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return PopularAutocompleteCursor.empty();
        }

        int separatorIndex = cursor.indexOf('_');
        if (separatorIndex < 0 || separatorIndex == cursor.length() - 1) {
            return PopularAutocompleteCursor.empty();
        }

        try {
            long recordCount = Long.parseLong(cursor.substring(0, separatorIndex).trim());
            int stickerNumber = Integer.parseInt(cursor.substring(separatorIndex + 1).trim());
            return new PopularAutocompleteCursor(recordCount, stickerNumber);
        } catch (NumberFormatException ignored) {
            return PopularAutocompleteCursor.empty();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class AutocompleteSlice {
        private List<Bread> items;
        private String nextCursor;
        private Boolean hasMore;
    }

    @Getter
    @AllArgsConstructor
    private static class PopularAutocompleteCursor {
        private Long recordCount;
        private Integer stickerNumber;

        private static PopularAutocompleteCursor empty() {
            return new PopularAutocompleteCursor(null, null);
        }
    }
}
