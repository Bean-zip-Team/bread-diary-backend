package com.bean.breaddiary.domain.bread.service;

import com.bean.breaddiary.domain.bread.dto.mapper.BreadMapper;
import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadCatalogListResponse;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.entity.BreadType;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BreadService {

    private static final int AUTOCOMPLETE_LIMIT = 20;
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

    public List<Bread> searchAutocompleteBreads(String query) {
        Pageable limit = PageRequest.of(0, AUTOCOMPLETE_LIMIT);

        if (query == null || query.isBlank()) {
            return breadRepository.findPopularOrderByRecordCountDesc(limit);
        }

        return breadRepository.findByNameContainingIgnoreCaseOrderByStickerNumberAsc(
                query.trim(),
                limit
        );
    }

    public BreadAutocompleteResponse createAutocompleteResponse(
            List<Bread> breads,
            Map<UUID, Long> eatCounts
    ) {
        return breadMapper.mapToAutocompleteResponse(breads, eatCounts);
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

    @Transactional
    public Bread createUserBread(CreateNewBreadRecordRequest request, UUID userId) {
        validateBreadNameNotDuplicated(request.getName());

        Bread bread = breadMapper.mapToBread(
                request,
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
}
