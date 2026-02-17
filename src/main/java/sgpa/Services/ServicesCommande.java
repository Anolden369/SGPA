package sgpa.Services;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sgpa.Entities.Commande;
import sgpa.Entities.LigneCommande;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServicesCommande {
    private Connection cnx;

    public ServicesCommande() {
        this.cnx = ConnexionBDD.getCnx();
    }

    public void creerCommande(int idFournisseur, List<LigneCommande> lignes) throws SQLException {
        cnx.setAutoCommit(false);
        try {
            String sqlCmd = "INSERT INTO commande (id_fournisseur, date_commande, statut) VALUES (?, CURRENT_DATE, 'EN_ATTENTE')";
            PreparedStatement psCmd = cnx.prepareStatement(sqlCmd, Statement.RETURN_GENERATED_KEYS);
            psCmd.setInt(1, idFournisseur);
            psCmd.executeUpdate();

            ResultSet rs = psCmd.getGeneratedKeys();
            if (rs.next()) {
                int idCmd = rs.getInt(1);
                String sqlLigne = "INSERT INTO ligne_commande (id_commande, id_medicament, quantite) VALUES (?, ?, ?)";
                PreparedStatement psLigne = cnx.prepareStatement(sqlLigne);
                for (LigneCommande l : lignes) {
                    psLigne.setInt(1, idCmd);
                    psLigne.setInt(2, l.getIdMedicament());
                    psLigne.setInt(3, l.getQuantite());
                    psLigne.executeUpdate();
                }
            }
            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    public ObservableList<Commande> getAllCommandes() throws SQLException {
        ObservableList<Commande> commandes = FXCollections.observableArrayList();
        String sql = "SELECT c.*, f.nom as nom_fournisseur FROM commande c JOIN fournisseur f ON c.id_fournisseur = f.id ORDER BY c.date_commande DESC";
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            commandes.add(new Commande(
                rs.getInt("id"),
                rs.getInt("id_fournisseur"),
                rs.getString("nom_fournisseur"),
                rs.getDate("date_commande").toLocalDate(),
                rs.getString("statut")
            ));
        }
        return commandes;
    }

    public ObservableList<LigneCommande> getLignesCommande(int idCommande) throws SQLException {
        ObservableList<LigneCommande> lignes = FXCollections.observableArrayList();
        String sql = "SELECT lc.*, m.nom_commercial, m.date_peremption " +
                "FROM ligne_commande lc JOIN medicament m ON lc.id_medicament = m.id WHERE lc.id_commande = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idCommande);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            lignes.add(new LigneCommande(
                rs.getInt("id_commande"),
                rs.getInt("id_medicament"),
                rs.getString("nom_commercial"),
                rs.getInt("quantite"),
                rs.getDate("date_peremption") == null ? null : rs.getDate("date_peremption").toLocalDate()
            ));
        }
        return lignes;
    }

    public void receptionnerCommande(int idCommande) throws SQLException {
        cnx.setAutoCommit(false);
        try {
            ObservableList<LigneCommande> lignes = getLignesCommande(idCommande);
            for (LigneCommande ligne : lignes) {
                int newLotId = insertNewLotFromTemplate(ligne.getIdMedicament(), ligne.getQuantite());
                updateLigneCommandeMedicament(idCommande, ligne.getIdMedicament(), newLotId);
            }

            String sqlUpdateCmd = "UPDATE commande SET statut = 'RECUE' WHERE id = ?";
            PreparedStatement psUpdateCmd = cnx.prepareStatement(sqlUpdateCmd);
            psUpdateCmd.setInt(1, idCommande);
            psUpdateCmd.executeUpdate();

            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    private int insertNewLotFromTemplate(int templateMedicamentId, int quantiteRecue) throws SQLException {
        String sqlTemplate = "SELECT nom_commercial, principe_actif, forme_galenique, dosage, " +
                "prix_public, prix_achat_ht, taux_tva, necessite_ordonnance, stock_minimum " +
                "FROM medicament WHERE id = ?";
        try (PreparedStatement psTemplate = cnx.prepareStatement(sqlTemplate)) {
            psTemplate.setInt(1, templateMedicamentId);
            try (ResultSet rs = psTemplate.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Médicament introuvable pour la ligne de commande: " + templateMedicamentId);
                }

                String nomCommercial = rs.getString("nom_commercial");
                String principeActif = rs.getString("principe_actif");
                String forme = rs.getString("forme_galenique");
                String dosage = rs.getString("dosage");
                double prixPublic = rs.getDouble("prix_public");
                double prixAchatHt = rs.getDouble("prix_achat_ht");
                double tauxTva = rs.getDouble("taux_tva");
                boolean ordonnance = rs.getBoolean("necessite_ordonnance");
                int stockMinimum = rs.getInt("stock_minimum");

                LocalDate nextExpiry = computeNextExpiryDate(nomCommercial, principeActif, forme, dosage);
                String sqlInsert = "INSERT INTO medicament (nom_commercial, principe_actif, forme_galenique, dosage, " +
                        "prix_public, prix_achat_ht, taux_tva, necessite_ordonnance, date_peremption, stock_actuel, stock_minimum) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement psInsert = cnx.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    psInsert.setString(1, nomCommercial);
                    psInsert.setString(2, principeActif);
                    psInsert.setString(3, forme);
                    psInsert.setString(4, dosage);
                    psInsert.setDouble(5, prixPublic);
                    psInsert.setDouble(6, prixAchatHt);
                    psInsert.setDouble(7, tauxTva);
                    psInsert.setBoolean(8, ordonnance);
                    psInsert.setDate(9, Date.valueOf(nextExpiry));
                    psInsert.setInt(10, Math.max(0, quantiteRecue));
                    psInsert.setInt(11, stockMinimum);
                    psInsert.executeUpdate();

                    try (ResultSet generated = psInsert.getGeneratedKeys()) {
                        if (generated.next()) {
                            return generated.getInt(1);
                        }
                    }
                }
            }
        }
        throw new SQLException("Impossible de créer un nouveau lot pour le médicament: " + templateMedicamentId);
    }

    private LocalDate computeNextExpiryDate(String nomCommercial, String principeActif, String forme, String dosage) throws SQLException {
        String sqlMax = "SELECT MAX(date_peremption) FROM medicament " +
                "WHERE nom_commercial = ? AND principe_actif = ? AND forme_galenique = ? AND dosage = ?";
        LocalDate maxKnown = null;
        try (PreparedStatement psMax = cnx.prepareStatement(sqlMax)) {
            psMax.setString(1, nomCommercial);
            psMax.setString(2, principeActif);
            psMax.setString(3, forme);
            psMax.setString(4, dosage);
            try (ResultSet rs = psMax.executeQuery()) {
                if (rs.next()) {
                    Date maxDate = rs.getDate(1);
                    if (maxDate != null) {
                        maxKnown = maxDate.toLocalDate();
                    }
                }
            }
        }

        LocalDate minFuture = LocalDate.now().plusMonths(18);
        LocalDate candidate = maxKnown == null ? minFuture : maxKnown.plusMonths(6);
        if (candidate.isBefore(minFuture)) {
            candidate = minFuture;
        }

        while (existsExactExpiryForProduct(nomCommercial, principeActif, forme, dosage, candidate)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private boolean existsExactExpiryForProduct(String nomCommercial, String principeActif, String forme, String dosage, LocalDate expiry) throws SQLException {
        String sqlExists = "SELECT 1 FROM medicament " +
                "WHERE nom_commercial = ? AND principe_actif = ? AND forme_galenique = ? AND dosage = ? AND date_peremption = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sqlExists)) {
            ps.setString(1, nomCommercial);
            ps.setString(2, principeActif);
            ps.setString(3, forme);
            ps.setString(4, dosage);
            ps.setDate(5, Date.valueOf(expiry));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void updateLigneCommandeMedicament(int idCommande, int oldMedicamentId, int newMedicamentId) throws SQLException {
        String sql = "UPDATE ligne_commande SET id_medicament = ? WHERE id_commande = ? AND id_medicament = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, newMedicamentId);
            ps.setInt(2, idCommande);
            ps.setInt(3, oldMedicamentId);
            ps.executeUpdate();
        }
    }

    public int countCommandesEnAttente() throws SQLException {
        String sql = "SELECT COUNT(*) FROM commande WHERE statut = 'EN_ATTENTE'";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public Map<String, Integer> getRepartitionStatuts() throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("En attente", 0);
        result.put("Reçue", 0);
        String sql = "SELECT statut, COUNT(*) AS nb FROM commande GROUP BY statut";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String statut = rs.getString("statut");
                int count = rs.getInt("nb");
                if ("RECUE".equalsIgnoreCase(statut)) {
                    result.put("Reçue", count);
                } else {
                    result.put("En attente", count);
                }
            }
        }
        return result;
    }
}
