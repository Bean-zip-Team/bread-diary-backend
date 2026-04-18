package com.bean.breaddiary.domain.breadrecord.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.entity.BreadType;
import com.bean.breaddiary.domain.bread.service.BreadService;
import com.bean.breaddiary.domain.breadrecord.dto.request.UpdateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordDeleteResponse;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordDetailResponse;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import com.bean.breaddiary.global.s3.S3UploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BreadRecordComplexServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RECORD_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    private BreadService breadService;
    private BreadRecordService breadRecordService;
    private S3UploadService s3UploadService;
    private BreadRecordComplexService breadRecordComplexService;

    @BeforeEach
    void setUp() {
        breadService = mock(BreadService.class);
        breadRecordService = mock(BreadRecordService.class);
        s3UploadService = mock(S3UploadService.class);
        breadRecordComplexService = new BreadRecordComplexService(
                breadService,
                breadRecordService,
                s3UploadService
        );
    }

    @Test
    void updateBreadRecordUploadsPhotoWhenPhotoExists() {
        BreadRecord breadRecord = createBreadRecord(createBread(null));
        UpdateBreadRecordRequest request = new UpdateBreadRecordRequest(
                new MockMultipartFile("photo", "photo.webp", "image/webp", "photo".getBytes()),
                "르뺑블루",
                LocalDate.of(2026, 3, 14),
                5,
                "맛있어요."
        );
        BreadRecord updatedRecord = createBreadRecord(breadRecord.getBread());
        BreadRecordDetailResponse expected = new BreadRecordDetailResponse();

        when(breadRecordService.getActiveRecordForUser(USER_ID, RECORD_ID)).thenReturn(breadRecord);
        when(s3UploadService.uploadBreadPhoto(USER_ID, request.getPhoto()))
                .thenReturn("https://cdn.bread-diary.app/bread-photos/new.webp");
        when(breadRecordService.updateBreadRecord(
                breadRecord,
                request,
                "https://cdn.bread-diary.app/bread-photos/new.webp"
        )).thenReturn(updatedRecord);
        when(breadRecordService.createDetailResponse(updatedRecord)).thenReturn(expected);

        BreadRecordDetailResponse actual = breadRecordComplexService.updateBreadRecord(
                USER_ID,
                RECORD_ID,
                request
        );

        assertSame(expected, actual);
        verify(breadRecordService).validateEatenDate(LocalDate.of(2026, 3, 14));
        verify(s3UploadService).uploadBreadPhoto(USER_ID, request.getPhoto());
        verify(breadRecordService).updateBreadRecord(
                breadRecord,
                request,
                "https://cdn.bread-diary.app/bread-photos/new.webp"
        );
    }

    @Test
    void updateBreadRecordKeepsPhotoWhenPhotoIsMissing() {
        BreadRecord breadRecord = createBreadRecord(createBread(null));
        UpdateBreadRecordRequest request = new UpdateBreadRecordRequest(
                null,
                null,
                null,
                4,
                null
        );
        BreadRecordDetailResponse expected = new BreadRecordDetailResponse();

        when(breadRecordService.getActiveRecordForUser(USER_ID, RECORD_ID)).thenReturn(breadRecord);
        when(breadRecordService.updateBreadRecord(breadRecord, request, null)).thenReturn(breadRecord);
        when(breadRecordService.createDetailResponse(breadRecord)).thenReturn(expected);

        BreadRecordDetailResponse actual = breadRecordComplexService.updateBreadRecord(
                USER_ID,
                RECORD_ID,
                request
        );

        assertSame(expected, actual);
        verify(s3UploadService, never()).uploadBreadPhoto(USER_ID, request.getPhoto());
        verify(breadRecordService).updateBreadRecord(breadRecord, request, null);
    }

    @Test
    void deleteBreadRecordDeletesUserCreatedBreadWhenNoActiveRecordRemains() {
        Bread bread = createBread(USER_ID);
        BreadRecord breadRecord = createBreadRecord(bread);

        when(breadRecordService.getActiveRecordForUser(USER_ID, RECORD_ID)).thenReturn(breadRecord);
        when(breadRecordService.hasRemainingActiveRecord(USER_ID, bread)).thenReturn(false);
        when(breadRecordService.existsActiveRecordByBread(bread)).thenReturn(false);

        BreadRecordDeleteResponse response = breadRecordComplexService.deleteBreadRecord(USER_ID, RECORD_ID);

        assertTrue(response.getStickerRemoved());
        verify(breadRecordService).deleteBreadRecord(breadRecord);
        verify(breadService).deleteIfUserCreated(bread);
    }

    private Bread createBread(UUID createdBy) {
        return Bread.builder()
                .id(UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789"))
                .stickerNumber(7)
                .name("크루아상")
                .breadType(BreadType.PASTRY)
                .imageUrl("https://cdn.bread-diary.app/breads/croissant.webp")
                .createdBy(createdBy)
                .build();
    }

    private BreadRecord createBreadRecord(Bread bread) {
        return BreadRecord.builder()
                .id(RECORD_ID)
                .userId(USER_ID)
                .bread(bread)
                .photoUrl("https://cdn.bread-diary.app/bread-photos/record.webp")
                .shopName("르뺑블루")
                .eatenDate(LocalDate.of(2026, 3, 14))
                .rating(5)
                .review("맛있어요.")
                .build();
    }
}
