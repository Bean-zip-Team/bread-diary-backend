package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.dto.request.LogoutRequest;
import com.bean.breaddiary.domain.auth.dto.request.RefreshTokenRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.auth.entity.UserSession;
import com.bean.breaddiary.domain.auth.repository.UserSessionRepository;
import com.bean.breaddiary.domain.bread.repository.BreadRepository;
import com.bean.breaddiary.domain.breadrecord.repository.BreadRecordRepository;
import com.bean.breaddiary.domain.user.entity.User;
import com.bean.breaddiary.domain.user.repository.UserRepository;
import com.bean.breaddiary.domain.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "app.auth.jwt.secret=test-secret-key",
        "app.auth.jwt.issuer=bread-diary",
        "app.auth.toss.base-url=http://localhost:65535",
        "TOSS_WEBHOOK_SECRET=test-webhook-secret",
        "app.aws.s3.region=ap-northeast-2",
        "app.aws.s3.bucket=test-bucket",
        "cloud.aws.credentials.accessKey=test-access-key",
        "cloud.aws.credentials.secretKey=test-secret-key"
})
class AuthRefreshMysqlIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("breaddiary_test")
            .withUsername("appuser")
            .withPassword("qwer123!@");

    private static final String INITIAL_JTI = "initial-refresh-jti";

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private BreadRepository breadRepository;

    @Autowired
    private BreadRecordRepository breadRecordRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private ExecutorService executorService;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        executorService = Executors.newFixedThreadPool(2);

        transactionTemplate.executeWithoutResult(status -> {
            userSessionRepository.deleteAll();
            breadRecordRepository.deleteAll();
            breadRepository.deleteAll();
            userRepository.deleteAll();
        });
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void findSessionForUpdateAppliesMysqlRowLock() throws Exception {
        RefreshFixture fixture = createRefreshFixture();
        CountDownLatch firstTransactionLocked = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);

        Future<?> firstLock = executorService.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            userSessionRepository.findByIdAndRevokedAtIsNullForUpdate(fixture.sessionId())
                    .orElseThrow();
            firstTransactionLocked.countDown();
            await(releaseFirstTransaction);
        }));

        assertTrue(firstTransactionLocked.await(5, TimeUnit.SECONDS));

        Future<Optional<UserSession>> competingLock = executorService.submit(() -> transactionTemplate.execute(status ->
                userSessionRepository.findByIdAndRevokedAtIsNullForUpdate(fixture.sessionId())
        ));

        Thread.sleep(300);
        assertFalse(competingLock.isDone());

        releaseFirstTransaction.countDown();

        firstLock.get(5, TimeUnit.SECONDS);
        assertTrue(competingLock.get(5, TimeUnit.SECONDS).isPresent());
    }

    @Test
    void concurrentRefreshWithSameTokenAllowsOnlyOneSuccess() throws Exception {
        RefreshFixture fixture = createRefreshFixture();
        CountDownLatch startLatch = new CountDownLatch(1);
        Callable<RefreshResult> refreshAttempt = () -> {
            startLatch.await();
            return refresh(fixture.refreshToken());
        };

        Future<RefreshResult> firstAttempt = executorService.submit(refreshAttempt);
        Future<RefreshResult> secondAttempt = executorService.submit(refreshAttempt);

        startLatch.countDown();

        List<RefreshResult> results = List.of(
                firstAttempt.get(10, TimeUnit.SECONDS),
                secondAttempt.get(10, TimeUnit.SECONDS)
        );

        long successCount = results.stream()
                .filter(RefreshResult::success)
                .count();
        long conflictCount = results.stream()
                .filter(result -> HttpStatus.CONFLICT.equals(result.status()))
                .count();

        assertEquals(1L, successCount);
        assertEquals(1L, conflictCount);
    }

    @Test
    void refreshAfterLogoutFails() {
        RefreshFixture fixture = createRefreshFixture();

        authService.logout(new LogoutRequest(fixture.refreshToken()));

        RefreshResult result = refresh(fixture.refreshToken());

        assertFalse(result.success());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void refreshAfterWithdrawalFails() {
        RefreshFixture fixture = createRefreshFixture();
        User user = userRepository.findById(fixture.userId())
                .orElseThrow();

        userService.hardDeleteUserData(user);

        RefreshResult result = refresh(fixture.refreshToken());

        assertFalse(result.success());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    private RefreshFixture createRefreshFixture() {
        return transactionTemplate.execute(status -> {
            User user = userRepository.save(User.builder()
                    .tossUserKey("toss-user-key-" + UUID.randomUUID())
                    .nickname("bread-lover")
                    .email("bread-" + UUID.randomUUID() + "@toss.im")
                    .build());

            LocalDateTime issuedAt = LocalDateTime.now().minusMinutes(1);
            LocalDateTime refreshExpiresAt = issuedAt.plusDays(30);
            UserSession userSession = userSessionRepository.save(UserSession.builder()
                    .userId(user.getId())
                    .refreshTokenHash("__PENDING_REFRESH_TOKEN__")
                    .currentJti("pending-" + UUID.randomUUID())
                    .refreshExpiresAt(refreshExpiresAt)
                    .build());

            String refreshToken = jwtTokenProvider.createRefreshToken(
                    user.getId(),
                    userSession.getId(),
                    INITIAL_JTI,
                    issuedAt,
                    refreshExpiresAt
            );
            userSession.rotateRefreshToken(
                    hash(refreshToken),
                    INITIAL_JTI,
                    refreshExpiresAt
            );

            return new RefreshFixture(
                    user.getId(),
                    userSession.getId(),
                    refreshToken
            );
        });
    }

    private RefreshResult refresh(String refreshToken) {
        try {
            AuthTokenResponse response = authService.refresh(new RefreshTokenRequest(refreshToken));
            return new RefreshResult(true, null, response.getRefreshToken());
        } catch (ResponseStatusException exception) {
            return new RefreshResult(false, exception.getStatusCode(), null);
        }
    }

    private String hash(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for lock verification.", exception);
        }
    }

    private record RefreshFixture(
            UUID userId,
            UUID sessionId,
            String refreshToken
    ) {
    }

    private record RefreshResult(
            boolean success,
            HttpStatusCode status,
            String refreshToken
    ) {
    }
}
