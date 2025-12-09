package bot.commands.reminders;

import bot.TelegramBot;
import bot.commands.Command;
import bot.reminder.ReminderService;
import bot.timezone.UserTimezoneService;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class AddOnceReminderCommand implements Command {

    @Override
    public String getCommandName() {
        return "addOnceReminder";
    }

    @Override
    public String getDescription() {
        return "Добавить однократное напоминание";
    }

    @Override
    public String getUsage() {
        return "/addOnceReminder";
    }

    private final ReminderService reminderService = new ReminderService();
    private final UserTimezoneService timezoneService = new UserTimezoneService();


    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();
        ZoneId userZone = getUserZone(bot, chatId);
		
        bot.sendMessage(chatId, "Введите имя напоминания.");

        bot.setPendingInputHandler(chatId, (reminderName) -> {
            if (reminderName == null || reminderName.trim().isEmpty()) {
                bot.sendMessage(chatId, "Имя напоминания не может быть пустым. Попробуйте снова: /addReminder");
                return;
            }

            String cleanName = reminderName.trim();

            bot.sendMessage(chatId, "Введите дату и время напоминания в формате:\n" +
                "dd.MM.yyyy HH:mm\n" +
                "Например: 26.11.2025 15:30"
            );

            bot.setPendingInputHandler(chatId, (dateTimeInput) -> {
                if (dateTimeInput == null || dateTimeInput.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Время не указано. Операция отменена.");
                    return;
                }

                LocalDateTime remindAt;
                try {
                    // Парсим дату в формате dd.MM.yyyy HH:mm
                    remindAt = LocalDateTime.parse(
                        dateTimeInput.trim(),
                        java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ENGLISH)
                    );
                } catch (DateTimeParseException e) {
                    bot.sendMessage(chatId, "Неверный формат даты. Используйте: dd.MM.yyyy HH:mm\nОперация отменена.");
                    return;
                }

                // Проверка: напоминание не в прошлом
                // Возможно добавить "Уже слишком поздно)"
                if (remindAt.isBefore(LocalDateTime.now())) {
                    bot.sendMessage(chatId, "Нельзя создать напоминание в прошлом. Операция отменена.");
                    return;
                }

                bot.sendMessage(chatId, "Введите текст напоминания.");

                bot.setPendingInputHandler(chatId, (reminderText) -> {
                    if (reminderText == null || reminderText.trim().isEmpty()) {
                        bot.sendMessage(chatId, "Текст напоминания не может быть пустым. Операция отменена.");
                        return;
                    }

                    try {
						// Сохраняем как ONCE-напоминание (в UTC!)
                        reminderService.addOnceReminder(chatId, cleanName, reminderText.trim(), remindAt, userZone);
                        bot.sendMessage(chatId,
                            "Напоминание \"" + cleanName + "\" установлено на " +
                            remindAt.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + "!"
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        bot.sendMessage(chatId, "Ошибка при создании напоминания. Попробуйте позже.");
                    }
                });
            });
        });
    }
    //Для использования в лямбдах
    private ZoneId getUserZone(TelegramBot bot, Long chatId) {
        try {
            Integer offsetHours = timezoneService.getTimezone(chatId);
            if (offsetHours == null) {
                bot.sendMessage(chatId, "Часовой пояс не указан. Будет использован системный "+ ZoneId.systemDefault() +". \n" + 
                                "Используйте /setOrEditTimezone для установки или изменения часового пояса");
                return ZoneId.systemDefault();
            } else {
                bot.sendMessage(chatId, "Ваш часовой пояс: " + offsetHours);
                return ZoneOffset.ofHours(offsetHours);
            }
        } catch (SQLException e) {
            bot.sendMessage(chatId, "Не удалось получить часовой пояс. Будет использован системный " + ZoneId.systemDefault());
            return ZoneId.systemDefault();
        }
    }
}