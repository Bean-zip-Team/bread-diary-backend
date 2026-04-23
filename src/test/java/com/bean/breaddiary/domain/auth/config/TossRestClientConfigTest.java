package com.bean.breaddiary.domain.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TossRestClientConfigTest {

    private final TossRestClientConfig tossRestClientConfig = new TossRestClientConfig();

    @Test
    void tossRestClientBuildsWithoutMtlsWhenKeyStoreIsMissing() {
        assertDoesNotThrow(() -> tossRestClientConfig.tossRestClient(
                RestClient.builder(),
                "https://toss.example",
                "",
                ""
        ));
    }

    @Test
    void tossRestClientRejectsPartialMtlsConfiguration() {
        assertThrows(IllegalStateException.class, () -> tossRestClientConfig.tossRestClient(
                RestClient.builder(),
                "https://toss.example",
                "/tmp/toss-client.p12",
                ""
        ));
    }

    @Test
    void tossRestClientBuildsWithPkcs12KeyStore() throws Exception {
        String password = "changeit";
        Path keyStorePath = createEmptyPkcs12KeyStore(password);

        assertDoesNotThrow(() -> tossRestClientConfig.tossRestClient(
                RestClient.builder(),
                "https://toss.example",
                keyStorePath.toString(),
                password
        ));
    }

    private Path createEmptyPkcs12KeyStore(String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password.toCharArray());

        Path keyStorePath = Files.createTempFile("toss-client-test-", ".p12");
        try (OutputStream outputStream = Files.newOutputStream(keyStorePath)) {
            keyStore.store(outputStream, password.toCharArray());
        }

        return keyStorePath;
    }
}
