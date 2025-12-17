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

public class DisableGroupCommand implements Command {

    private final ReminderService reminderService = new ReminderService();

    @Override
    public String getCommandName() {
        return "disableGroup";
    }

    @Override
    public String getDescription() {
        return "Отключить все напоминания в группе";
    }

    @Override
    public String getUsage() {
        return "/disableGroup <groupName>";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        if (args.length == 0) {
            bot.sendMessage(chatId, "Укажите имя группы. Пример: /disableGroup Работа");
            return;
        }

        String groupName = String.join(" ", args);

        try {
            List<String> reminderNames = reminderService.getUserReminderNames(chatId);
            int count = 0;

            for (String name : reminderNames) {
                Reminder r = reminderService.getReminder(chatId, name);
                if (r != null && matchesGroup(r, groupName)) {
                    toggleReminderEnabled(r, false); // отключить
                    count++;
                }
            }

            bot.sendMessage(chatId, count + " напоминаний в группе \"" + groupName + "\" отключено.");
        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при отключении группы.");
        }
    }

    private boolean matchesGroup(Reminder r, String groupName) {
        if (r.getType() == ReminderType.ONCE) {
            OnceProperties props = r.getPropertiesAs(OnceProperties.class);
            return groupName.equals(props.group);
        } else if (r.getType() == ReminderType.RECURRING) {
            RecurringProperties props = r.getPropertiesAs(RecurringProperties.class);
            return groupName.equals(props.group);
        }
        return false;
    }

    private void toggleReminderEnabled(Reminder r, boolean enabled) throws SQLException {
        if (r.getType() == ReminderType.ONCE) {
            OnceProperties props = r.getPropertiesAs(OnceProperties.class);
            props.enabled = enabled;
            reminderService.updateReminderProperties(r.getUserId(), r.getReminderName(), props);
        } else if (r.getType() == ReminderType.RECURRING) {
            RecurringProperties props = r.getPropertiesAs(RecurringProperties.class);
            props.enabled = enabled;
            reminderService.updateReminderProperties(r.getUserId(), r.getReminderName(), props);
        }
    }
}