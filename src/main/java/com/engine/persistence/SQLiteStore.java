package com.engine.persistence;

import com.engine.engine.StepResult;
import java.sql.*;

public class SQLiteStore {
    private final String dbUrl;

    public SQLiteStore(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        initDb();
    }

    private void initDb() {
        String sql = """
            CREATE TABLE IF NOT EXISTS steps (
                workflow_id TEXT NOT NULL,
                step_key    TEXT NOT NULL,
                status      TEXT NOT NULL,
                output      TEXT,
                PRIMARY KEY (workflow_id, step_key)
            );
        """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;"); // Enables concurrent reads
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize DB", e);
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    // Check if a step already completed
    public synchronized StepResult getStep(String workflowId, String stepKey) {
        String sql = "SELECT status, output FROM steps WHERE workflow_id=? AND step_key=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, workflowId);
            ps.setString(2, stepKey);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new StepResult(stepKey, rs.getString("status"), rs.getString("output"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB read failed", e);
        }
        return null;
    }

    // Save a completed step result
    public synchronized void saveStep(String workflowId, String stepKey,
                                      String status, String output) {
        String sql = """
            INSERT OR REPLACE INTO steps (workflow_id, step_key, status, output)
            VALUES (?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, workflowId);
            ps.setString(2, stepKey);
            ps.setString(3, status);
            ps.setString(4, output);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB write failed", e);
        }
    }
}