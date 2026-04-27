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
    }
}
