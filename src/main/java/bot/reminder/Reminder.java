package bot.reminder;

import bot.reminder.once.OnceProperties;
import bot.reminder.recurring.RecurringProperties;

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
    public Boolean isEnabled() {
        if (type == ReminderType.ONCE) {
            OnceProperties props = getPropertiesAs(OnceProperties.class);
            return props.enabled != null && props.enabled;
        } else if (type == ReminderType.RECURRING) {
            RecurringProperties props = getPropertiesAs(RecurringProperties.class);
            return props.enabled != null && props.enabled;
        }
        return true; // по умолчанию
    }
    public String getGroup() {
        if (type == ReminderType.ONCE) {
            OnceProperties props = getPropertiesAs(OnceProperties.class);
            return props.group != null ? props.group : "";
        } else if (type == ReminderType.RECURRING) {
            RecurringProperties props = getPropertiesAs(RecurringProperties.class);
            return props.group != null ? props.group : "";
        }
        return "";
    }

    public <T> T getPropertiesAs(Class<T> clazz) {
        return ReminderService.parseProperties(propertiesJson, clazz);
    }
}