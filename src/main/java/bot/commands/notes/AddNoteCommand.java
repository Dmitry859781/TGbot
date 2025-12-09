package bot.commands.notes;

import bot.TelegramBot;
import bot.commands.Command;
import bot.note.NoteService;

import org.telegram.telegrambots.meta.api.objects.Message;

public class AddNoteCommand implements Command{

	@Override
    public String getCommandName() {
        return "addNote";
    }

    @Override
    public String getDescription() {
        return "Добавить заметку";
    }

    @Override
    public String getUsage() {
        return "/addNote";
    }
    
    private final NoteService noteService = new NoteService();

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        // Шаг 1: Запрашиваем имя заметки
        bot.sendMessage(chatId, "Введите имя добавляемой заметки.");

        // Регистрируем обработчик для получения имени заметки
        bot.setPendingInputHandler(chatId, (noteName) -> {
            if (noteName == null || noteName.trim().isEmpty()) {
                bot.sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова: /addNote");
                return;
            }

            String cleanName = noteName.trim();

            // Шаг 2: Запрашиваем текст заметки, передавая имя через замыкание
            bot.sendMessage(chatId, "Введите текст для заметки \"" + cleanName + "\".");

            // Регистрируем обработчик для получения текста
            bot.setPendingInputHandler(chatId, (noteText) -> {
                if (noteText == null || noteText.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Текст заметки не может быть пустым. Операция отменена.");
                    return;
                }

                try {
                    noteService.addNoteToDB(chatId, cleanName, noteText.trim());
                    bot.sendMessage(chatId, "Заметка \"" + cleanName + "\" добавлена!");
                } catch (Exception e) {
                	e.printStackTrace();
                    bot.sendMessage(chatId, "Ошибка добавления заметки. Попробуйте позже.");
                }
            });
        });
    }
}
