package com.bean.breaddiary.global.s3;

import com.bean.breaddiary.global.image.ProcessedBreadPhoto;
import com.bean.breaddiary.global.image.ResizedImage;
import com.bean.breaddiary.global.image.ImageResizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private static final long MAX_PHOTO_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final S3Client s3Client;
    private final ImageResizeService imageResizeService;

    @Value("${app.aws.s3.region}")
    private String region;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.s3.public-base-url:}")
    private String publicBaseUrl;

    @Value("${app.aws.s3.bread-photo-prefix:bread-photos}")
    private String breadPhotoPrefix;

    public String uploadBreadPhoto(UUID userId, MultipartFile photo) {
        validateS3Properties();
        validatePhoto(photo);

        ProcessedBreadPhoto processedBreadPhoto = imageResizeService.processBreadPhoto(photo);
        String imageId = UUID.randomUUID().toString();
        String originalKey = createBreadPhotoKey(userId, imageId, processedBreadPhoto.getOriginal());
        String thumbnailKey = createThumbnailKey(originalKey);

        try {
            putObject(originalKey, processedBreadPhoto.getOriginal());
            putObject(thumbnailKey, processedBreadPhoto.getThumbnail());
        } catch (S3Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다.", exception);
        }

        return createPublicUrl(originalKey);
    }

    private void validateS3Properties() {
        if (!StringUtils.hasText(region)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 region 설정이 필요합니다.");
        }
        if (!StringUtils.hasText(bucket)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 bucket 설정이 필요합니다.");
        }
    }

    private void validatePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진은 필수입니다.");
        }

        if (photo.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "사진 크기는 10MB 이하여야 합니다.");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(photo.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진은 JPEG, PNG, WebP 형식만 업로드할 수 있습니다.");
        }
    }

    private void putObject(String key, ResizedImage image) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(image.getContentType())
                .contentLength((long) image.getBytes().length)
                .build();

        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(image.getBytes())
        );
    }

    private String createBreadPhotoKey(UUID userId, String imageId, ResizedImage image) {
        return "%s/%s/%s%s".formatted(
                normalizePrefix(breadPhotoPrefix),
                userId,
                imageId,
                image.getExtension()
        );
    }

    private String createThumbnailKey(String originalKey) {
        int extensionIndex = originalKey.lastIndexOf('.');
        if (extensionIndex < 0) {
            return originalKey + "_thumb";
        }

        return originalKey.substring(0, extensionIndex)
                + "_thumb"
                + originalKey.substring(extensionIndex);
    }

    private String createPublicUrl(String key) {
        if (StringUtils.hasText(publicBaseUrl)) {
            return "%s/%s".formatted(removeTrailingSlash(publicBaseUrl), key);
        }

        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                bucket,
                region,
                key
        );
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "bread-photos";
        }
        return removeTrailingSlash(prefix);
    }

    private String removeTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
