package com.bean.breaddiary.domain.auth.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
public class TossAuthClient {

    private static final String GENERATE_TOKEN_PATH =
            "/api-partner/v1/apps-in-toss/user/oauth2/generate-token";
    private static final String LOGIN_ME_PATH =
            "/api-partner/v1/apps-in-toss/user/oauth2/login-me";
    private static final String REMOVE_BY_USER_KEY_PATH =
            "/api-partner/v1/apps-in-toss/user/oauth2/remove-by-user-key";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String unlinkAccessToken;

    public TossAuthClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.auth.toss.base-url:https://apps-in-toss-api.toss.im}") String baseUrl,
            @Value("${TOSS_UNLINK_ACCESS_TOKEN:${app.auth.toss.unlink-access-token:}}") String unlinkAccessToken
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
        this.unlinkAccessToken = unlinkAccessToken;
    }

    public TossGenerateTokenSuccess exchangeAuthorizationCode(
            String authorizationCode,
            String referrer
    ) {
        try {
            TossGenerateTokenResponse response = restClient.post()
                    .uri(GENERATE_TOKEN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TossGenerateTokenRequest(authorizationCode, referrer))
                    .retrieve()
                    .body(TossGenerateTokenResponse.class);

            if (response == null || response.success() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "토스 토큰 발급에 실패했습니다.");
            }

            return response.success();
        } catch (RestClientResponseException exception) {
            throw convertException(exception, "토스 토큰 발급에 실패했습니다.");
        }
    }

    public TossLoginMeSuccess getUserInfo(String tossAccessToken) {
        try {
            TossLoginMeResponse response = restClient.get()
                    .uri(LOGIN_ME_PATH)
                    .header("Authorization", "Bearer " + tossAccessToken)
                    .retrieve()
                    .body(TossLoginMeResponse.class);

            if (response == null || response.success() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "토스 사용자 정보 조회에 실패했습니다.");
            }

            return response.success();
        } catch (RestClientResponseException exception) {
            throw convertException(exception, "토스 사용자 정보 조회에 실패했습니다.");
        }
    }

    public void unlinkByUserKey(String userKey) {
        requireUnlinkAccessToken();

        try {
            TossUnlinkResponse response = restClient.post()
                    .uri(REMOVE_BY_USER_KEY_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + unlinkAccessToken)
                    .body(new TossUnlinkRequest(userKey))
                    .retrieve()
                    .body(TossUnlinkResponse.class);

            if (response == null || response.success() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "토스 연결 끊기에 실패했습니다.");
            }
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "토스 연결 끊기에 실패했습니다.",
                    exception
            );
        }
    }

    private ResponseStatusException convertException(
            RestClientResponseException exception,
            String fallbackMessage
    ) {
        String errorCode = extractErrorCode(exception.getResponseBodyAsString());

        if ("invalid_grant".equalsIgnoreCase(errorCode)) {
            return new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "토스 인가 코드가 유효하지 않습니다.",
                    exception
            );
        }

        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                fallbackMessage,
                exception
        );
    }

    private String extractErrorCode(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }

        try {
            TossGrantError tossGrantError = objectMapper.readValue(responseBody, TossGrantError.class);
            if (StringUtils.hasText(tossGrantError.error())) {
                return tossGrantError.error();
            }

            TossGenerateTokenResponse tossResponse = objectMapper.readValue(
                    responseBody,
                    TossGenerateTokenResponse.class
            );
            return tossResponse.error() == null ? null : tossResponse.error().errorCode();
        } catch (IOException exception) {
            return null;
        }
    }

    private void requireUnlinkAccessToken() {
        if (!StringUtils.hasText(unlinkAccessToken)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "TOSS_UNLINK_ACCESS_TOKEN 설정이 필요합니다."
            );
        }
    }

    private record TossGenerateTokenRequest(
            String authorizationCode,
            String referrer
    ) {
    }

    private record TossUnlinkRequest(
            String userKey
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossGenerateTokenResponse(
            String resultType,
            TossGenerateTokenSuccess success,
            TossApiError error
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossGenerateTokenSuccess(
            String accessToken,
            String refreshToken,
            String tokenType,
            Long expiresIn,
            String scope
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossLoginMeResponse(
            String resultType,
            TossLoginMeSuccess success,
            TossApiError error
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossLoginMeSuccess(
            JsonNode userKey,
            String name,
            String email
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossUnlinkResponse(
            String resultType,
            JsonNode success,
            TossApiError error
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossApiError(
            String errorCode,
            String reason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TossGrantError(
            String error
    ) {
    }
}
