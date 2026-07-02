package com.bean.breaddiary.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtTokenProvider {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final String secret;
    private final String issuer;

    public JwtTokenProvider(
            ObjectMapper objectMapper,
            @Value("${app.auth.jwt.secret:}") String secret,
            @Value("${app.auth.jwt.issuer:bread-diary}") String issuer
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.issuer = issuer;
    }

    public String createAccessToken(
            UUID userId,
            UUID sessionId,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        return createToken(userId, sessionId, "access", null, issuedAt, expiresAt);
    }

    public String createRefreshToken(
            UUID userId,
            UUID sessionId,
            String jti,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        return createToken(userId, sessionId, "refresh", jti, issuedAt, expiresAt);
    }

    public JwtTokenClaims parseToken(String token) {
        validateSecretConfigured();

        String[] segments = splitToken(token);
        String unsignedToken = segments[0] + "." + segments[1];
        String expectedSignature = sign(unsignedToken);

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                segments[2].getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        Map<String, Object> claims = readClaims(segments[1]);
        validateIssuer(claims.get("iss"));

        return new JwtTokenClaims(
                UUID.fromString(requiredStringClaim(claims, "sub")),
                UUID.fromString(requiredStringClaim(claims, "sid")),
                requiredStringClaim(claims, "type"),
                optionalStringClaim(claims, "jti"),
                toLocalDateTime(claims.get("iat")),
                toLocalDateTime(claims.get("exp"))
        );
    }

    private String createToken(
            UUID userId,
            UUID sessionId,
            String type,
            String jti,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        validateSecretConfigured();

        try {
            String encodedHeader = encodeJson(Map.of(
                    "alg", "HS256",
                    "typ", "JWT"
            ));

            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("iss", issuer);
            claims.put("sub", userId.toString());
            claims.put("sid", sessionId.toString());
            claims.put("type", type);
            claims.put("iat", toEpochSecond(issuedAt));
            claims.put("exp", toEpochSecond(expiresAt));

            if (StringUtils.hasText(jti)) {
                claims.put("jti", jti);
            }

            String encodedPayload = encodeJson(claims);
            String unsignedToken = encodedHeader + "." + encodedPayload;

            return unsignedToken + "." + sign(unsignedToken);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JWT 생성에 실패했습니다.",
                    exception
            );
        }
    }

    private String encodeJson(Map<String, Object> value) throws JsonProcessingException {
        return URL_ENCODER.encodeToString(
                objectMapper.writeValueAsBytes(value)
        );
    }

    private String[] splitToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 비어 있습니다.");
        }

        String[] segments = token.split("\\.");
        if (segments.length != 3) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 형식이 올바르지 않습니다.");
        }

        return segments;
    }

    private Map<String, Object> readClaims(String payloadSegment) {
        try {
            return objectMapper.readValue(
                    URL_DECODER.decode(payloadSegment),
                    new TypeReference<>() {
                    }
            );
        } catch (IllegalArgumentException | IOException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 해석에 실패했습니다.", exception);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(
                    mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8))
            );
        } catch (GeneralSecurityException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JWT 서명 생성에 실패했습니다.",
                    exception
            );
        }
    }

    private void validateIssuer(Object issuerClaim) {
        String tokenIssuer = issuerClaim == null ? null : issuerClaim.toString();

        if (!StringUtils.hasText(tokenIssuer) || !issuer.equals(tokenIssuer)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 발급자가 올바르지 않습니다.");
        }
    }

    private String requiredStringClaim(Map<String, Object> claims, String key) {
        String value = optionalStringClaim(claims, key);
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 claim이 올바르지 않습니다.");
        }
        return value;
    }

    private String optionalStringClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        return value == null ? null : value.toString();
    }

    private long toEpochSecond(LocalDateTime value) {
        return value.toEpochSecond(ZoneOffset.UTC);
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (!(value instanceof Number number)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 시간 claim이 올바르지 않습니다.");
        }

        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(number.longValue()),
                ZoneOffset.UTC
        );
    }

    private void validateSecretConfigured() {
        if (!StringUtils.hasText(secret)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JWT secret 설정이 필요합니다."
            );
        }
    }

    public record JwtTokenClaims(
            UUID userId,
            UUID sessionId,
            String type,
            String jti,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        public boolean isExpiredAt(LocalDateTime now) {
            LocalDateTime targetTime = now == null ? LocalDateTime.now() : now;
            return !expiresAt.isAfter(targetTime);
        }
    }
}
