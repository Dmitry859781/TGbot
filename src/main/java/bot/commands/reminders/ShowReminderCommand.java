package bot.commands.reminders;

import bot.TelegramBot;
import bot.commands.Command;
import bot.Reminder.Reminder;
import bot.Reminder.ReminderService;
import bot.Reminder.ReminderType;
import bot.Reminder.once.OnceProperties;
import bot.Reminder.recurring.RecurringProperties;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ShowReminderCommand implements Command {

    @Override
    public String getCommandName() {
        return "showReminder";
    }

    @Override
    public String getDescription() {
        return "Показать детали напоминания";
    }

    @Override
    public String getUsage() {
        return "/showReminder";
    }

    private final ReminderService reminderService = new ReminderService();

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        try {
            List<String> reminderNames = reminderService.getUserReminderNames(chatId);

            if (reminderNames.isEmpty()) {
                bot.sendMessage(chatId, "У вас нет напоминаний. Добавьте первое с помощью /addOnceReminder или /addRecurringReminder");
                return;
            }

            StringBuilder response = new StringBuilder("Какое напоминание показать?\n");
            for (int i = 0; i < reminderNames.size(); i++) {
                response.append(i + 1).append(". ").append(reminderNames.get(i)).append("\n");
            }
            bot.sendMessage(chatId, response.toString().trim());

            bot.setPendingInputHandler(chatId, (input) -> {
                if (input == null || input.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Ввод не должен быть пустым. Попробуйте снова: /showReminder");
                    return;
                }

                String reminderName  = input.trim();

                try {
                    Reminder reminder = reminderService.getReminder(chatId, reminderName);
                    if (reminder == null) {
                        bot.sendMessage(chatId, "Напоминание \"" + reminderName + "\" не найдено.");
                        return;
                    }

                    String details = formatReminderDetails(reminder);
                    bot.sendMessage(chatId, details);

                } catch (Exception e) {
                    e.printStackTrace();
                    bot.sendMessage(chatId, "Ошибка при получении напоминания. Попробуйте позже.");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Не удалось загрузить список напоминаний.");
        }
    }

    // Форматирует детали напоминания для отображения
    private String formatReminderDetails(Reminder r) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(r.getName()).append("\"\n");
        sb.append("Текст: ").append(r.getText()).append("\n");
        sb.append("Тип: ").append(formatType(r.getType())).append("\n");

        try {
            if (ReminderType.ONCE.equals(r.getType())) {
                OnceProperties props = r.getPropertiesAs(OnceProperties.class);
                // Парсим UTC-время из строки
                LocalDateTime utcTime = LocalDateTime.parse(props.remind_at,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                // Пока выводим как есть (в UTC). В будущем можно конвертировать в локальный часовой пояс
                sb.append("Дата и время: ").append(utcTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append(" (UTC)");
            } else if (ReminderType.RECURRING.equals(r.getType())) {
                RecurringProperties props = r.getPropertiesAs(RecurringProperties.class);
                if (props.schedule != null && !props.schedule.isEmpty()) {
                    String scheduleStr = props.schedule.stream()
                        .map(item -> formatDay(item.day) + " в " + item.time)
                        .collect(Collectors.joining(", "));
                    sb.append("Расписание: ").append(scheduleStr);
                } else {
                    sb.append("Расписание: (не задано)");
                }
            }
        } catch (Exception e) {
            sb.append("Ошибка при загрузке свойств");
        }

        return sb.toString();
    }

    private String formatType(ReminderType type) {
        return switch (type) {
            case ONCE -> "Однократное";
            case RECURRING -> "Повторяющееся";
        };
    }

    // Преобразует "MON" → "Пн" и т.д.
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