package com.bean.breaddiary.domain.breadrecord.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.service.BreadService;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.UpdateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordDeleteResponse;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordDetailResponse;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordCreateResponse;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import com.bean.breaddiary.global.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    public BreadRecordDetailResponse getBreadRecord(UUID userId, UUID recordId) {
        BreadRecord breadRecord = breadRecordService.getActiveRecordForUser(
                userId,
                recordId
        );

        return breadRecordService.createDetailResponse(breadRecord);
    }

    public BreadRecordDetailResponse updateBreadRecord(
            UUID userId,
            UUID recordId,
            UpdateBreadRecordRequest request
    ) {
        breadRecordService.validateEatenDate(request.getEatenDate());

        BreadRecord breadRecord = breadRecordService.getActiveRecordForUser(
                userId,
                recordId
        );
        String photoUrl = uploadPhotoIfPresent(userId, request.getPhoto());
        BreadRecord updatedRecord = breadRecordService.updateBreadRecord(
                breadRecord,
                request,
                photoUrl
        );

        return breadRecordService.createDetailResponse(updatedRecord);
    }

    public BreadRecordDeleteResponse deleteBreadRecord(UUID userId, UUID recordId) {
        BreadRecord breadRecord = breadRecordService.getActiveRecordForUser(
                userId,
                recordId
        );
        Bread bread = breadRecord.getBread();

        breadRecordService.deleteBreadRecord(breadRecord);

        boolean stickerRemoved = !breadRecordService.hasRemainingActiveRecord(userId, bread);
        if (stickerRemoved && !breadRecordService.existsActiveRecordByBread(bread)) {
            breadService.deleteIfUserCreated(bread);
        }

        return new BreadRecordDeleteResponse(stickerRemoved);
    }

    private String uploadPhotoIfPresent(UUID userId, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return null;
        }

        return s3UploadService.uploadBreadPhoto(userId, photo);
    }
}
