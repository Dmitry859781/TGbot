package bot.commands.reminders;

import bot.TelegramBot;
import bot.commands.Command;
import bot.reminder.ReminderService;
import bot.reminder.ReminderType;
import bot.reminder.once.OnceProperties;
import bot.reminder.recurring.RecurringProperties;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ImportReminderCommand implements Command {

    private final ReminderService reminderService = new ReminderService();
    private final Gson gson = new Gson();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getCommandName() {
        return "importReminder";
    }

    @Override
    public String getDescription() {
        return "Импортировать напоминание из JSON";
    }

    @Override
    public String getUsage() {
        return "/importReminder";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        bot.sendMessage(chatId, "Введите JSON-строку напоминания для импорта.");

        bot.setPendingInputHandler(chatId, (jsonInput) -> {
            if (jsonInput == null || jsonInput.trim().isEmpty()) {
                bot.sendMessage(chatId, "JSON не может быть пустым. Операция отменена.");
                return;
            }

            try {
                // Десериализуем DTO
                ReminderExportDto dto = gson.fromJson(jsonInput.trim(), ReminderExportDto.class);
                if (dto == null || dto.name == null || dto.text == null || dto.type == null || dto.properties == null) {
                    bot.sendMessage(chatId, "Некорректный формат JSON. Операция отменена.");
                    return;
                }

                // Импортируем напоминание
                switch (dto.type) {
                    case ONCE -> {
                        OnceProperties props = gson.fromJson(gson.toJson(dto.properties), OnceProperties.class);
                        if (props.deleteAfterSend == null) props.deleteAfterSend = true;
                        LocalDateTime utcTime = LocalDateTime.parse(props.remind_at, TIME_FORMATTER);
                        reminderService.addOnceReminder(chatId, dto.name, dto.text, utcTime, ZoneId.systemDefault(), props.deleteAfterSend);
                    }
                    case RECURRING -> {
                        RecurringProperties props = gson.fromJson(gson.toJson(dto.properties), RecurringProperties.class);
                        reminderService.addRecurringReminder(chatId, dto.name, dto.text, props);
                    }
                    default -> {
                        bot.sendMessage(chatId, "Неизвестный тип напоминания. Операция отменена.");
                        return;
                    }
                }

                bot.sendMessage(chatId, "Напоминание \"" + dto.name + "\" импортировано!");

            } catch (JsonSyntaxException e) {
                bot.sendMessage(chatId, "Неверный формат JSON. Операция отменена.");
            } catch (Exception e) {
                e.printStackTrace();
                bot.sendMessage(chatId, "Ошибка при импорте напоминания.");
            }
        });
    }

    //DTO
    private static class ReminderExportDto {
        public String name;
        public String text;
        public ReminderType type;
        public Object properties;
    }
}