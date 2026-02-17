package sgpa.Services;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class ServicesSettings {
    private final Connection cnx;

    public ServicesSettings() {
        this.cnx = ConnexionBDD.getCnx();
    }

    public void ensureTableExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS app_settings (" +
                "`key` VARCHAR(80) NOT NULL PRIMARY KEY," +
                "`value` TEXT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Statement stmt = cnx.createStatement()) {
            stmt.execute(sql);
        }
    }

    public Map<String, String> getAll() throws SQLException {
        ensureTableExists();
        Map<String, String> values = new HashMap<>();
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT `key`, `value` FROM app_settings")) {
            while (rs.next()) {
                values.put(rs.getString("key"), rs.getString("value"));
            }
        }
        return values;
    }

    public String get(String key, String defaultValue) throws SQLException {
        ensureTableExists();
        String sql = "SELECT `value` FROM app_settings WHERE `key` = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) throws SQLException {
        String value = get(key, String.valueOf(defaultValue));
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    public void putAll(Map<String, String> values) throws SQLException {
        ensureTableExists();
        String sql = "REPLACE INTO app_settings(`key`, `value`) VALUES(?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                ps.setString(1, entry.getKey());
                ps.setString(2, entry.getValue() == null ? "" : entry.getValue().trim());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
