package bot.Note;

import java.sql.*;
import java.util.*;

public class NoteService {
	
	private static final String DB_URL = "jdbc:sqlite:database/Note.db";
	
	// Для драйверов JDBC ниже 4
	static {
	    try {
	        Class.forName("org.sqlite.JDBC");
	    } catch (ClassNotFoundException e) {
	        throw new RuntimeException("SQLite JDBC driver not found", e);
	    }
	}
	
	// Добавление заметки по userId и noteName, primarykey(userId, noteName)
    public void addNoteToDB(Long userId, String noteName, String text) throws SQLException {
    	String sql = "INSERT INTO notes (User_id, Note_name, Text) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, noteName);
            stmt.setString(3, text);
            stmt.executeUpdate();
        }
    }
    
    // Удаление заметки по userId и noteName
    public void removeNoteFromDB(Long userId, String noteName) throws SQLException {
        String sql = "DELETE FROM notes WHERE User_id = ? AND Note_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, noteName);
            stmt.executeUpdate();
        }
    }
    
    // Возвращает текст заметки по userId и noteName
    public String getNote(Long userId, String noteName) throws SQLException {
        String sql = "SELECT text FROM notes WHERE User_id = ? AND Note_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, noteName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Text");
                }
                return null;
            }
        }
    }
    
    // Получение списка имён заметок пользователя
    public List<String> getUserNotes(Long userId) throws SQLException {
        String sql = "SELECT Note_name FROM notes WHERE User_id = ? ORDER BY Note_name";
        List<String> noteNames = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    noteNames.add(rs.getString("Note_name"));
                }
            }
        }
        return noteNames;
    }
}
