package bot.commands;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Message;

import bot.TelegramBot;

@ExtendWith(MockitoExtension.class)
public class HelpCommandTest {

    @Mock
    private TelegramBot bot;

    @Mock
    private Message message;

    private Map<String, Command> commandRegistry;
    private HelpCommand helpCommand;

    @BeforeEach
    void setUp() {
        commandRegistry = new HashMap<>();
        helpCommand = new HelpCommand(commandRegistry);
        when(message.getChatId()).thenReturn(12345L);
    }

    @Test
    void shouldListAllCommands_WhenNoArgsProvided() {
        // Given
        Command authorsCmd = mock(Command.class);
        when(authorsCmd.getCommandName()).thenReturn("authors");
        when(authorsCmd.getDescription()).thenReturn("Информация об авторах");

        Command aboutCmd = mock(Command.class);
        when(aboutCmd.getCommandName()).thenReturn("about");
        when(aboutCmd.getDescription()).thenReturn("Информация о боте");

        commandRegistry.put("authors", authorsCmd);
        commandRegistry.put("about", aboutCmd);

        // When
        helpCommand.execute(bot, message, new String[]{});

        // Then
        String expected = "Доступные команды:\n/about — Информация о боте\n/authors — Информация об авторах\n";
        verify(bot).sendMessage(eq(12345L), eq(expected));
    }

    @Test
    void shouldShowSpecificCommand_WhenValidCommandNameProvided() {
        // Given
        Command noteCmd = mock(Command.class);
        when(noteCmd.getCommandName()).thenReturn("note");
        when(noteCmd.getDescription()).thenReturn("Создать заметку");
        when(noteCmd.getUsage()).thenReturn("/note <текст>");

        commandRegistry.put("note", noteCmd);

        // When
        helpCommand.execute(bot, message, new String[]{"note"});

        // Then
        String expected = "Команда: /note\nОписание: Создать заметку\nИспользование: /note <текст>";
        verify(bot).sendMessage(eq(12345L), eq(expected));
    }

    @Test
    void shouldShowNotFoundMessage_WhenInvalidCommandProvided() {
        // Given — registry пуст или не содержит "unknown"
        // When
        helpCommand.execute(bot, message, new String[]{"unknown"});

        // Then
        String expected = "Команда 'unknown' не найдена.";
        verify(bot).sendMessage(eq(12345L), eq(expected));
    }

    @Test
    void shouldHandleExtraArgs_IgnoringThem() {
        // Given
        Command cmd = mock(Command.class);
        when(cmd.getCommandName()).thenReturn("test");
        when(cmd.getDescription()).thenReturn("Тестовая команда");
        when(cmd.getUsage()).thenReturn("/test");
        commandRegistry.put("test", cmd);

        // When — передаём больше аргументов, но используем только первый
        helpCommand.execute(bot, message, new String[]{"test", "лишнее", "ещё"});

        // Then — всё равно показывает помощь по "test"
        String expected = "Команда: /test\nОписание: Тестовая команда\nИспользование: /test";
        verify(bot).sendMessage(eq(12345L), eq(expected));
    }
}