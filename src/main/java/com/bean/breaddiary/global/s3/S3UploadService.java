package com.bean.breaddiary.global.s3;

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

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final S3Client s3Client;

    @Value("${app.aws.s3.region}")
    private String region;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.s3.public-base-url:}")
    private String publicBaseUrl;

    @Value("${app.aws.s3.bread-photo-prefix:bread}")
    private String breadPhotoPrefix;

    public String uploadBreadPhoto(UUID userId, MultipartFile photo) {
        validateS3Properties();
        validatePhoto(photo);

        String key = createBreadPhotoKey(userId, photo);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(photo.getContentType())
                .contentLength(photo.getSize())
                .build();

        try {
            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(photo.getInputStream(), photo.getSize())
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 파일을 읽을 수 없습니다.", exception);
        } catch (S3Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다.", exception);
        }

        return createPublicUrl(key);
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

        if (!ALLOWED_CONTENT_TYPES.contains(photo.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진은 JPEG, PNG, WebP 형식만 업로드할 수 있습니다.");
        }
    }

    private String createBreadPhotoKey(UUID userId, MultipartFile photo) {
        return "%s/%s/%s%s".formatted(
                normalizePrefix(breadPhotoPrefix),
                userId,
                UUID.randomUUID(),
                resolveExtension(photo.getContentType())
        );
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다.");
        };
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
            return "bread";
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
