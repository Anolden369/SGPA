package sgpa.Services;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sgpa.Entities.Fournisseur;

import java.sql.*;

public class ServicesFournisseur {
    private Connection cnx;

    public ServicesFournisseur() {
        this.cnx = ConnexionBDD.getCnx();
    }

    public ObservableList<Fournisseur> getAllFournisseurs() throws SQLException {
        ObservableList<Fournisseur> fournisseurs = FXCollections.observableArrayList();
        String sql = "SELECT * FROM fournisseur";
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            fournisseurs.add(new Fournisseur(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("contact"),
                rs.getString("adresse")
            ));
        }
        return fournisseurs;
    }

    public void addFournisseur(Fournisseur f) throws SQLException {
        String sql = "INSERT INTO fournisseur (nom, contact, adresse) VALUES (?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, f.getNom());
        ps.setString(2, f.getContact());
        ps.setString(3, f.getAdresse());
        ps.executeUpdate();
    }

    public void updateFournisseur(Fournisseur f) throws SQLException {
        String sql = "UPDATE fournisseur SET nom=?, contact=?, adresse=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, f.getNom());
        ps.setString(2, f.getContact());
        ps.setString(3, f.getAdresse());
        ps.setInt(4, f.getId());
        ps.executeUpdate();
    }

    public void deleteFournisseur(int id) throws SQLException {
        cnx.setAutoCommit(false);
        try {
            // Supprime d'abord les lignes de commandes du fournisseur
            String sqlLignes = "DELETE lc FROM ligne_commande lc " +
                    "JOIN commande c ON c.id = lc.id_commande WHERE c.id_fournisseur = ?";
            try (PreparedStatement psLignes = cnx.prepareStatement(sqlLignes)) {
                psLignes.setInt(1, id);
                psLignes.executeUpdate();
            }

            // Puis les commandes
            String sqlCmd = "DELETE FROM commande WHERE id_fournisseur = ?";
            try (PreparedStatement psCmd = cnx.prepareStatement(sqlCmd)) {
                psCmd.setInt(1, id);
                psCmd.executeUpdate();
            }

            // Puis le fournisseur
            String sqlF = "DELETE FROM fournisseur WHERE id = ?";
            try (PreparedStatement psF = cnx.prepareStatement(sqlF)) {
                psF.setInt(1, id);
                psF.executeUpdate();
            }

            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }
}
