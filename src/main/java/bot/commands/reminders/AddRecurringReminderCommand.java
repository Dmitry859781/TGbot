package bot.commands.reminders;

import bot.TelegramBot;
import bot.commands.Command;
import bot.reminder.ReminderService;
import bot.reminder.recurring.RecurringProperties;
import bot.reminder.recurring.ScheduleItem;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class AddRecurringReminderCommand implements Command {

    // Маппинг сокращений дней
    private static final Map<String, String> DAY_ALIAS_TO_ISO = Map.ofEntries(
        Map.entry("пн", "MON"),
        Map.entry("вт", "TUE"),
        Map.entry("ср", "WED"),
        Map.entry("чт", "THU"),
        Map.entry("пт", "FRI"),
        Map.entry("сб", "SAT"),
        Map.entry("вс", "SUN")
    );

    @Override
    public String getCommandName() {
        return "addRecurringReminder";
    }

    @Override
    public String getDescription() {
        return "Добавить повторяющееся напоминание";
    }

    @Override
    public String getUsage() {
        return "/addRecurringReminder";
    }

    private final ReminderService reminderService = new ReminderService();

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        bot.sendMessage(chatId, "Введите имя повторяющегося напоминания.");

        bot.setPendingInputHandler(chatId, (reminderName) -> {
            if (reminderName == null || reminderName.trim().isEmpty()) {
                bot.sendMessage(chatId, "Имя не может быть пустым. Попробуйте снова: /addRecurring");
                return;
            }
            String cleanName = reminderName.trim();

            bot.sendMessage(chatId, "Введите текст напоминания.");

            bot.setPendingInputHandler(chatId, (reminderText) -> {
                if (reminderText == null || reminderText.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Текст не может быть пустым. Операция отменена.");
                    return;
                }
                String cleanText = reminderText.trim();

                bot.sendMessage(chatId,
                    "Укажите дни недели через запятую.\n" +
                    "Пример: пн,вт,чт,вс\n" +
                    "Допустимые значения: пн, вт, ср, чт, пт, сб, вс"
                );

                bot.setPendingInputHandler(chatId, (daysInput) -> {
                    if (daysInput == null || daysInput.trim().isEmpty()) {
                        bot.sendMessage(chatId, "Не указаны дни недели. Операция отменена.");
                        return;
                    }

                    Set<String> isoDays = parseDays(daysInput.trim().toLowerCase());
                    if (isoDays.isEmpty()) {
                        bot.sendMessage(chatId,
                            "Не удалось распознать дни. Используйте: пн, вт, ср, чт, пт, сб, вс\n" +
                            "Пример: пн,вт,чт"
                        );
                        return;
                    }

                    bot.sendMessage(chatId,
                            "Введите времена напоминаний через запятую.\n" +
                            "Количество времён должно совпадать с количеством дней.\n" +
                            "Формат: H:mm (например, 9:00, 13:30, 15:05)"
                        );

                    bot.setPendingInputHandler(chatId, (timeInput) -> {
                        if (timeInput == null || timeInput.trim().isEmpty()) {
                            bot.sendMessage(chatId, "Время не указано. Операция отменена.");
                            return;
                        }

                        String[] timeParts = timeInput.trim().split(",");
                        if (timeParts.length != isoDays.size()) {
                            bot.sendMessage(chatId,
                                "Количество времён (" + timeParts.length + ") не совпадает с количеством дней (" + isoDays.size() + ").\n" +
                                "Операция отменена."
                            );
                            return;
                        }

                        List<String> normalizedTimes = new ArrayList<>();
                        for (String timeStr : timeParts) {
                            String cleanTime = timeStr.trim();
                            LocalTime time;
                            try {
                                time = LocalTime.parse(cleanTime, DateTimeFormatter.ofPattern("H:mm"));
                            } catch (DateTimeParseException e) {
                                bot.sendMessage(chatId, "Неверный формат времени: " + cleanTime + ".\nПримеры: 9:00, 13:30, 15:05\nОперация отменена.");
                                return;
                            }
                            normalizedTimes.add(time.format(DateTimeFormatter.ofPattern("H:mm")));
                        }

                        RecurringProperties props = new RecurringProperties();
                        props.schedule = new ArrayList<>();
                        List<String> sortedDays = new ArrayList<>(isoDays);

                        for (int i = 0; i < sortedDays.size(); i++) {
                            ScheduleItem item = new ScheduleItem();
                            item.day = sortedDays.get(i);
                            item.time = normalizedTimes.get(i);
                            props.schedule.add(item);
                        }
                        
                        try {
                            reminderService.addRecurringReminder(chatId, cleanName, cleanText, props);
                            String daysStr = String.join(", ", isoDays);
                            String timeStr = String.join(", ", normalizedTimes);
                            bot.sendMessage(chatId,
                                "Повторяющееся напоминание \"" + cleanName + "\" создано!\n" +
                                "Дни: " + daysStr + "\n" +
                                "Время: " + timeStr
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                            bot.sendMessage(chatId, "Ошибка при создании напоминания. Попробуйте позже.");
                        }
                    });
                });
            });
        });
    }

    // Парсит "пн,вт,чт" → Set{"MON", "TUE", "THU"}
    private Set<String> parseDays(String input) {
        Set<String> result = new LinkedHashSet<>(); // сохраняем порядок
        String[] parts = input.split("[,;\\s]+");

        for (String part : parts) {
            String alias = part.trim();
            if (alias.isEmpty()) continue;
            String iso = DAY_ALIAS_TO_ISO.get(alias);
            if (iso != null) {
                result.add(iso);
            }
        }
        return result;
    }
}