package bot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import bot.commands.Command;
import bot.commands.InputHandler;
import bot.reminder.Reminder;
import bot.reminder.ReminderService;
import bot.reminder.ReminderType;
import bot.commands.CommandRegistry;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

public class TelegramBot extends TelegramLongPollingBot {

    private final String botToken = System.getenv("TELEGRAM_BOT_TOKEN");
    
    public TelegramBot() {
    	
    }

    @Override
    public String getBotUsername() {
        return "Unterrichtung_bot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
    
    // Обработчик ожиданий
    private final Map<Long, InputHandler> pendingInputHandlers = new HashMap<>();
    
    public void setPendingInputHandler(Long chatId, InputHandler handler) {
        pendingInputHandlers.put(chatId, handler);
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        if (!message.hasText()) {
            //Реакция на картинки/стикеры (не текстовые сообщения)
            return;
        }
        String text = message.getText().trim();
        
        // Обработка ожидающих команд
        // Обработчик выполняет лишь единичное удержание
        // В следующей реализации удалять обработчики в функциях
        // Здесь реализовать команду отмены /cancel
        if (pendingInputHandlers.containsKey(chatId)) {
            InputHandler handler = pendingInputHandlers.remove(chatId); // Удаляем сразу!
            handler.handle(text);
            return; // Не обрабатываем как команду!
        }
        
        if (text.startsWith("/")) {
            // Парсим команду и аргументы
            String[] parts = text.split("\\s+", 2);
            String cmdName = parts[0].substring(1); // убираем "/"
            String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

            Command command = CommandRegistry.getCommand(cmdName);
            if (command != null) {
                command.execute(this, message, args);
            } else {
                sendMessage(message.getChatId(), "Неизвестная команда. Введите /help для списка.");
            }
        }
    }
    
    // Короче, Напоминания, которые надо выслать и бла бла бла
    private final ReminderService reminderService = new ReminderService();
    private ScheduledExecutorService scheduler;

    public void startReminderChecker() {
    	System.out.println("Запущена проверка напоминаний");
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkAndSendReminders, 0, 1, TimeUnit.MINUTES);
    }

    private void checkAndSendReminders() {
    	System.out.println("Запущен checkAndSend");
        try {
            List<Reminder> due = reminderService.getDueReminders();
            System.out.println("Найдено напоминаний для отправки: " + due.size());
            for (Reminder reminder : due) {
                // Отправляем уведомление    вкусовщина: (Возможно стоит просто текст напоминания высылать)
                String message = "Напоминание: " + reminder.getName() + "\n" + reminder.getText();
                execute(SendMessage.builder()
                    .chatId(reminder.getUserId().toString())
                    .text(message)
                    .build());

                // Если ONCE — удаляем после отправки
                if (ReminderType.ONCE.equals(reminder.getType())) {
                    reminderService.removeReminder(reminder.getUserId(), reminder.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Остановка при завершении
    public void stopReminderChecker() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    public void sendMessage(Long chatId, String text) {
        org.telegram.telegrambots.meta.api.methods.send.SendMessage msg =
            new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        try {
            execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}