package com.bean.breaddiary.domain.auth.client;

import com.bean.breaddiary.global.logging.RequestLogContext;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Slf4j
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
            @Qualifier("tossRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            @Value("${TOSS_UNLINK_ACCESS_TOKEN:${app.auth.toss.unlink-access-token:}}") String unlinkAccessToken
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.unlinkAccessToken = unlinkAccessToken;
    }

    public TossGenerateTokenSuccess exchangeAuthorizationCode(
            String authorizationCode,
            String referrer
    ) {
        long startNanos = System.nanoTime();

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

            logTossSuccess("tossExchange", startNanos);
            return response.success();
        } catch (RestClientResponseException exception) {
            logTossFailure(
                    "tossExchange",
                    exception.getStatusCode().value(),
                    extractErrorCode(exception.getResponseBodyAsString()),
                    startNanos,
                    exception
            );
            throw convertException(exception, "토스 토큰 발급에 실패했습니다.");
        } catch (ResponseStatusException exception) {
            logTossFailure("tossExchange", exception.getStatusCode().value(), null, startNanos, exception);
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpectedTossFailure("tossExchange", startNanos, exception);
            throw exception;
        }
    }

    public TossLoginMeSuccess getUserInfo(String tossAccessToken) {
        long startNanos = System.nanoTime();

        try {
            TossLoginMeResponse response = restClient.get()
                    .uri(LOGIN_ME_PATH)
                    .header("Authorization", "Bearer " + tossAccessToken)
                    .retrieve()
                    .body(TossLoginMeResponse.class);

            if (response == null || response.success() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "토스 사용자 정보 조회에 실패했습니다.");
            }

            logTossSuccess("tossLoginMe", startNanos);
            return response.success();
        } catch (RestClientResponseException exception) {
            logTossFailure(
                    "tossLoginMe",
                    exception.getStatusCode().value(),
                    extractErrorCode(exception.getResponseBodyAsString()),
                    startNanos,
                    exception
            );
            throw convertException(exception, "토스 사용자 정보 조회에 실패했습니다.");
        } catch (ResponseStatusException exception) {
            logTossFailure("tossLoginMe", exception.getStatusCode().value(), null, startNanos, exception);
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpectedTossFailure("tossLoginMe", startNanos, exception);
            throw exception;
        }
    }

    public void unlinkByUserKey(String userKey) {
        long startNanos = System.nanoTime();

        try {
            requireUnlinkAccessToken();

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
            logTossSuccess("tossUnlink", startNanos);
        } catch (RestClientResponseException exception) {
            logTossFailure(
                    "tossUnlink",
                    HttpStatus.BAD_GATEWAY.value(),
                    extractErrorCode(exception.getResponseBodyAsString()),
                    startNanos,
                    exception
            );
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "토스 연결 끊기에 실패했습니다.",
                    exception
            );
        } catch (ResponseStatusException exception) {
            logTossFailure("tossUnlink", exception.getStatusCode().value(), null, startNanos, exception);
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpectedTossFailure("tossUnlink", startNanos, exception);
            throw exception;
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

    private void logTossSuccess(String action, long startNanos) {
        log.info(
                "TOSS action={} result=success requestId={} status={} durationMs={}",
                action,
                RequestLogContext.currentRequestIdOrDefault(),
                HttpStatus.OK.value(),
                durationMs(startNanos)
        );
    }

    private void logTossFailure(
            String action,
            int status,
            String errorCode,
            long startNanos,
            Exception exception
    ) {
        if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error(
                    "TOSS action={} result=fail requestId={} status={} errorCode={} exceptionType={} durationMs={}",
                    action,
                    RequestLogContext.currentRequestIdOrDefault(),
                    status,
                    valueOrDefault(errorCode),
                    exception.getClass().getSimpleName(),
                    durationMs(startNanos)
            );
            return;
        }

        log.warn(
                "TOSS action={} result=fail requestId={} status={} errorCode={} exceptionType={} durationMs={}",
                action,
                RequestLogContext.currentRequestIdOrDefault(),
                status,
                valueOrDefault(errorCode),
                exception.getClass().getSimpleName(),
                durationMs(startNanos)
        );
    }

    private void logUnexpectedTossFailure(String action, long startNanos, RuntimeException exception) {
        log.error(
                "TOSS action={} result=fail requestId={} status={} errorCode={} exceptionType={} durationMs={}",
                action,
                RequestLogContext.currentRequestIdOrDefault(),
                "unexpected",
                "-",
                exception.getClass().getSimpleName(),
                durationMs(startNanos)
        );
    }

    private long durationMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String valueOrDefault(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private record TossGenerateTokenRequest(
            @JsonProperty("authorizationCode")
            String authorizationCode,
            @JsonProperty("referrer")
            String referrer
    ) {
    }

    private record TossUnlinkRequest(
            @JsonProperty("userKey")
            String userKey
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossGenerateTokenResponse(
            @JsonProperty("resultType")
            String resultType,
            @JsonProperty("success")
            TossGenerateTokenSuccess success,
            @JsonProperty("error")
            TossApiError error
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossGenerateTokenSuccess(
            @JsonProperty("accessToken")
            String accessToken,
            @JsonProperty("refreshToken")
            String refreshToken,
            @JsonProperty("tokenType")
            String tokenType,
            @JsonProperty("expiresIn")
            Long expiresIn,
            @JsonProperty("scope")
            String scope
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossLoginMeResponse(
            @JsonProperty("resultType")
            String resultType,
            @JsonProperty("success")
            TossLoginMeSuccess success,
            @JsonProperty("error")
            TossApiError error
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossLoginMeSuccess(
            @JsonProperty("userKey")
            JsonNode userKey,
            @JsonProperty("name")
            String name,
            @JsonProperty("email")
            String email
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossUnlinkResponse(
            @JsonProperty("resultType")
            String resultType,
            @JsonProperty("success")
            JsonNode success,
            @JsonProperty("error")
            TossApiError error
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record TossApiError(
            @JsonProperty("errorCode")
            String errorCode,
            @JsonProperty("reason")
            String reason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TossGrantError(
            @JsonProperty("error")
            String error
    ) {
    }
}
