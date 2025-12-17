package bot.commands.reminders;

import bot.TelegramBot;
import bot.commands.Command;
import bot.reminder.Reminder;
import bot.reminder.ReminderService;
import bot.reminder.ReminderType;
import bot.reminder.recurring.RecurringProperties;
import bot.reminder.recurring.ScheduleItem;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class EditRecurringReminderCommand implements Command {

    // Маппинг для парсинга дней (как в AddRecurringReminderCommand)
    private static final Map<String, String> DAY_ALIAS_TO_ISO = Map.ofEntries(
        Map.entry("пн", "MON"),
        Map.entry("вт", "TUE"),
        Map.entry("ср", "WED"),
        Map.entry("чт", "THU"),
        Map.entry("пт", "FRI"),
        Map.entry("сб", "SAT"),
        Map.entry("вс", "SUN"),
        Map.entry("mon", "MON"),
        Map.entry("tue", "TUE"),
        Map.entry("wed", "WED"),
        Map.entry("thu", "THU"),
        Map.entry("fri", "FRI"),
        Map.entry("sat", "SAT"),
        Map.entry("sun", "SUN")
    );

    @Override
    public String getCommandName() {
        return "editRecurringReminder";
    }

    @Override
    public String getDescription() {
        return "Редактировать повторяющееся напоминание";
    }

    @Override
    public String getUsage() {
        return "/editRecurringReminder";
    }

    private final ReminderService reminderService = new ReminderService();

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        try {
            List<String> reminderNames = reminderService.getUserReminderNames(chatId);
            reminderNames.removeIf(name -> {
                try {
                    Reminder r = reminderService.getReminder(chatId, name);
                    return r == null || !ReminderType.RECURRING.equals(r.getType());
                } catch (SQLException e) {
                    e.printStackTrace();
                    return true;
                }
            });

            if (reminderNames.isEmpty()) {
                bot.sendMessage(chatId, "У вас нет повторяющихся напоминаний. Создайте с помощью /addRecurring");
                return;
            }

            // Показываем список с текущим расписанием
            StringBuilder response = new StringBuilder("Какое повторяющееся напоминание отредактировать?\n");
            for (int i = 0; i < reminderNames.size(); i++) {
                Reminder r = reminderService.getReminder(chatId, reminderNames.get(i));
                RecurringProperties props = r.getPropertiesAs(RecurringProperties.class);
                String scheduleStr = formatSchedule(props);
                response.append(i + 1).append(". ").append(reminderNames.get(i))
                        .append(" (").append(scheduleStr).append(")\n");
            }
            bot.sendMessage(chatId, response.toString().trim());

            bot.setPendingInputHandler(chatId, (input) -> {
                if (input == null || input.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Ввод не должен быть пустым. Попробуйте снова: /editRecurring");
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

                if (existing == null || !ReminderType.RECURRING.equals(existing.getType())) {
                    bot.sendMessage(chatId, "Напоминание не найдено или не является повторяющимся.");
                    return;
                }

                // Показываем текущие данные
                RecurringProperties oldProps = existing.getPropertiesAs(RecurringProperties.class);
                String oldSchedule = formatSchedule(oldProps);

                bot.sendMessage(chatId,
                    "Текущие данные:\n" +
                    "Имя: " + reminderName + "\n" +
                    "Текст: " + existing.getText() + "\n" +
                    "Расписание: " + oldSchedule + "\n\n" +
                    "Введите новые дни недели через запятую (например: пн,вт,чт,вс)\n" +
                    "Или отправьте \"-\", чтобы оставить без изменений."
                );

                bot.setPendingInputHandler(chatId, (newDaysInput) -> {
                    if (newDaysInput == null) {
                        bot.sendMessage(chatId, "Операция отменена.");
                        return;
                    }

                    final Set<String> newDays;
                    if ("-".equals(newDaysInput.trim())) {
                        // Оставляем старые дни
                        newDays = extractIsoDays(oldProps);
                    } else {
                        newDays = parseDays(newDaysInput.trim().toLowerCase());
                        if (newDays.isEmpty()) {
                            bot.sendMessage(chatId,
                                "Не удалось распознать дни. Используйте: пн, вт, ср, чт, пт, сб, вс");
                            return;
                        }
                    }

                    bot.sendMessage(chatId,
                        "Текущее время: " + extractTimeList(oldProps) + "\n" +
                        "Введите новое время в формате HH:mm (например: 09:00)\n" +
                        "Или отправьте \"-\", чтобы оставить без изменений."
                    );

                    bot.setPendingInputHandler(chatId, (newTimeInput) -> {
                        if (newTimeInput == null) {
                            bot.sendMessage(chatId, "Операция отменена.");
                            return;
                        }

                        List<String> newTimes;
                        if ("-".equals(newTimeInput.trim())) {
                            newTimes = extractTimeList(oldProps);
                        } else {
                        	String[] timeParts = newTimeInput.trim().split(",");
                            if (timeParts.length != newDays.size()) {
                                bot.sendMessage(chatId,
                                    "Количество времён (" + timeParts.length + ") не совпадает с количеством дней (" + newDays.size() + ").\n" +
                                    "Операция отменена."
                                );
                                return;
                            }

                            List<String> parsedTimes = new ArrayList<>();
                            for (String timeStr : timeParts) {
                                String cleanTime = timeStr.trim();
                                LocalTime time;
                                try {
                                    time = LocalTime.parse(cleanTime, DateTimeFormatter.ofPattern("H:mm"));
                                } catch (DateTimeParseException e) {
                                    bot.sendMessage(chatId, "Неверный формат времени: " + cleanTime + ".\nПримеры: 9:00, 13:30, 15:05\nОперация отменена.");
                                    return;
                                }
                                parsedTimes.add(time.format(DateTimeFormatter.ofPattern("H:mm")));
                            }
                            newTimes = parsedTimes;
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

                            // Собираем новое расписание
                            RecurringProperties newProps = new RecurringProperties();
                            newProps.schedule = new ArrayList<>();
                            List<String> sortedDays = new ArrayList<>(newDays); // чтобы гарантировать порядок

                            for (int i = 0; i < sortedDays.size(); i++) {
                                ScheduleItem item = new ScheduleItem();
                                item.day = sortedDays.get(i);
                                item.time = newTimes.get(i);
                                newProps.schedule.add(item);
                            }

                            try {
                                reminderService.removeReminder(chatId, reminderName);
                                reminderService.addRecurringReminder(chatId, reminderName, newText, newProps);
                                StringBuilder scheduleStr = new StringBuilder();
                                for (int i = 0; i < sortedDays.size(); i++) {
                                    if (i > 0) scheduleStr.append(", ");
                                    scheduleStr.append(formatDay(sortedDays.get(i))).append(" в ").append(newTimes.get(i));
                                }
                                bot.sendMessage(chatId,
                                    "Напоминание \"" + reminderName + "\" обновлено!\n" +
                                    "Расписание: " + scheduleStr
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

    // Вспомогательные методы

    private String formatSchedule(RecurringProperties props) {
        if (props.schedule == null || props.schedule.isEmpty()) {
            return "(не задано)";
        }
        return props.schedule.stream()
            .map(item -> formatDay(item.day) + " в " + item.time)
            .collect(Collectors.joining(", "));
    }

    private Set<String> extractIsoDays(RecurringProperties props) {
        Set<String> days = new LinkedHashSet<>();
        if (props.schedule != null) {
            for (ScheduleItem item : props.schedule) {
                if (item.day != null) days.add(item.day);
            }
        }
        return days;
    }

    private List<String> extractTimeList(RecurringProperties props) {
        if (props.schedule == null || props.schedule.isEmpty()) {
            return Collections.singletonList("09:00");
        }
        return props.schedule.stream()
            .map(item -> item.time)
            .collect(Collectors.toList());
    }

    private Set<String> parseDays(String input) {
        Set<String> result = new LinkedHashSet<>();
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

    private String formatDay(String isoDay) {
        return switch (isoDay != null ? isoDay : "") {
            case "MON" -> "Пн";
            case "TUE" -> "Вт";
            case "WED" -> "Ср";
            case "THU" -> "Чт";
            case "FRI" -> "Пт";
            case "SAT" -> "Сб";
            case "SUN" -> "Вс";
            default -> "?";
        };
    }
}