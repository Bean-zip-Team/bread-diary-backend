package com.bean.breaddiary.global.s3;

import com.bean.breaddiary.global.image.ImageResizeService;
import com.bean.breaddiary.global.image.ProcessedBreadPhoto;
import com.bean.breaddiary.global.image.ResizedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3UploadServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private S3Client s3Client;
    private ImageResizeService imageResizeService;
    private S3UploadService s3UploadService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        imageResizeService = mock(ImageResizeService.class);
        s3UploadService = new S3UploadService(s3Client, imageResizeService);

        ReflectionTestUtils.setField(s3UploadService, "region", "ap-northeast-2");
        ReflectionTestUtils.setField(s3UploadService, "bucket", "breaddiary-bucket");
        ReflectionTestUtils.setField(s3UploadService, "publicBaseUrl", "https://cdn.example.com");
        ReflectionTestUtils.setField(s3UploadService, "breadPhotoPrefix", "bread-photos");
    }

    @Test
    void uploadBreadPhotoUploadsOriginalAndThumbnailAndReturnsOriginalCloudFrontUrl() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "bread.jpg",
                "image/jpeg",
                "photo".getBytes()
        );
        when(imageResizeService.processBreadPhoto(photo)).thenReturn(new ProcessedBreadPhoto(
                new ResizedImage("original".getBytes(), "image/webp", ".webp"),
                new ResizedImage("thumbnail".getBytes(), "image/webp", ".webp")
        ));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String photoUrl = s3UploadService.uploadBreadPhoto(USER_ID, photo);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, org.mockito.Mockito.times(2)).putObject(requestCaptor.capture(), any(RequestBody.class));
        List<PutObjectRequest> requests = requestCaptor.getAllValues();
        String originalKey = requests.get(0).key();
        String thumbnailKey = requests.get(1).key();

        assertTrue(originalKey.matches("bread-photos/%s/[0-9a-f\\-]{36}\\.webp".formatted(USER_ID)));
        assertEquals(originalKey.replace(".webp", "_thumb.webp"), thumbnailKey);
        assertEquals("image/webp", requests.get(0).contentType());
        assertEquals("image/webp", requests.get(1).contentType());
        assertEquals(8L, requests.get(0).contentLength());
        assertEquals(9L, requests.get(1).contentLength());
        assertEquals("https://cdn.example.com/" + originalKey, photoUrl);
    }

    @Test
    void uploadBreadPhotoRejectsUnsupportedContentType() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "bread.gif",
                "image/gif",
                "photo".getBytes()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> s3UploadService.uploadBreadPhoto(USER_ID, photo)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void uploadBreadPhotoRejectsOver10MbPhoto() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "large.jpg",
                "image/jpeg",
                new byte[10 * 1024 * 1024 + 1]
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> s3UploadService.uploadBreadPhoto(USER_ID, photo)
        );

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, exception.getStatusCode());
    }
}
