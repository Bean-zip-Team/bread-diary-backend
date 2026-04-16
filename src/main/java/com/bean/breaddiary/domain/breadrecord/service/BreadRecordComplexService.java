package com.bean.breaddiary.domain.breadrecord.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.service.BreadService;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordCreateResponse;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import com.bean.breaddiary.global.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BreadRecordComplexService {

    private final BreadService breadService;
    private final BreadRecordService breadRecordService;
    private final S3UploadService s3UploadService;

    public BreadRecordCreateResponse createBreadRecord(UUID userId, CreateBreadRecordRequest request) {
        breadRecordService.validateEatenDate(request.getEatenDate());

        Bread bread = breadService.getBreadById(request.getBreadId());
        Boolean isFirstRecord = !breadRecordService.existsActiveRecord(userId, bread);

        String photoUrl = s3UploadService.uploadBreadPhoto(userId, request.getPhoto());

        BreadRecord savedRecord = breadRecordService.createBreadRecord(
                request,
                userId,
                bread,
                photoUrl
        );

        return breadRecordService.createResponse(savedRecord, isFirstRecord);
    }

    public BreadRecordCreateResponse createNewBreadRecord(UUID userId, CreateNewBreadRecordRequest request) {
        breadRecordService.validateEatenDate(request.getEatenDate());

        Bread bread = breadService.createUserBread(request, userId);
        String photoUrl = s3UploadService.uploadBreadPhoto(userId, request.getPhoto());

        BreadRecord savedRecord = breadRecordService.createBreadRecord(
                request,
                userId,
                bread,
                photoUrl
        );

        return breadRecordService.createResponse(savedRecord, true);
    }
}
