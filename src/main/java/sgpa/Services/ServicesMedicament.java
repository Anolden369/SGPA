package sgpa.Services;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sgpa.Entities.Medicament;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class ServicesMedicament {
    private Connection cnx;

    public ServicesMedicament() {
        this.cnx = ConnexionBDD.getCnx();
    }

    public ObservableList<Medicament> getAllMedicaments() throws SQLException {
        ObservableList<Medicament> medicaments = FXCollections.observableArrayList();
        String sql = "SELECT * FROM medicament ORDER BY nom_commercial ASC, date_peremption ASC, id ASC";
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            medicaments.add(new Medicament(
                rs.getInt("id"),
                rs.getString("nom_commercial"),
                rs.getString("principe_actif"),
                rs.getString("forme_galenique"),
                rs.getString("dosage"),
                rs.getDouble("prix_public"),
                rs.getDouble("prix_achat_ht"),
                rs.getDouble("taux_tva"),
                rs.getBoolean("necessite_ordonnance"),
                rs.getDate("date_peremption").toLocalDate(),
                rs.getInt("stock_actuel"),
                rs.getInt("stock_minimum")
            ));
        }
        return medicaments;
    }

    public void addMedicament(Medicament m) throws SQLException {
        String sql = "INSERT INTO medicament (nom_commercial, principe_actif, forme_galenique, dosage, prix_public, prix_achat_ht, taux_tva, necessite_ordonnance, date_peremption, stock_actuel, stock_minimum) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, m.getNomCommercial());
        ps.setString(2, m.getPrincipeActif());
        ps.setString(3, m.getFormeGalenique());
        ps.setString(4, m.getDosage());
        ps.setDouble(5, m.getPrixPublic());
        ps.setDouble(6, m.getPrixAchatHT());
        ps.setDouble(7, m.getTauxTva());
        ps.setBoolean(8, m.isNecessiteOrdonnance());
        ps.setDate(9, Date.valueOf(m.getDatePeremption()));
        ps.setInt(10, m.getStockActuel());
        ps.setInt(11, m.getStockMinimum());
        ps.executeUpdate();
    }

    public void updateMedicament(Medicament m) throws SQLException {
        String sql = "UPDATE medicament SET nom_commercial=?, principe_actif=?, forme_galenique=?, dosage=?, prix_public=?, prix_achat_ht=?, taux_tva=?, necessite_ordonnance=?, date_peremption=?, stock_actuel=?, stock_minimum=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, m.getNomCommercial());
        ps.setString(2, m.getPrincipeActif());
        ps.setString(3, m.getFormeGalenique());
        ps.setString(4, m.getDosage());
        ps.setDouble(5, m.getPrixPublic());
        ps.setDouble(6, m.getPrixAchatHT());
        ps.setDouble(7, m.getTauxTva());
        ps.setBoolean(8, m.isNecessiteOrdonnance());
        ps.setDate(9, Date.valueOf(m.getDatePeremption()));
        ps.setInt(10, m.getStockActuel());
        ps.setInt(11, m.getStockMinimum());
        ps.setInt(12, m.getId());
        ps.executeUpdate();
    }

    public void deleteMedicament(int id) throws SQLException {
        cnx.setAutoCommit(false);
        try {
            try (PreparedStatement ps1 = cnx.prepareStatement("DELETE FROM ligne_vente WHERE id_medicament = ?")) {
                ps1.setInt(1, id);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = cnx.prepareStatement("DELETE FROM ligne_commande WHERE id_medicament = ?")) {
                ps2.setInt(1, id);
                ps2.executeUpdate();
            }
            try (PreparedStatement ps3 = cnx.prepareStatement("DELETE FROM medicament WHERE id = ?")) {
                ps3.setInt(1, id);
                ps3.executeUpdate();
            }
            // Nettoyage commandes orphelines (sans ligne)
            try (PreparedStatement ps4 = cnx.prepareStatement(
                    "DELETE c FROM commande c LEFT JOIN ligne_commande lc ON lc.id_commande = c.id WHERE lc.id_commande IS NULL")) {
                ps4.executeUpdate();
            }
            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }
    
    public ObservableList<Medicament> getAlertesStock() throws SQLException {
        ObservableList<Medicament> alertes = FXCollections.observableArrayList();
        String sql = "SELECT * FROM medicament WHERE stock_actuel <= stock_minimum";
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            alertes.add(new Medicament(
                rs.getInt("id"),
                rs.getString("nom_commercial"),
                rs.getString("principe_actif"),
                rs.getString("forme_galenique"),
                rs.getString("dosage"),
                rs.getDouble("prix_public"),
                rs.getDouble("prix_achat_ht"),
                rs.getDouble("taux_tva"),
                rs.getBoolean("necessite_ordonnance"),
                rs.getDate("date_peremption").toLocalDate(),
                rs.getInt("stock_actuel"),
                rs.getInt("stock_minimum")
            ));
        }
        return alertes;
    }

    public int getStockDisponibleParNom(String nomCommercial) throws SQLException {
        String sql = "SELECT COALESCE(SUM(stock_actuel), 0) FROM medicament WHERE nom_commercial = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nomCommercial);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    public ObservableList<Medicament> getAlertesPeremption() throws SQLException {
        ObservableList<Medicament> alertes = FXCollections.observableArrayList();
        // Alerte si la date de péremption est entre aujourd'hui et les 3 prochains mois.
        String sql = "SELECT * FROM medicament WHERE date_peremption BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE, INTERVAL 3 MONTH)";
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            alertes.add(new Medicament(
                rs.getInt("id"),
                rs.getString("nom_commercial"),
                rs.getString("principe_actif"),
                rs.getString("forme_galenique"),
                rs.getString("dosage"),
                rs.getDouble("prix_public"),
                rs.getDouble("prix_achat_ht"),
                rs.getDouble("taux_tva"),
                rs.getBoolean("necessite_ordonnance"),
                rs.getDate("date_peremption").toLocalDate(),
                rs.getInt("stock_actuel"),
                rs.getInt("stock_minimum")
            ));
        }
        return alertes;
    }

    public int countStockBas() throws SQLException {
        String sql = "SELECT COUNT(*) FROM medicament WHERE stock_actuel <= stock_minimum";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public Map<String, Integer> getRepartitionEtatStock() throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("Rupture", 0);
        result.put("Stock bas", 0);
        result.put("Stock OK", 0);

        String sql = "SELECT " +
                "SUM(CASE WHEN stock_actuel <= 0 THEN 1 ELSE 0 END) AS rupture, " +
                "SUM(CASE WHEN stock_actuel > 0 AND stock_actuel <= stock_minimum THEN 1 ELSE 0 END) AS bas, " +
                "SUM(CASE WHEN stock_actuel > stock_minimum THEN 1 ELSE 0 END) AS ok " +
                "FROM medicament";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                result.put("Rupture", rs.getInt("rupture"));
                result.put("Stock bas", rs.getInt("bas"));
                result.put("Stock OK", rs.getInt("ok"));
            }
        }
        return result;
    }

    public ObservableList<Medicament> getMedicamentsByIds(List<Integer> ids) throws SQLException {
        ObservableList<Medicament> medicaments = FXCollections.observableArrayList();
        if (ids == null || ids.isEmpty()) {
            return medicaments;
        }

        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (int ignored : ids) {
            joiner.add("?");
        }

        String sql = "SELECT * FROM medicament WHERE id IN " + joiner;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            int i = 1;
            for (Integer id : ids) {
                ps.setInt(i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    medicaments.add(new Medicament(
                            rs.getInt("id"),
                            rs.getString("nom_commercial"),
                            rs.getString("principe_actif"),
                            rs.getString("forme_galenique"),
                            rs.getString("dosage"),
                            rs.getDouble("prix_public"),
                            rs.getDouble("prix_achat_ht"),
                            rs.getDouble("taux_tva"),
                            rs.getBoolean("necessite_ordonnance"),
                            rs.getDate("date_peremption").toLocalDate(),
                            rs.getInt("stock_actuel"),
                            rs.getInt("stock_minimum")
                    ));
                }
            }
        }
        return medicaments;
    }
}
