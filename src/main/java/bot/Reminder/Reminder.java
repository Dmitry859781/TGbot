package bot.Reminder;

import com.google.gson.Gson;

public class Reminder {
    private Long userId;
    private String reminderName;
    private String text;
    private ReminderType type;
    private String propertiesJson; // "сырой" JSON
    
    public Reminder(Long userId, String reminderName, String text, ReminderType type, String propertiesJson) {
        this.userId = userId;
        this.reminderName = reminderName;
        this.text = text;
        this.type = type;
        this.propertiesJson = propertiesJson;
    }
    
	// Геттеры
    public <T> T getPropertiesAs(Class<T> clazz) {
        return new Gson().fromJson(propertiesJson, clazz);
    }
    
    public Long getUserId() {
    	return userId;
    }
    
    public ReminderType getType() {
    	return type;
    }
    
	public String getName() {
		return reminderName;
	}
	
	public String getText() {
		return text;
	}
}
