package bot.reminder;

public class Reminder {
    private final Long userId;
    private final String reminderName;
    private final String text;
    private final ReminderType type;
    private final String propertiesJson;

    public Reminder(Long userId, String reminderName, String text, ReminderType type, String propertiesJson) {
        this.userId = userId;
        this.reminderName = reminderName;
        this.text = text;
        this.type = type;
        this.propertiesJson = propertiesJson;
    }

    public Long getUserId() { return userId; }
    public String getName() { return reminderName;}
    public String getReminderName() { return reminderName; }
    public String getText() { return text; }
    public ReminderType getType() { return type; }
    public String getPropertiesJson() { return propertiesJson; }

    public <T> T getPropertiesAs(Class<T> clazz) {
        return ReminderService.parseProperties(propertiesJson, clazz);
    }
}