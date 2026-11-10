package com.mundodev.filafacil.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void safeSummaryNaoIncluiSenha() {
        AppConfig config = new AppConfig(
                "local",
                8080,
                "jdbc:postgresql://localhost:5432/filafacil",
                "filafacil",
                "senha-secreta-123",
                "",
                "us-east-1",
                "data/anexos",
                2048);
        String summary = config.safeSummary();
        assertFalse(summary.contains("senha-secreta-123"));
        assertTrue(summary.contains("mode=local"));
        assertTrue(summary.contains("port=8080"));
    }

    @Test
    void redactUrlRemoveCredencialEmbutida() {
        String redacted = AppConfig.redactUrl("jdbc:postgresql://user:secret@localhost:5432/db");
        assertFalse(redacted.contains("secret"));
        assertTrue(redacted.contains(":***@"));
    }

    @Test
    void fromEnvUsaDefaultsLocais() {
        AppConfig config = AppConfig.fromEnv();
        assertEquals("local", config.mode());
        assertTrue(config.port() > 0);
        assertTrue(config.local());
    }
}
