package com.capstone.confhub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Auto-fix schema issues that Hibernate ddl-auto=update cannot handle
 * (e.g. widening existing VARCHAR columns).
 * Safe to run multiple times — ALTER TYPE is idempotent in PostgreSQL.
 */
@Component
@Slf4j
public class SchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public SchemaMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            log.info("[SchemaMigration] Applying column type fixes...");
            jdbc.execute("ALTER TABLE papers ALTER COLUMN title TYPE VARCHAR(1000)");
            jdbc.execute("ALTER TABLE papers ALTER COLUMN abstract TYPE TEXT");
            log.info("[SchemaMigration] Column type fixes applied successfully.");
        } catch (Exception e) {
            log.warn("[SchemaMigration] Could not apply column fixes (may already be correct): {}", e.getMessage());
        }
    }
}
