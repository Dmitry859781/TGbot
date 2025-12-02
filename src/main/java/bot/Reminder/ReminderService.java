package bot.Reminder;

import bot.Reminder.Reminder;
import bot.Reminder.once.OnceProperties;
import bot.Reminder.recurring.RecurringProperties;
import bot.Reminder.recurring.ScheduleItem;

import com.google.gson.Gson;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

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
     // Конвертируем локальное время сервера в UTC (Переписать для разных временных зон)
        props.remind_at = remindAt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
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
    
    public List<Reminder> getDueReminders() {
        List<Reminder> dueReminders = new ArrayList<>();
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        String todayIso = nowUtc.getDayOfWeek().toString(); // "MON", "TUE", ...

        String timeNow = nowUtc.format(DateTimeFormatter.ofPattern("HH:mm"));
        
        //Для дебага
    	System.out.println("Текущее UTC-время: " + nowUtc);
    	System.out.println("Сегодня: " + todayIso + ", время: " + timeNow);
    	
        try {
            // Получаем все напоминания
            String sql = "SELECT user_id, reminder_name, text, type, properties FROM reminders";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Reminder reminder = new Reminder(
                        rs.getLong("user_id"),
                        rs.getString("reminder_name"),
                        rs.getString("text"),
                        ReminderType.valueOf(rs.getString("type")),
                        rs.getString("properties")
                    );

                    boolean shouldNotify = false;

                    if (ReminderType.ONCE.equals(reminder.getType())) {
                        OnceProperties props = reminder.getPropertiesAs(OnceProperties.class);
                        LocalDateTime remindAt = LocalDateTime.parse(props.remind_at,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        if (!remindAt.isAfter(nowUtc)) {
                            shouldNotify = true;
                            // Удалим после отправки (Возможно добавлю флаг отменяющий это)
                        }
                    } else if (ReminderType.RECURRING.equals(reminder.getType())) {
                        RecurringProperties props = reminder.getPropertiesAs(RecurringProperties.class);
                        if (props.schedule != null) {
                            for (ScheduleItem item : props.schedule) {
                                if (todayIso.equals(item.day) && timeNow.equals(item.time)) {
                                    shouldNotify = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (shouldNotify) {
                        dueReminders.add(reminder);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dueReminders;
    }

    // Вспомогательный метод для парсинга свойств
    public static <T> T parseProperties(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
}