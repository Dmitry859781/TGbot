package bot.Reminder;

import bot.Reminder.Reminder;
import bot.Reminder.once.OnceProperties;
import bot.Reminder.recurring.RecurringProperties;
import com.google.gson.Gson;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReminderService {

    private static final String DB_URL = "jdbc:sqlite:database/Reminder.db";
    private static final Gson GSON = new Gson();

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }
    
    // Добавление одноразового напоминания primarykey(userId, reminderName)
    public void addOnceReminder(Long userId, String reminderName, String text, LocalDateTime remindAt) {
        OnceProperties props = new OnceProperties();
        // Всегда сохраняем в UTC как строку
        props.remind_at = remindAt.atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        addReminder(userId, reminderName, text, ReminderType.ONCE, props);
    }

    // Добавление одноразового напоминания primarykey(userId, reminderName)
    public void addRecurringReminder(Long userId, String reminderName, String text, RecurringProperties props) {
        addReminder(userId, reminderName, text, ReminderType.RECURRING, props);
    }

    //Добавление "универсального" оповещения
    private void addReminder(Long userId, String reminderName, String text, ReminderType type, Object properties) {
        String json = GSON.toJson(properties);
        String sql = """
            INSERT INTO reminders (user_id, reminder_name, text, type, properties)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, reminderName);
            stmt.setString(3, text);
            stmt.setString(4, type.name());
            stmt.setString(5, json);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add reminder", e);
        }
    }

    // Удаление оповещения
    public void removeReminder(Long userId, String reminderName) throws SQLException {
        String sql = "DELETE FROM reminders WHERE user_id = ? AND reminder_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, reminderName);
            stmt.executeUpdate();
        }
    }

    // Получение оповещения
    public Reminder getReminder(Long userId, String reminderName) throws SQLException {
        String sql = "SELECT type, text, properties FROM reminders WHERE user_id = ? AND reminder_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, reminderName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Reminder(
                        userId,
                        reminderName,
                        rs.getString("text"),
                        ReminderType.valueOf(rs.getString("type")),
                        rs.getString("properties")
                    );
                }
                return null;
            }
        }
    }

    // Получение списка имён заметок пользователя
    public List<String> getUserReminderNames(Long userId) throws SQLException {
        String sql = "SELECT reminder_name FROM reminders WHERE user_id = ? ORDER BY reminder_name";
        List<String> names = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("reminder_name"));
                }
            }
        }
        return names;
    }

    // Вспомогательный метод для парсинга свойств
    public static <T> T parseProperties(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
}