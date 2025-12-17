package bot.commands.reminders.group;

import bot.TelegramBot;
import bot.commands.Command;
import bot.reminder.Reminder;
import bot.reminder.ReminderService;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

public class ShowGroupCommand implements Command {

    private final ReminderService reminderService = new ReminderService();

    @Override
    public String getCommandName() {
        return "showGroup";
    }

    @Override
    public String getDescription() {
        return "Показать все напоминания в группе";
    }

    @Override
    public String getUsage() {
        return "/showGroup <groupName>";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        if (args.length == 0) {
            bot.sendMessage(chatId, "Укажите имя группы. Пример: /showGroup Работа");
            return;
        }

        String groupName = String.join(" ", args);

        try {
            List<String> reminderNames = reminderService.getUserReminderNames(chatId);
            StringBuilder response = new StringBuilder("Напоминания в группе \"" + (groupName.isEmpty() ? "<без группы>" : groupName) + "\":\n");

            boolean found = false;
            for (String name : reminderNames) {
                Reminder r = reminderService.getReminder(chatId, name);
                if (r != null && r.getGroup().equals(groupName)) {
                    String status = r.isEnabled() ? "Активна" : "Не активна";
                    response.append(status).append(" ").append(name).append("\n");
                    found = true;
                }
            }

            if (!found) {
                response.append("(нет напоминаний)");
            }

            bot.sendMessage(chatId, response.toString().trim());

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при загрузке напоминаний.");
        }
    }
}