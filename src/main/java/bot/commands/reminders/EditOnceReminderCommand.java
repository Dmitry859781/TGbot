package bot.commands.reminders;

import bot.TelegramBot;
import bot.commands.Command;
import bot.reminder.Reminder;
import bot.reminder.ReminderService;
import bot.reminder.ReminderType;
import bot.reminder.once.OnceProperties;
import bot.timezone.UserTimezoneService;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class EditOnceReminderCommand implements Command {

    @Override
    public String getCommandName() {
        return "editOnceReminder";
    }

    @Override
    public String getDescription() {
        return "Редактировать однократное напоминание";
    }

    @Override
    public String getUsage() {
        return "/editOnceReminder";
    }

    private final ReminderService reminderService = new ReminderService();
    private final UserTimezoneService timezoneService = new UserTimezoneService();

    @Override
    public void execute(TelegramBot bot, Message message, String[] args){
        Long chatId = message.getChatId();
        ZoneId userZone = getUserZone(bot, chatId);
        
        try {
            List<String> reminderNames = reminderService.getUserReminderNames(chatId);
            reminderNames.removeIf(name -> {
            	Reminder r = null;
				try {
					r = reminderService.getReminder(chatId, name);
				} catch (SQLException e) {
					e.printStackTrace();
				}
                return r == null || !ReminderType.ONCE.equals(r.getType());
            });

            if (reminderNames.isEmpty()) {
                bot.sendMessage(chatId, "У вас нет однократных напоминаний. Создайте с помощью /addReminder");
                return;
            }

            StringBuilder response = new StringBuilder("Какое однократное напоминание отредактировать?\n");
            for (int i = 0; i < reminderNames.size(); i++) {
                Reminder r = reminderService.getReminder(chatId, reminderNames.get(i));
                OnceProperties props = r.getPropertiesAs(OnceProperties.class);
                LocalDateTime time = LocalDateTime.parse(props.remind_at,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String timeStr = time.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                response.append(i + 1).append(". ").append(reminderNames.get(i))
                        .append(" (").append(timeStr).append(")\n");
            }
            bot.sendMessage(chatId, response.toString().trim());

            bot.setPendingInputHandler(chatId, (input) -> {
                if (input == null || input.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Ввод не должен быть пустым. Попробуйте снова: /editOnceReminder");
                    return;
                }

                String reminderName = input.trim();
                final Reminder existing;
				try {
					existing = reminderService.getReminder(chatId, reminderName);
				} catch (SQLException e) {
					bot.sendMessage(chatId, "Ошибка при загрузке напоминания.");
				    e.printStackTrace();
				    return;
				}
                if (existing == null || !ReminderType.ONCE.equals(existing.getType())) {
                    bot.sendMessage(chatId, "Напоминание не найдено или не является однократным.");
                    return;
                }

                // Показываем текущие данные
                OnceProperties oldProps = existing.getPropertiesAs(OnceProperties.class);
                LocalDateTime oldTime = LocalDateTime.parse(oldProps.remind_at,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String oldTimeStr = oldTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

                bot.sendMessage(chatId,
                    "Текущие данные:\n" +
                    "Имя: " + reminderName + "\n" +
                    "Текст: " + existing.getText() + "\n" +
                    "Время: " + oldTimeStr + "\n\n" +
                    "Введите новое время в формате dd.MM.yyyy HH:mm\n" +
                    "Или отправьте \"-\", чтобы оставить без изменений."
                );

                bot.setPendingInputHandler(chatId, (newTimeInput) -> {
                    if (newTimeInput == null) {
                        bot.sendMessage(chatId, "Операция отменена.");
                        return;
                    }

                    final LocalDateTime newTime;
                    String trimmedInput = newTimeInput.trim();
                    if ("-".equals(trimmedInput)) {
                        newTime = null;
                    } else {
                        try {
                            newTime = LocalDateTime.parse(trimmedInput,
                                DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm", Locale.ENGLISH));
                            if (newTime.isBefore(LocalDateTime.now())) {
                                bot.sendMessage(chatId, "Нельзя установить напоминание в прошлое. Операция отменена.");
                                return;
                            }
                        } catch (DateTimeParseException e) {
                            bot.sendMessage(chatId, "Неверный формат времени. Используйте: dd.MM.yyyy HH:mm");
                            return;
                        }
                    }

                    bot.sendMessage(chatId,
                        "Текущий текст: " + existing.getText() + "\n" +
                        "Введите новый текст\n" +
                        "Или отправьте \"-\", чтобы оставить без изменений."
                    );

                    bot.setPendingInputHandler(chatId, (newTextInput) -> {
                        if (newTextInput == null) {
                            bot.sendMessage(chatId, "Операция отменена.");
                            return;
                        }

                        String newText = "-".equals(newTextInput.trim()) ? existing.getText() : newTextInput.trim();
                        if (newText.isEmpty()) {
                            bot.sendMessage(chatId, "Текст не может быть пустым. Операция отменена.");
                            return;
                        }

                        try {
                            reminderService.removeReminder(chatId, reminderName);
                            LocalDateTime finalTime = (newTime != null) ? newTime : oldTime;
                            reminderService.addOnceReminder(chatId, reminderName, newText, finalTime, userZone);
                            bot.sendMessage(chatId, "Напоминание \"" + reminderName + "\" обновлено!");
                        } catch (Exception e) {
                            e.printStackTrace();
                            bot.sendMessage(chatId, "Ошибка при обновлении напоминания.");
                        }
                    });
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Не удалось загрузить список напоминаний.");
        }
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