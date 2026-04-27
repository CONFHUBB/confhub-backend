package com.capstone.confhub.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs ALTER TABLE statements on startup to add any missing columns
 * that Hibernate ddl-auto=update may have failed to create.
 */
@Component
public class DatabaseMigration {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrate() {
        // paper_files: add is_copyright_submission if missing
        try {
            jdbcTemplate.execute(
                "ALTER TABLE paper_files ADD COLUMN IF NOT EXISTS is_copyright_submission BOOLEAN NOT NULL DEFAULT false"
            );
            System.out.println("[DatabaseMigration] Column 'is_copyright_submission' ensured on paper_files.");
        } catch (Exception e) {
            System.out.println("[DatabaseMigration] Skipping is_copyright_submission: " + e.getMessage());
        }

        // users: add status and backfill existing records
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20)");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS status_until TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN status SET DEFAULT 'AVAILABLE'");
            jdbcTemplate.execute("UPDATE users SET status = 'AVAILABLE' WHERE status IS NULL");
            jdbcTemplate.execute("UPDATE users SET status = 'AVAILABLE' WHERE status <> 'AVAILABLE' AND status_until IS NULL");
            jdbcTemplate.execute("UPDATE users SET status_until = NULL WHERE status = 'AVAILABLE'");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN status SET NOT NULL");
            System.out.println("[DatabaseMigration] Columns 'status' and 'status_until' ensured on users.");
        } catch (Exception e) {
            System.out.println("[DatabaseMigration] Skipping users status migration: " + e.getMessage());
        }
    }
}
