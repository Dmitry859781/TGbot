package bot.commands.notes;

import bot.TelegramBot;
import bot.commands.InputHandler;
import bot.commands.notes.AddNoteCommand;
import bot.note.NoteService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.Message;

import static org.mockito.Mockito.*;

import java.sql.SQLException;

public class AddNoteCommandTest {

    @Mock
    private TelegramBot mockBot;

    @Mock
    private Message mockMessage;

    @Mock
    private NoteService mockNoteService;

    @Captor
    private ArgumentCaptor<InputHandler> firstHandlerCaptor;

    @Captor
    private ArgumentCaptor<InputHandler> secondHandlerCaptor;

    private AddNoteCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new AddNoteCommand();

        try {
            var field = AddNoteCommand.class.getDeclaredField("noteService");
            field.setAccessible(true);
            field.set(command, mockNoteService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(mockMessage.getChatId()).thenReturn(123L);
    }

    @Test
    void shouldRequestNoteName_thenNoteText_andSaveNote_whenUserProvidesValidInputs() {
        Long chatId = 123L;
        when(mockMessage.getChatId()).thenReturn(chatId);

        // Запускаем команду — зарегистрирован 1-й обработчик
        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "Введите имя добавляемой заметки.");

        // Получаем и вызываем 1-й обработчик
        ArgumentCaptor<InputHandler> captor1 = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), captor1.capture());
        InputHandler nameHandler = captor1.getValue();
        nameHandler.handle("Список покупок");

        verify(mockBot).sendMessage(chatId, "Введите текст для заметки \"Список покупок\".");

        // захватываем все вызовы setPendingInputHandler
        ArgumentCaptor<InputHandler> allHandlers = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot, times(2)).setPendingInputHandler(eq(chatId), allHandlers.capture());

        // Второй обработчик — последний в списке
        InputHandler textHandler = allHandlers.getAllValues().get(1);
        textHandler.handle("Молоко, хлеб, яйца");

        try {
			verify(mockNoteService).addNoteToDB(chatId, "Список покупок", "Молоко, хлеб, яйца");
		} catch (SQLException e) {
			e.printStackTrace();
		}
        verify(mockBot).sendMessage(chatId, "Заметка \"Список покупок\" добавлена!");
    }

    @Test
    void shouldNotifyUserAndCancelOperation_whenNoteTextIsEmpty() {
        Long chatId = 123L;
        when(mockMessage.getChatId()).thenReturn(chatId);

        // Запускаем команду — зарегистрирован 1-й обработчик
        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "Введите имя добавляемой заметки.");

        // Получаем и вызываем 1-й обработчик
        ArgumentCaptor<InputHandler> captor1 = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), captor1.capture());
        InputHandler nameHandler = captor1.getValue();
        nameHandler.handle("Напоминание");

        verify(mockBot).sendMessage(chatId, "Введите текст для заметки \"Напоминание\".");

        // Захватываем ВСЕ вызовы setPendingInputHandler (их должно быть 2)
        ArgumentCaptor<InputHandler> allHandlers = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot, times(2)).setPendingInputHandler(eq(chatId), allHandlers.capture());

        // Второй обработчик — последний в списке
        InputHandler textHandler = allHandlers.getAllValues().get(1);
        textHandler.handle("   "); // пустой текст

        // Проверки
        verify(mockBot).sendMessage(chatId, "Текст заметки не может быть пустым. Операция отменена.");
        try {
			verify(mockNoteService, never()).addNoteToDB(anyLong(), anyString(), anyString());
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }

    @Test
    void shouldNotifyUserAndCancelOperation_whenNoteNameIsEmpty() {
        Long chatId = 123L;

        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).setPendingInputHandler(eq(chatId), firstHandlerCaptor.capture());
        firstHandlerCaptor.getValue().handle("   ");

        verify(mockBot).sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова: /addNote");
        try {
			verify(mockNoteService, never()).addNoteToDB(anyLong(), anyString(), anyString());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}