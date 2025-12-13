package bot.timezone;

import java.sql.*;

public class UserTimezoneService {

    private final String DB_URL;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    public UserTimezoneService() {
        this("jdbc:sqlite:database/Timezone.db");
    }

    public UserTimezoneService(String DB_URL) {
        this.DB_URL = DB_URL;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            String dbPath = DB_URL.replace("jdbc:sqlite:", "");
            java.nio.file.Path dbDir = java.nio.file.Paths.get(dbPath).getParent();
            if (dbDir != null && !java.nio.file.Files.exists(dbDir)) {
                java.nio.file.Files.createDirectories(dbDir);
            }

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_timezone (
                    user_id INTEGER PRIMARY KEY,
                    timezone_id INTEGER NOT NULL
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize UserTimezoneService database", e);
        }
    }

    public void saveTimezoneOffset(Long userId, int offsetHours) throws SQLException {
        String sql = "INSERT OR REPLACE INTO user_timezone (user_id, timezone_id) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, offsetHours);
            stmt.executeUpdate();
        }
    }

    public Integer getTimezone(Long userId) throws SQLException {
        String sql = "SELECT timezone_id FROM user_timezone WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("timezone_id");
                }
                return null;
            }
        }
    }
}