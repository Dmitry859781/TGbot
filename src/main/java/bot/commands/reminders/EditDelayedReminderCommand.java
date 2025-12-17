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

public class EditDelayedReminderCommand implements Command {

    @Override
    public String getCommandName() {
        return "editDelayedReminder";
    }

    @Override
    public String getDescription() {
        return "Редактировать однократное напоминание (включая с отсрочкой)";
    }

    @Override
    public String getUsage() {
        return "/editDelayedReminder";
    }

    private final ReminderService reminderService = new ReminderService();
    private final UserTimezoneService timezoneService = new UserTimezoneService();

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();
        ZoneId userZone = getUserZone(bot, chatId);

        try {
            List<String> reminderNames = reminderService.getUserReminderNames(chatId);
            reminderNames.removeIf(name -> {
                try {
                    Reminder r = reminderService.getReminder(chatId, name);
                    return r == null || !ReminderType.ONCE.equals(r.getType());
                } catch (SQLException e) {
                    e.printStackTrace();
                    return true;
                }
            });

            if (reminderNames.isEmpty()) {
                bot.sendMessage(chatId, "У вас нет однократных напоминаний. Создайте с помощью /addOnceReminder или /addDelayedReminder");
                return;
            }

            // Показываем список
            StringBuilder response = new StringBuilder("Какое напоминание отредактировать?\n");
            for (int i = 0; i < reminderNames.size(); i++) {
                Reminder r = reminderService.getReminder(chatId, reminderNames.get(i));
                OnceProperties props = r.getPropertiesAs(OnceProperties.class);
                String timeStr = LocalDateTime.parse(props.remind_at,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(userZone)
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                response.append(i + 1).append(". ").append(reminderNames.get(i))
                        .append(" (").append(timeStr).append(")\n");
            }
            bot.sendMessage(chatId, response.toString().trim());

            bot.setPendingInputHandler(chatId, (input) -> {
                if (input == null || input.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Ввод не должен быть пустым. Попробуйте снова: /editDelayedReminder");
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
                LocalDateTime oldTimeUtc = LocalDateTime.parse(oldProps.remind_at,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                LocalDateTime oldTimeLocal = oldTimeUtc.atZone(ZoneOffset.UTC)
                                                      .withZoneSameInstant(userZone)
                                                      .toLocalDateTime();
                String oldTimeStr = oldTimeLocal.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

                bot.sendMessage(chatId,
                    "Текущие данные:\n" +
                    "Имя: " + reminderName + "\n" +
                    "Текст: " + existing.getText() + "\n" +
                    "Время: " + oldTimeStr + "\n\n" +
                    "Введите новое имя (или \"-\", чтобы оставить без изменений):"
                );

                bot.setPendingInputHandler(chatId, (newNameInput) -> {
                    if (newNameInput == null) {
                        bot.sendMessage(chatId, "Операция отменена.");
                        return;
                    }

                    String newReminderName = "-".equals(newNameInput.trim()) ? reminderName : newNameInput.trim();
                    if (newReminderName.isEmpty()) {
                        bot.sendMessage(chatId, "Имя не может быть пустым. Операция отменена.");
                        return;
                    }

                    bot.sendMessage(chatId,
                        "Текущее время: " + oldTimeStr + "\n" +
                        "Введите новое время через которое сработает напоминание (в формате H:mm, например 1:30)\n" +
                        "Или отправьте \"-\", чтобы оставить без изменений."
                    );

                    bot.setPendingInputHandler(chatId, (newTimeInput) -> {
                        if (newTimeInput == null) {
                            bot.sendMessage(chatId, "Операция отменена.");
                            return;
                        }

                        LocalDateTime newTime;
                        if ("-".equals(newTimeInput.trim())) {
                            newTime = oldTimeLocal;
                        } else {
                            String[] parts = newTimeInput.trim().split(":");
                            int hours, minutes;
                            try {
                                hours = Integer.parseInt(parts[0]);
                                minutes = Integer.parseInt(parts[1]);
                            } catch (NumberFormatException e) {
                                bot.sendMessage(chatId, "Неверный формат времени. Используйте: H:mm (например, 1:30)\nОперация отменена.");
                                return;
                            }
                            if (hours < 0 || minutes < 0 || minutes > 59) {
                                bot.sendMessage(chatId, "Неверные значения времени. Часы >= 0, минуты от 0 до 59.\nОперация отменена.");
                                return;
                            }

                            LocalDateTime now = LocalDateTime.now(userZone);
                            newTime = now.plusHours(hours).plusMinutes(minutes);
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
                                reminderService.addOnceReminder(chatId, newReminderName, newText, newTime, userZone, true);
                                bot.sendMessage(chatId,
                                    "Напоминание \"" + newReminderName + "\" обновлено!\n" +
                                    "Новое время: " + newTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                                bot.sendMessage(chatId, "Ошибка при обновлении напоминания.");
                            }
                        });
                    });
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Не удалось загрузить список напоминаний.");
        }
    }

    // Для использования в лямбдах
    private ZoneId getUserZone(TelegramBot bot, Long chatId) {
        try {
            Integer offsetHours = timezoneService.getTimezone(chatId);
            if (offsetHours == null) {
                bot.sendMessage(chatId, "Часовой пояс не указан. Будет использован системный " + ZoneId.systemDefault() + ".\n" +
                                "Используйте /setOrEditTimezone для установки или изменения часового пояса");
                return ZoneId.systemDefault();
            } else {
                return ZoneOffset.ofHours(offsetHours);
            }
        } catch (SQLException e) {
            return ZoneId.systemDefault();
        }
    }
}