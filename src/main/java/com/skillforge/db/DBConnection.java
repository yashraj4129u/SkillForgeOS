
package com.skillforge.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = setting("SKILLFORGE_DB_URL",
            "jdbc:mysql://localhost:3306/skillforge_db");
    private static final String USER = setting("SKILLFORGE_DB_USER", "root");
    private static final String PASSWORD = setting("SKILLFORGE_DB_PASSWORD", "");
    private static volatile boolean portfolioSchemaReady;
    private static volatile boolean collaborationSchemaReady;

    public static Connection getConnection()
            throws SQLException {

        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        try {
            ensurePortfolioSchema(connection);
            ensureCollaborationSchema(connection);
            return connection;
        } catch (SQLException e) {
            try { connection.close(); } catch (SQLException closeError) { e.addSuppressed(closeError); }
            throw e;
        }
    }

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /** Applies the small additive schema needed by reviews and squad timelines. */
    private static synchronized void ensureCollaborationSchema(Connection connection)
            throws SQLException {
        if (collaborationSchemaReady) return;

        if (tableExists(connection, "milestones")) {
            ensureColumn(connection, "milestones", "attachment_name", "VARCHAR(255) NULL");
            ensureColumn(connection, "milestones", "attachment", "MEDIUMBLOB NULL");
            ensureColumn(connection, "milestones", "resource_link", "VARCHAR(1000) NULL");
        }

        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS milestone_submissions (
                    submission_id INT AUTO_INCREMENT PRIMARY KEY,
                    milestone_id INT NOT NULL,
                    participant_id VARCHAR(64) NOT NULL,
                    submission_link VARCHAR(1000) NULL,
                    file_name VARCHAR(255) NULL,
                    file_data MEDIUMBLOB NULL,
                    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uq_milestone_participant (milestone_id, participant_id)
                )
                """);
        }

        if (tableExists(connection, "milestone_submissions")) {
            ensureColumn(connection, "milestone_submissions", "review_status",
                    "VARCHAR(24) NOT NULL DEFAULT 'PENDING'");
            ensureColumn(connection, "milestone_submissions", "review_feedback", "TEXT NULL");
        }

        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS activity_events (
                    event_id INT AUTO_INCREMENT PRIMARY KEY,
                    team_id INT NOT NULL,
                    actor_user_id INT NULL,
                    event_type VARCHAR(40) NOT NULL,
                    description VARCHAR(500) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_activity_team_time (team_id, created_at)
                )
                """);
        }
        collaborationSchemaReady = true;
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (var check = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?")) {
            check.setString(1, table);
            try (var result = check.executeQuery()) { result.next(); return result.getInt(1) > 0; }
        }
    }

    private static void ensureColumn(Connection connection, String table, String column,
                                     String definition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema=DATABASE() AND table_name=? AND column_name=?";
        try (var check = connection.prepareStatement(sql)) {
            check.setString(1, table); check.setString(2, column);
            try (var result = check.executeQuery()) {
                result.next();
                if (result.getInt(1) == 0) try (var alter = connection.createStatement()) {
                    alter.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                }
            }
        }
    }

    private static synchronized void ensurePortfolioSchema(Connection connection)
            throws SQLException {
        if (portfolioSchemaReady) return;

        String[][] fields = {
                {"github_url", "VARCHAR(255) NULL"},
                {"resume_file_name", "VARCHAR(255) NULL"},
                {"resume_file", "MEDIUMBLOB NULL"}
        };
        String existsSql = "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema=DATABASE() AND table_name='participants' AND column_name=?";
        for (String[] field : fields) {
            try (var check = connection.prepareStatement(existsSql)) {
                check.setString(1, field[0]);
                try (var result = check.executeQuery()) {
                    result.next();
                    if (result.getInt(1) == 0) {
                        try (var alter = connection.createStatement()) {
                            alter.executeUpdate("ALTER TABLE participants ADD COLUMN " +
                                    field[0] + " " + field[1]);
                        }
                    }
                }
            }
        }
        portfolioSchemaReady = true;
    }
}

