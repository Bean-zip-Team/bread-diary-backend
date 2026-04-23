package com.bean.breaddiary.domain.auth.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

@Configuration
public class TossRestClientConfig {

    private static final String PKCS12_KEY_STORE_TYPE = "PKCS12";

    @Bean
    @Qualifier("tossRestClient")
    public RestClient tossRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.auth.toss.base-url:https://apps-in-toss-api.toss.im}") String baseUrl,
            @Value("${app.auth.toss.mtls.key-store-path:}") String keyStorePath,
            @Value("${app.auth.toss.mtls.key-store-password:}") String keyStorePassword
    ) {
        RestClient.Builder tossRestClientBuilder = restClientBuilder.baseUrl(baseUrl);
        if (!isMtlsConfigured(keyStorePath, keyStorePassword)) {
            return tossRestClientBuilder.build();
        }

        SSLContext sslContext = createMtlsSslContext(keyStorePath, keyStorePassword);
        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        return tossRestClientBuilder
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    private boolean isMtlsConfigured(String keyStorePath, String keyStorePassword) {
        boolean hasPath = StringUtils.hasText(keyStorePath);
        boolean hasPassword = StringUtils.hasText(keyStorePassword);
        if (hasPath != hasPassword) {
            throw new IllegalStateException(
                    "토스 mTLS 설정은 key-store-path와 key-store-password를 함께 지정해야 합니다."
            );
        }

        return hasPath;
    }

    private SSLContext createMtlsSslContext(String keyStorePath, String keyStorePassword) {
        try {
            char[] password = keyStorePassword.toCharArray();
            KeyStore keyStore = loadKeyStore(keyStorePath, password);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm()
            );
            keyManagerFactory.init(keyStore, password);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
            );
            trustManagerFactory.init((KeyStore) null);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    keyManagerFactory.getKeyManagers(),
                    trustManagerFactory.getTrustManagers(),
                    new SecureRandom()
            );

            return sslContext;
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("토스 mTLS SSL 설정을 초기화할 수 없습니다.", exception);
        }
    }

    private KeyStore loadKeyStore(String keyStorePath, char[] password)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(PKCS12_KEY_STORE_TYPE);
        try (InputStream inputStream = Files.newInputStream(Path.of(keyStorePath))) {
            keyStore.load(inputStream, password);
        }

        return keyStore;
    }
}
