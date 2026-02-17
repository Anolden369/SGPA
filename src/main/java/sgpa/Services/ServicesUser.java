package sgpa.Services;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sgpa.Entities.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

public class ServicesUser {
    private static final String HASH_PREFIX = "PBKDF2";
    private static final int PBKDF2_ITERATIONS = 65_536;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final String DELETED_USER_EMAIL = "deleted@sgpa.local";
    private Connection cnx;

    public ServicesUser() {
        this.cnx = ConnexionBDD.getCnx();
    }

    public User verifLogin(String email, String password) throws SQLException {
        User user = null;
        String sql = "SELECT u.id, u.nom, u.prenom, u.email, u.password, r.libelle " +
                     "FROM user u " +
                     "JOIN role r ON u.id_role = r.id " +
                     "WHERE u.email = ?";
        
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String storedPassword = rs.getString("password");
            if (verifyPassword(password, storedPassword)) {
                user = new User(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("libelle")
                );
                // Migration transparente d'un ancien mot de passe en clair vers un hash.
                if (!isHashedPassword(storedPassword)) {
                    updatePassword(rs.getInt("id"), password);
                }
            }
        }
        return user;
    }
    public ObservableList<User> getAllUsers() throws SQLException {
        ObservableList<User> users = FXCollections.observableArrayList();
        String sql = "SELECT u.id, u.nom, u.prenom, u.email, r.libelle " +
                "FROM user u JOIN role r ON u.id_role = r.id " +
                "WHERE u.email <> ?";
        PreparedStatement stmt = cnx.prepareStatement(sql);
        stmt.setString(1, DELETED_USER_EMAIL);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            users.add(new User(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getString("libelle")
            ));
        }
        return users;
    }

    public void addUser(User u, String password, int idRole) throws SQLException {
        String sql = "INSERT INTO user (nom, prenom, email, password, id_role) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, hashPassword(password));
        ps.setInt(5, idRole);
        ps.executeUpdate();
    }

    public void updateUser(User u, int idRole) throws SQLException {
        String sql = "UPDATE user SET nom=?, prenom=?, email=?, id_role=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setInt(4, idRole);
        ps.setInt(5, u.getId());
        ps.executeUpdate();
    }

    public void deleteUser(int id) throws SQLException {
        cnx.setAutoCommit(false);
        try {
            int deletedUserId = getOrCreateDeletedUserId();
            if (id == deletedUserId) {
                throw new SQLException("Cet utilisateur technique ne peut pas être supprimé.");
            }

            // Conserve l'historique: on réaffecte les ventes vers un utilisateur technique.
            String sqlVentes = "UPDATE vente SET id_user = ? WHERE id_user = ?";
            try (PreparedStatement psVentes = cnx.prepareStatement(sqlVentes)) {
                psVentes.setInt(1, deletedUserId);
                psVentes.setInt(2, id);
                psVentes.executeUpdate();
            }

            // Supprime l'utilisateur
            String sqlUser = "DELETE FROM user WHERE id = ?";
            try (PreparedStatement psUser = cnx.prepareStatement(sqlUser)) {
                psUser.setInt(1, id);
                psUser.executeUpdate();
            }

            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    private int getOrCreateDeletedUserId() throws SQLException {
        String sqlFind = "SELECT id FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sqlFind)) {
            ps.setString(1, DELETED_USER_EMAIL);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        int fallbackRoleId = 2;
        String sqlRole = "SELECT id FROM role WHERE libelle = 'Preparateur/Vendeur' LIMIT 1";
        try (PreparedStatement psRole = cnx.prepareStatement(sqlRole);
             ResultSet rsRole = psRole.executeQuery()) {
            if (rsRole.next()) {
                fallbackRoleId = rsRole.getInt(1);
            }
        }

        String sqlInsert = "INSERT INTO user (nom, prenom, email, password, id_role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement psInsert = cnx.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            psInsert.setString(1, "supprimé");
            psInsert.setString(2, "Utilisateur");
            psInsert.setString(3, DELETED_USER_EMAIL);
            psInsert.setString(4, hashPassword("deleted-user-blocked-login"));
            psInsert.setInt(5, fallbackRoleId);
            psInsert.executeUpdate();
            try (ResultSet rs = psInsert.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Impossible de créer l'utilisateur technique supprimé.");
    }

    public void updatePassword(int idUser, String rawPassword) throws SQLException {
        String sql = "UPDATE user SET password=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, hashPassword(rawPassword));
        ps.setInt(2, idUser);
        ps.executeUpdate();
    }

    public int countUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM user";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private boolean verifyPassword(String rawPassword, String storedPassword) throws SQLException {
        if (storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        if (!isHashedPassword(storedPassword)) {
            return storedPassword.equals(rawPassword);
        }
        try {
            String[] parts = storedPassword.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] computedHash = pbkdf2(rawPassword, salt, iterations);
            return MessageDigest.isEqual(expectedHash, computedHash);
        } catch (RuntimeException | GeneralSecurityException e) {
            throw new SQLException("Impossible de vérifier le mot de passe.", e);
        }
    }

    private boolean isHashedPassword(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(HASH_PREFIX + "$");
    }

    private String hashPassword(String rawPassword) throws SQLException {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            byte[] hash = pbkdf2(rawPassword, salt, PBKDF2_ITERATIONS);
            return HASH_PREFIX + "$" + PBKDF2_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new SQLException("Impossible de chiffrer le mot de passe.", e);
        }
    }

    private byte[] pbkdf2(String rawPassword, byte[] salt, int iterations) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }
}
