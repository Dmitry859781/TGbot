package bot.reminder;

import bot.reminder.once.OnceProperties;
import bot.reminder.recurring.RecurringProperties;
import bot.reminder.recurring.ScheduleItem;
import bot.timezone.UserTimezoneService;

import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReminderService {

    private static final String DB_URL = "jdbc:sqlite:database/Reminder.db";
    private static final Gson GSON = new Gson();
    private final UserTimezoneService timezoneService = new UserTimezoneService();

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    public ReminderService() {
        initializeDatabase();
    }

    public ReminderService(String dbUrl) throws NoSuchFieldException, SecurityException {
        // Для тестов
        Field field = getClass().getDeclaredField("DB_URL");
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
                CREATE TABLE IF NOT EXISTS reminders (
                    user_id INTEGER NOT NULL,
                    reminder_name TEXT NOT NULL,
                    text TEXT NOT NULL,
                    type TEXT NOT NULL CHECK(type IN ('ONCE', 'RECURRING')),
                    properties TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (user_id, reminder_name)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ReminderService database", e);
        }
    }

    public void addOnceReminder(Long userId, String reminderName, String text, LocalDateTime remindAt, 
    		ZoneId userZone, boolean deleteAfterSend) {
        OnceProperties props = new OnceProperties();
        LocalDateTime utcTime = remindAt.atZone(userZone)
                                    .withZoneSameInstant(ZoneOffset.UTC)
                                    .toLocalDateTime();
        props.remind_at = utcTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        props.deleteAfterSend = deleteAfterSend;
        props.group = "";
        props.enabled = true;
        addReminder(userId, reminderName, text, ReminderType.ONCE, props);
    }

    public void addRecurringReminder(Long userId, String reminderName, String text, 
    		RecurringProperties props) {
    	props.group = "";
    	props.enabled = true;
        addReminder(userId, reminderName, text, ReminderType.RECURRING, props);
    }

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

    public void removeReminder(Long userId, String reminderName) throws SQLException {
        String sql = "DELETE FROM reminders WHERE user_id = ? AND reminder_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, reminderName);
            stmt.executeUpdate();
        }
    }

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
        Instant nowInstant = Instant.now();

        try {
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

                    ZoneId userZone;
                    try {
                        Integer offsetHours = timezoneService.getTimezone(reminder.getUserId());
                        userZone = (offsetHours != null)
                            ? ZoneOffset.ofHours(offsetHours)
                            : ZoneId.systemDefault();
                    } catch (SQLException e) {
                        userZone = ZoneId.systemDefault();
                    }

                    LocalDateTime nowLocal = LocalDateTime.ofInstant(nowInstant, userZone);
                    String todayIso = nowLocal.getDayOfWeek().toString();
                    String timeNow = nowLocal.format(DateTimeFormatter.ofPattern("HH:mm"));
                    
                    if (!reminder.isEnabled()) {
                        continue;
                    }
                    
                    boolean shouldNotify = false;

                    if (ReminderType.ONCE.equals(reminder.getType())) {
                        OnceProperties props = reminder.getPropertiesAs(OnceProperties.class);
                        LocalDateTime remindAt = LocalDateTime.parse(props.remind_at,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        Instant remindAtInstant = remindAt.atZone(ZoneOffset.UTC).toInstant();
                        if (!remindAtInstant.isAfter(nowInstant)) {
                            shouldNotify = true;
                        }
                    } else if (ReminderType.RECURRING.equals(reminder.getType())) {
                        RecurringProperties props = reminder.getPropertiesAs(RecurringProperties.class);
                        if (props.schedule != null) {
                            for (ScheduleItem item : props.schedule) {
                                if (todayIso.substring(0, 3).equals(item.day) && timeNow.equals(item.time)) {
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
    
    public void updateReminderProperties(Long userId, String reminderName, Object newProperties) throws SQLException {
        String json = GSON.toJson(newProperties);
        String sql = "UPDATE reminders SET properties = ? WHERE user_id = ? AND reminder_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, json);
            stmt.setLong(2, userId);
            stmt.setString(3, reminderName);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Напоминание не найдено");
            }
        }
    }
    
    public List<String> getUserGroups(Long userId) throws SQLException {
        String sql = """
            SELECT DISTINCT properties FROM reminders WHERE user_id = ?
            """;
        Set<String> groups = new HashSet<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String json = rs.getString("properties");
                    if (json != null) {
                        if (json.contains("remind_at")) {
                            OnceProperties props = GSON.fromJson(json, OnceProperties.class);
                            groups.add(props.group != null ? props.group : "");
                        } else {
                            RecurringProperties props = GSON.fromJson(json, RecurringProperties.class);
                            groups.add(props.group != null ? props.group : "");
                        }
                    }
                }
            }
        }
        List<String> list = new ArrayList<>(groups);
        Collections.sort(list); // чтобы порядок был
        return list;
    }
    
    public static <T> T parseProperties(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
}