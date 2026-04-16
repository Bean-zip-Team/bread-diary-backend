package com.bean.breaddiary.domain.breadrecord.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadrecord.dto.mapper.BreadRecordMapper;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordCreateResponse;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import com.bean.breaddiary.domain.breadrecord.repository.BreadRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BreadRecordService {

    private final BreadRecordRepository breadRecordRepository;
    private final BreadRecordMapper breadRecordMapper;

    public boolean existsActiveRecord(UUID userId, Bread bread) {
        return breadRecordRepository.existsByUserIdAndBreadAndDeletedAtIsNull(userId, bread);
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
