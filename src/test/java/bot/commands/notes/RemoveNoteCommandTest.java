package bot.commands.notes;

import bot.TelegramBot;
import bot.commands.InputHandler;
import bot.commands.notes.RemoveNoteCommand;
import bot.note.NoteService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class RemoveNoteCommandTest {

    @Mock
    private TelegramBot mockBot;

    @Mock
    private Message mockMessage;

    @Mock
    private NoteService mockNoteService;

    private RemoveNoteCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new RemoveNoteCommand();

        // Подмена noteService через рефлексию
        try {
            var field = RemoveNoteCommand.class.getDeclaredField("noteService");
            field.setAccessible(true);
            field.set(command, mockNoteService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(mockMessage.getChatId()).thenReturn(123L);
    }

    @Test
    void shouldListNotes_thenRequestName_thenRemoveNote_whenUserProvidesValidNoteName() throws SQLException {
        Long chatId = 123L;
        List<String> notes = Arrays.asList("Заметка 1", "Заметка 2");
        try {
			when(mockNoteService.getUserNotes(chatId)).thenReturn(notes);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        command.execute(mockBot, mockMessage, new String[]{});

        // Проверка: отправлен список с заголовком
        String expectedList = "Какую заметку хотите удалить?\n1. Заметка 1\n2. Заметка 2";
        verify(mockBot).sendMessage(chatId, expectedList);

        // Захватываем обработчик ввода имени
        ArgumentCaptor<InputHandler> handlerCaptor = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), handlerCaptor.capture());

        // Имитация ввода имени
        handlerCaptor.getValue().handle("Заметка 1");

        // Проверка: заметка удалена
        verify(mockNoteService).removeNoteFromDB(chatId, "Заметка 1");
        verify(mockBot).sendMessage(chatId, "Заметка \"Заметка 1\" удалена!");
    }

    @Test
    void shouldNotifyNoNotes_whenUserHasNoNotes() throws SQLException {
        Long chatId = 123L;
        when(mockNoteService.getUserNotes(chatId)).thenReturn(List.of());

        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "У вас пока нет заметок. Добавьте первую с помощью /addNote");
        verify(mockBot, never()).setPendingInputHandler(anyLong(), any(InputHandler.class));
    }

    @Test
    void shouldNotifyError_whenFetchingNotesFails() throws SQLException {
        Long chatId = 123L;
        when(mockNoteService.getUserNotes(chatId)).thenThrow(new RuntimeException("DB error"));

        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "Не удалось вывести список заметок. Попробуйте позже.");
    }

    @Test
    void shouldCancelAndNotify_whenNoteNameIsEmpty() throws SQLException {
        Long chatId = 123L;
        List<String> notes = List.of("Test");
        when(mockNoteService.getUserNotes(chatId)).thenReturn(notes);

        command.execute(mockBot, mockMessage, new String[]{});

        String expectedList = "Какую заметку хотите удалить?\n1. Test";
        verify(mockBot).sendMessage(chatId, expectedList);

        ArgumentCaptor<InputHandler> handlerCaptor = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), handlerCaptor.capture());

        handlerCaptor.getValue().handle("   "); // пустое имя

        verify(mockBot).sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова: /addNote");
        verify(mockNoteService, never()).removeNoteFromDB(anyLong(), anyString());
    }

    @Test
    void shouldNotifyError_whenRemoveFails() throws SQLException {
        Long chatId = 123L;
        List<String> notes = List.of("Note to remove");
        when(mockNoteService.getUserNotes(chatId)).thenReturn(notes);

        // Имитация SQLException при удалении
        doThrow(SQLException.class)
            .when(mockNoteService)
            .removeNoteFromDB(chatId, "Note to remove");

        command.execute(mockBot, mockMessage, new String[]{});

        String expectedList = "Какую заметку хотите удалить?\n1. Note to remove";
        verify(mockBot).sendMessage(chatId, expectedList);

        ArgumentCaptor<InputHandler> handlerCaptor = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), handlerCaptor.capture());

        handlerCaptor.getValue().handle("Note to remove");

        verify(mockNoteService).removeNoteFromDB(chatId, "Note to remove");
        verify(mockBot).sendMessage(chatId, "Не удалось удалить заметку. Попробуйте позже.");
    }
}