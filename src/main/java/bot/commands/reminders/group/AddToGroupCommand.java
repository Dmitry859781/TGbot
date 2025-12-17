package bot.commands.reminders.group;

import bot.TelegramBot;
import bot.commands.Command;
import bot.reminder.Reminder;
import bot.reminder.ReminderService;
import bot.reminder.ReminderType;
import bot.reminder.once.OnceProperties;
import bot.reminder.recurring.RecurringProperties;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.SQLException;
import java.util.List;

public class AddToGroupCommand implements Command {

    @Override
    public String getCommandName() {
        return "addToGroup";
    }

    @Override
    public String getDescription() {
        return "Добавить одно или несколько напоминаний в группу";
    }

    @Override
    public String getUsage() {
        return "/addToGroup";
    }

    private final ReminderService reminderService = new ReminderService();

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        try {
            List<String> groups = reminderService.getUserGroups(chatId);
            if (!groups.isEmpty()) {
                StringBuilder response = new StringBuilder("Список ваших групп:\n");
                for (String group : groups) {
                    response.append("- ").append(group.isEmpty() ? "<без группы>" : group).append("\n");
                }
                bot.sendMessage(chatId, response.toString().trim());
            }
        } catch (SQLException e) {
        	bot.sendMessage(chatId, "Не удалось загрузить список групп");
            return;
        }

        bot.sendMessage(chatId, "Введите имя группы (например, \"Работа\").");

        bot.setPendingInputHandler(chatId, (groupNameInput) -> {
            if (groupNameInput == null || groupNameInput.trim().isEmpty()) {
                bot.sendMessage(chatId, "Имя группы не может быть пустым. Операция отменена.");
                return;
            }

            String groupName = groupNameInput.trim();

            bot.sendMessage(chatId, "Введите имена напоминаний через запятую (например: Напоминание1, Напоминание2).");

            bot.setPendingInputHandler(chatId, (namesInput) -> {
                if (namesInput == null || namesInput.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Список напоминаний не может быть пустым. Операция отменена.");
                    return;
                }

                String[] names = namesInput.trim().split(",");
                int count = 0;

                for (String name : names) {
                    String cleanName = name.trim();
                    try {
                        Reminder r = reminderService.getReminder(chatId, cleanName);
                        if (r != null) {
                            updateReminderGroup(r, groupName);
                            count++;
                        } else {
                            bot.sendMessage(chatId, "Напоминание \"" + cleanName + "\" не найдено.");
                        }
                    } catch (SQLException e) {
                        bot.sendMessage(chatId, "Ошибка при обновлении \"" + cleanName + "\".");
                    }
                }

                bot.sendMessage(chatId, count + " напоминаний добавлено в группу \"" + groupName + "\".");
            });
        });
    }

    private void updateReminderGroup(Reminder r, String newGroup) throws SQLException {
        if (r.getType() == ReminderType.ONCE) {
            OnceProperties props = r.getPropertiesAs(OnceProperties.class);
            props.group = newGroup;
            reminderService.updateReminderProperties(r.getUserId(), r.getReminderName(), props);
        } else if (r.getType() == ReminderType.RECURRING) {
            RecurringProperties props = r.getPropertiesAs(RecurringProperties.class);
            props.group = newGroup;
            reminderService.updateReminderProperties(r.getUserId(), r.getReminderName(), props);
        }
    }
}