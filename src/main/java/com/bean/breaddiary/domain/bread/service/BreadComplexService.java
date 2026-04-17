package com.bean.breaddiary.domain.bread.service;

import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteResponse;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadrecord.service.BreadRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BreadComplexService {

    private final BreadService breadService;
    private final BreadRecordService breadRecordService;

    public BreadAutocompleteResponse autocompleteBreads(String query, UUID userId) {
        List<Bread> breads = breadService.searchAutocompleteBreads(query);

        Map<UUID, Long> eatCounts = resolveEatCounts(userId, breads);

        return breadService.createAutocompleteResponse(breads, eatCounts);
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
}
