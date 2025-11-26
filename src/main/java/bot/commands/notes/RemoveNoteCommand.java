package bot.commands.notes;

import bot.TelegramBot;
import bot.Note.NoteService;
import bot.commands.Command;

import java.sql.SQLException;
import java.util.List;
import org.telegram.telegrambots.meta.api.objects.Message;

public class RemoveNoteCommand implements Command {
	
	@Override
    public String getCommandName() {
        return "removeNote";
    }

    @Override
    public String getDescription() {
        return "Удалить заметку";
    }

    @Override
    public String getUsage() {
        return "/removeNote";
    }
    
    private final NoteService noteService = new NoteService();

	@Override
	public void execute(TelegramBot bot, Message message, String[] args) {
		Long chatId = message.getChatId();
		try {
			List<String> noteNames = noteService.getUserNotes(chatId);
			
			//Шаг 1: Выводим список заметок
			if (noteNames.isEmpty()) {
                bot.sendMessage(chatId, "У вас пока нет заметок. Добавьте первую с помощью /addNote");
                return;
			}
			
            // StringBuilder - изменяемый String
        	// Заменить на StringBuffer при добавлении многопоточности
        	StringBuilder response = new StringBuilder("Какую заметку хотите удалить?\n");
            for (int i = 0; i < noteNames.size(); i++) {
                response.append(i + 1).append(". ").append(noteNames.get(i)).append("\n");
            }
            bot.sendMessage(chatId, response.toString().trim());
            
            // Регистрируем обработчик для получения имени заметки
            bot.setPendingInputHandler(chatId, (noteName) -> {
            	if (noteName == null || noteName.trim().isEmpty()) {
            		bot.sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова: /addNote");
                    return;
            	}
            	
            	String cleanName = noteName.trim();
            	
            	//Шаг 2: Удаляем заметку
        		try {
					noteService.removeNoteFromDB(chatId, noteName);
					bot.sendMessage(chatId, "Заметка \"" + cleanName + "\" удалена!");
				} catch (SQLException e) {
					bot.sendMessage(chatId, "Не удалось удалить заметку. Попробуйте позже.");
				}
            });
            
		} catch (Exception e) {
            bot.sendMessage(chatId, "Не удалось вывести список заметок. Попробуйте позже.");
		}
	}
}