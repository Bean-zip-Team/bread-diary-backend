package com.bean.breaddiary.domain.breadrecord.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileRecordResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileStatsResponse;
import com.bean.breaddiary.domain.breadrecord.dto.mapper.BreadRecordMapper;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCatalogStats;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordCreateResponse;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCountProjection;
import com.bean.breaddiary.domain.breadrecord.repository.BreadRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BreadRecordService {

    private final BreadRecordRepository breadRecordRepository;
    private final BreadRecordMapper breadRecordMapper;

    public boolean existsActiveRecord(UUID userId, Bread bread) {
        return breadRecordRepository.existsByUserIdAndBreadAndDeletedAtIsNull(userId, bread);
    }

    public Map<UUID, Long> countActiveRecordsByBreadIds(UUID userId, List<UUID> breadIds) {
        if (userId == null || breadIds == null || breadIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return breadRecordRepository.countActiveRecordsByBreadIds(userId, breadIds)
                .stream()
                .collect(Collectors.toMap(
                        BreadRecordCountProjection::getBreadId,
                        BreadRecordCountProjection::getEatCount
                ));
    }

    public Map<UUID, BreadRecordCatalogStats> findCatalogStatsByBreadIds(UUID userId, List<UUID> breadIds) {
        if (userId == null || breadIds == null || breadIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return breadRecordMapper.mapToCatalogStatsMap(
                breadRecordRepository.findCatalogStatsByBreadIds(userId, breadIds),
                breadRecordRepository.findLatestPhotoUrlsByBreadIds(userId, breadIds)
        );
    }

    public List<BreadRecord> findActiveRecordsByUserAndBread(UUID userId, Bread bread) {
        if (userId == null || bread == null) {
            return Collections.emptyList();
        }

        return breadRecordRepository.findAllByUserIdAndBreadAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId,
                bread
        );
    }

    public BreadProfileStatsResponse createProfileStats(List<BreadRecord> breadRecords) {
        return breadRecordMapper.mapToProfileStats(breadRecords);
    }

    public List<BreadProfileRecordResponse> createProfileRecordResponses(List<BreadRecord> breadRecords) {
        return breadRecordMapper.mapToProfileRecords(breadRecords);
    }

    @Transactional
    public BreadRecord createBreadRecord(
            CreateBreadRecordRequest request,
            UUID userId,
            Bread bread,
            String photoUrl
    ) {
        BreadRecord breadRecord = breadRecordMapper.mapToBreadRecord(
                request,
                userId,
                bread,
                photoUrl,
                resolveEatenDate(request.getEatenDate())
        );

        return breadRecordRepository.save(breadRecord);
    }

    @Transactional
    public BreadRecord createBreadRecord(
            CreateNewBreadRecordRequest request,
            UUID userId,
            Bread bread,
            String photoUrl
    ) {
        BreadRecord breadRecord = breadRecordMapper.mapToBreadRecord(
                request,
                userId,
                bread,
                photoUrl,
                resolveEatenDate(request.getEatenDate())
        );

        return breadRecordRepository.save(breadRecord);
    }

    public BreadRecordCreateResponse createResponse(
            BreadRecord breadRecord,
            Boolean isFirstRecord
    ) {
        return breadRecordMapper.mapToCreateResponse(
                breadRecord,
                isFirstRecord
        );
    }

    private LocalDate resolveEatenDate(LocalDate eatenDate) {
        return eatenDate == null ? LocalDate.now() : eatenDate;
    }

    public void validateEatenDate(LocalDate eatenDate) {
        if (eatenDate != null && eatenDate.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "미래 날짜는 선택할 수 없습니다."
            );
        }
    }
}
