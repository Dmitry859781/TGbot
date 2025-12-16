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

public class AddDelayedReminderCommand implements Command {

    @Override
    public String getCommandName() {
        return "addDelayedReminder";
    }

    @Override
    public String getDescription() {
        return "Назначить напоминание через указанный интервал";
    }

    @Override
    public String getUsage() {
        return "/addDelayedReminder";
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

            bot.sendMessage(chatId, "Время через которое сработает напоминание в формате:\n" +
                "HH:mm\n" +
                "Например: 1:30"
            );

            bot.setPendingInputHandler(chatId, (TimeInput) -> {
                if (TimeInput == null || TimeInput.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Время не указано. Операция отменена.");
                    return;
                }

                String[] parts = TimeInput.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                if (hours < 0 || minutes > 59 || minutes < 0) {
                	bot.sendMessage(chatId, "Неверные значения времени. Часы >= 0, минуты от 0 до 59.\nОперация отменена.");
                	return;
                }
                LocalDateTime now = LocalDateTime.now(userZone);
                LocalDateTime remindAt = now.plusHours(hours).plusMinutes(minutes);

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