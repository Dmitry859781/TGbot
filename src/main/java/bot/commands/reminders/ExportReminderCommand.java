package bot.commands.reminders;

import bot.TelegramBot;
import bot.commands.Command;
import bot.reminder.Reminder;
import bot.reminder.ReminderService;
import bot.reminder.ReminderType;
import bot.reminder.once.OnceProperties;
import bot.reminder.recurring.RecurringProperties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.SQLException;
import java.util.List;

public class ExportReminderCommand implements Command {

    private final ReminderService reminderService = new ReminderService();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String getCommandName() {
        return "exportReminder";
    }

    @Override
    public String getDescription() {
        return "Экспортировать напоминание в JSON";
    }

    @Override
    public String getUsage() {
        return "/exportReminder";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        try {
            List<String> reminderNames = reminderService.getUserReminderNames(chatId);
            if (reminderNames.isEmpty()) {
                bot.sendMessage(chatId, "У вас нет напоминаний для экспорта.");
                return;
            }

            StringBuilder response = new StringBuilder("Какое напоминание экспортировать?\n");
            for (int i = 0; i < reminderNames.size(); i++) {
                response.append(i + 1).append(". ").append(reminderNames.get(i)).append("\n");
            }
            bot.sendMessage(chatId, response.toString().trim());

            bot.setPendingInputHandler(chatId, (input) -> {
                if (input == null || input.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Ввод не должен быть пустым. Попробуйте снова: /exportReminder");
                    return;
                }

                String reminderName = input.trim();
                Reminder reminder;
                try {
                    reminder = reminderService.getReminder(chatId, reminderName);
                } catch (SQLException e) {
                    bot.sendMessage(chatId, "Ошибка при загрузке напоминания.");
                    e.printStackTrace();
                    return;
                }

                if (reminder == null) {
                    bot.sendMessage(chatId, "Напоминание не найдено.");
                    return;
                }
                ReminderExportDto dto = new ReminderExportDto();
                dto.name = reminder.getReminderName();
                dto.text = reminder.getText();
                dto.type = reminder.getType();
                dto.properties = reminder.getPropertiesAs(Object.class); // JSON-свойства

                String json = gson.toJson(dto);
                bot.sendMessage(chatId, "JSON экспортированного напоминания:\n```json\n" + json + "\n```");
            });

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при экспорте напоминания.");
        }
    }

    //DTO
    private static class ReminderExportDto {
        public String name;
        public String text;
        public ReminderType type;
        public Object properties;
    }
}