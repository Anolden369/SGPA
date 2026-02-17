package sgpa.Services;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sgpa.Entities.LigneVente;
import sgpa.Entities.PredictionCommande;
import sgpa.Entities.Vente;

import java.sql.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServicesVente {
    private Connection cnx;

    public ServicesVente() {
        this.cnx = ConnexionBDD.getCnx();
    }

    public void enregistrerVente(int idUser, double montantHT, double montantTVA, double montantTTC, boolean surOrdonnance, List<LigneVente> lignes) throws SQLException {
        enregistrerVente(idUser, montantHT, montantTVA, montantTTC, surOrdonnance, lignes, false);
    }

    public void enregistrerVente(int idUser, double montantHT, double montantTVA, double montantTTC, boolean surOrdonnance, List<LigneVente> lignes, boolean useLatestExpiry) throws SQLException {
        cnx.setAutoCommit(false);
        Set<Integer> lowStockIdsAfterSale = new HashSet<>();
        try {
            // 1. Créer la vente
            String sqlVente = "INSERT INTO vente (date_heure, montant_ht, montant_tva, montant_ttc, sur_ordonnance, id_user) VALUES (NOW(), ?, ?, ?, ?, ?)";
            PreparedStatement psVente = cnx.prepareStatement(sqlVente, Statement.RETURN_GENERATED_KEYS);
            psVente.setDouble(1, 0);
            psVente.setDouble(2, 0);
            psVente.setDouble(3, 0);
            psVente.setBoolean(4, surOrdonnance);
            psVente.setInt(5, idUser);
            psVente.executeUpdate();

            ResultSet rs = psVente.getGeneratedKeys();
            if (rs.next()) {
                int idVente = rs.getInt(1);
                
                // 2. Créer les lignes et mettre à jour le stock
                String sqlLigne = "INSERT INTO ligne_vente (id_vente, id_medicament, quantite, prix_unitaire_ht, cout_achat_unitaire_ht, taux_tva, montant_tva, prix_unitaire_ttc) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                String sqlStock = "UPDATE medicament SET stock_actuel = stock_actuel - ? WHERE id = ?";
                PreparedStatement psLigne = cnx.prepareStatement(sqlLigne);
                PreparedStatement psStock = cnx.prepareStatement(sqlStock);
                double totalHTCalc = 0.0;
                double totalTVACalc = 0.0;
                double totalTTCCalc = 0.0;

                for (LigneVente l : lignes) {
                    List<LotAllocation> allocations = allocateLotsForSale(l.getIdMedicament(), l.getQuantite(), useLatestExpiry);
                    for (LotAllocation alloc : allocations) {
                        psLigne.setInt(1, idVente);
                        psLigne.setInt(2, alloc.idMedicament);
                        psLigne.setInt(3, alloc.quantiteVendue);
                        psLigne.setDouble(4, alloc.prixUnitaireHT);
                        psLigne.setDouble(5, alloc.coutAchatUnitaireHT);
                        psLigne.setDouble(6, alloc.tauxTva);
                        psLigne.setDouble(7, alloc.montantTvaUnitaire);
                        psLigne.setDouble(8, alloc.prixUnitaireTTC);
                        psLigne.executeUpdate();

                        psStock.setInt(1, alloc.quantiteVendue);
                        psStock.setInt(2, alloc.idMedicament);
                        psStock.executeUpdate();

                        totalHTCalc += alloc.quantiteVendue * alloc.prixUnitaireHT;
                        totalTVACalc += alloc.quantiteVendue * alloc.montantTvaUnitaire;
                        totalTTCCalc += alloc.quantiteVendue * alloc.prixUnitaireTTC;

                        int stockAfter = alloc.stockAvant - alloc.quantiteVendue;
                        if (stockAfter <= alloc.stockMinimum) {
                            lowStockIdsAfterSale.add(alloc.idMedicament);
                        }
                    }
                }

                String sqlUpdateTotals = "UPDATE vente SET montant_ht=?, montant_tva=?, montant_ttc=? WHERE id=?";
                PreparedStatement psTotals = cnx.prepareStatement(sqlUpdateTotals);
                psTotals.setDouble(1, round2(totalHTCalc));
                psTotals.setDouble(2, round2(totalTVACalc));
                psTotals.setDouble(3, round2(totalTTCCalc));
                psTotals.setInt(4, idVente);
                psTotals.executeUpdate();
            }

            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }

        if (!lowStockIdsAfterSale.isEmpty()) {
            try {
                boolean sent = new ServicesNotification().sendLowStockAlertByIds(new ArrayList<>(lowStockIdsAfterSale));
                if (!sent) {
                    System.err.println("Alerte email non envoyée: SMTP désactivé ou configuration email incomplète.");
                }
            } catch (Exception ignored) {
                System.err.println("Alerte email non envoyée: " + ignored.getMessage());
            }
        }
    }

    public ObservableList<Vente> getHistoriqueVentes() throws SQLException {
        ObservableList<Vente> historique = FXCollections.observableArrayList();
        String sql = "SELECT v.*, " +
                "COALESCE(NULLIF(CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')), ' '), 'Utilisateur supprimé') AS nom_vendeur " +
                "FROM vente v LEFT JOIN user u ON v.id_user = u.id ORDER BY v.date_heure DESC";
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            historique.add(new Vente(
                rs.getInt("id"),
                rs.getTimestamp("date_heure").toLocalDateTime(),
                rs.getDouble("montant_ht"),
                rs.getDouble("montant_tva"),
                rs.getDouble("montant_ttc"),
                rs.getBoolean("sur_ordonnance"),
                rs.getInt("id_user"),
                rs.getString("nom_vendeur")
            ));
        }
        return historique;
    }

    public ObservableList<LigneVente> getLignesVente(int idVente) throws SQLException {
        ObservableList<LigneVente> lignes = FXCollections.observableArrayList();
        String sql = "SELECT lv.*, m.nom_commercial FROM ligne_vente lv JOIN medicament m ON lv.id_medicament = m.id WHERE lv.id_vente = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVente);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            lignes.add(new LigneVente(
                rs.getInt("id_vente"),
                rs.getInt("id_medicament"),
                rs.getString("nom_commercial"),
                rs.getInt("quantite"),
                rs.getDouble("prix_unitaire_ht"),
                rs.getDouble("taux_tva"),
                rs.getDouble("montant_tva"),
                rs.getDouble("prix_unitaire_ttc")
            ));
        }
        return lignes;
    }

    public double getChiffreAffaireMois() throws SQLException {
        String sql = "SELECT SUM(montant_ttc) FROM vente WHERE MONTH(date_heure) = MONTH(CURRENT_DATE) AND YEAR(date_heure) = YEAR(CURRENT_DATE) AND DATE(date_heure) <= CURRENT_DATE";
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) return rs.getDouble(1);
        return 0;
    }

    public double getChiffreAffaireMoisPrecedent() throws SQLException {
        return getMonthAggregate("COALESCE(SUM(montant_ttc), 0)", -1);
    }

    public double getTotalHTMois() throws SQLException {
        String sql = "SELECT SUM(montant_ht) FROM vente WHERE MONTH(date_heure) = MONTH(CURRENT_DATE) AND YEAR(date_heure) = YEAR(CURRENT_DATE) AND DATE(date_heure) <= CURRENT_DATE";
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) return rs.getDouble(1);
        return 0;
    }

    public double getTotalHTMoisPrecedent() throws SQLException {
        return getMonthAggregate("COALESCE(SUM(montant_ht), 0)", -1);
    }

    public double getTotalTVAMois() throws SQLException {
        String sql = "SELECT SUM(montant_tva) FROM vente WHERE MONTH(date_heure) = MONTH(CURRENT_DATE) AND YEAR(date_heure) = YEAR(CURRENT_DATE) AND DATE(date_heure) <= CURRENT_DATE";
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) return rs.getDouble(1);
        return 0;
    }

    public double getTotalTVAMoisPrecedent() throws SQLException {
        return getMonthAggregate("COALESCE(SUM(montant_tva), 0)", -1);
    }

    public double getCoutAchatMois() throws SQLException {
        String sql = "SELECT COALESCE(SUM(lv.quantite * lv.cout_achat_unitaire_ht), 0) " +
                "FROM ligne_vente lv JOIN vente v ON v.id = lv.id_vente " +
                "WHERE MONTH(v.date_heure) = MONTH(CURRENT_DATE) " +
                "AND YEAR(v.date_heure) = YEAR(CURRENT_DATE) AND DATE(v.date_heure) <= CURRENT_DATE";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public double getCoutAchatMoisPrecedent() throws SQLException {
        String sql = "SELECT COALESCE(SUM(lv.quantite * lv.cout_achat_unitaire_ht), 0) " +
                "FROM ligne_vente lv JOIN vente v ON v.id = lv.id_vente " +
                "WHERE DATE_FORMAT(v.date_heure, '%Y-%m') = DATE_FORMAT(DATE_ADD(CURRENT_DATE, INTERVAL -1 MONTH), '%Y-%m')";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public double getMargeHTMois() throws SQLException {
        String sql = "SELECT COALESCE(SUM(lv.quantite * (lv.prix_unitaire_ht - lv.cout_achat_unitaire_ht)), 0) " +
                "FROM ligne_vente lv JOIN vente v ON v.id = lv.id_vente " +
                "WHERE MONTH(v.date_heure) = MONTH(CURRENT_DATE) " +
                "AND YEAR(v.date_heure) = YEAR(CURRENT_DATE) AND DATE(v.date_heure) <= CURRENT_DATE";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public double getMargeHTMoisPrecedent() throws SQLException {
        String sql = "SELECT COALESCE(SUM(lv.quantite * (lv.prix_unitaire_ht - lv.cout_achat_unitaire_ht)), 0) " +
                "FROM ligne_vente lv JOIN vente v ON v.id = lv.id_vente " +
                "WHERE DATE_FORMAT(v.date_heure, '%Y-%m') = DATE_FORMAT(DATE_ADD(CURRENT_DATE, INTERVAL -1 MONTH), '%Y-%m')";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public int getNbVentesMois() throws SQLException {
        String sql = "SELECT COUNT(*) FROM vente WHERE MONTH(date_heure) = MONTH(CURRENT_DATE) AND YEAR(date_heure) = YEAR(CURRENT_DATE) AND DATE(date_heure) <= CURRENT_DATE";
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    public int getNbVentesMoisPrecedent() throws SQLException {
        return (int) getMonthAggregate("COUNT(*)", -1);
    }

    public int getNbVentesMoisByUser(int idUser) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vente WHERE MONTH(date_heure) = MONTH(CURRENT_DATE) " +
                "AND YEAR(date_heure) = YEAR(CURRENT_DATE) AND DATE(date_heure) <= CURRENT_DATE AND id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getNbVentesJour() throws SQLException {
        String sql = "SELECT COUNT(*) FROM vente WHERE DATE(date_heure) = CURRENT_DATE";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int getNbVentesHier() throws SQLException {
        String sql = "SELECT COUNT(*) FROM vente WHERE DATE(date_heure) = DATE_ADD(CURRENT_DATE, INTERVAL -1 DAY)";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int getNbVentesJourByUser(int idUser) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vente WHERE DATE(date_heure) = CURRENT_DATE AND id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getNbVentesHierByUser(int idUser) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vente WHERE DATE(date_heure) = DATE_ADD(CURRENT_DATE, INTERVAL -1 DAY) AND id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public double getChiffreAffaireJour() throws SQLException {
        String sql = "SELECT COALESCE(SUM(montant_ttc), 0) FROM vente WHERE DATE(date_heure) = CURRENT_DATE";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public double getChiffreAffaireJourByUser(int idUser) throws SQLException {
        String sql = "SELECT COALESCE(SUM(montant_ttc), 0) FROM vente WHERE DATE(date_heure) = CURRENT_DATE AND id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0;
    }

    public int getNbVentesSemaine() throws SQLException {
        String sql = "SELECT COUNT(*) FROM vente WHERE YEARWEEK(date_heure, 1) = YEARWEEK(CURRENT_DATE, 1)";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int getNbVentesSemaineByUser(int idUser) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vente WHERE YEARWEEK(date_heure, 1) = YEARWEEK(CURRENT_DATE, 1) AND id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public double getChiffreAffaireSemaine() throws SQLException {
        String sql = "SELECT COALESCE(SUM(montant_ttc), 0) FROM vente WHERE YEARWEEK(date_heure, 1) = YEARWEEK(CURRENT_DATE, 1)";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public double getChiffreAffaireSemainePrecedente() throws SQLException {
        String sql = "SELECT COALESCE(SUM(montant_ttc), 0) FROM vente " +
                "WHERE YEARWEEK(date_heure, 1) = YEARWEEK(DATE_ADD(CURRENT_DATE, INTERVAL -7 DAY), 1)";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public double getChiffreAffaireSemaineByUser(int idUser) throws SQLException {
        String sql = "SELECT COALESCE(SUM(montant_ttc), 0) FROM vente WHERE YEARWEEK(date_heure, 1) = YEARWEEK(CURRENT_DATE, 1) AND id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0;
    }

    public double getChiffreAffaireSemainePrecedenteByUser(int idUser) throws SQLException {
        String sql = "SELECT COALESCE(SUM(montant_ttc), 0) FROM vente " +
                "WHERE YEARWEEK(date_heure, 1) = YEARWEEK(DATE_ADD(CURRENT_DATE, INTERVAL -7 DAY), 1) AND id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0;
    }

    public Map<String, Integer> getVentesSemaineCourante() throws SQLException {
        Map<String, Integer> weekly = new LinkedHashMap<>();
        weekly.put("Lun", 0);
        weekly.put("Mar", 0);
        weekly.put("Mer", 0);
        weekly.put("Jeu", 0);
        weekly.put("Ven", 0);
        weekly.put("Sam", 0);

        String sql = "SELECT DAYOFWEEK(date_heure) AS jour, COUNT(*) AS nb " +
                "FROM vente WHERE YEARWEEK(date_heure, 1) = YEARWEEK(CURRENT_DATE, 1) GROUP BY DAYOFWEEK(date_heure)";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int day = rs.getInt("jour");
                int count = rs.getInt("nb");
                String label = mapDay(day);
                if (weekly.containsKey(label)) {
                    weekly.put(label, count);
                }
            }
        }
        return weekly;
    }

    public Map<String, Integer> getVentesSemaineCouranteByUser(int idUser) throws SQLException {
        Map<String, Integer> weekly = new LinkedHashMap<>();
        weekly.put("Lun", 0);
        weekly.put("Mar", 0);
        weekly.put("Mer", 0);
        weekly.put("Jeu", 0);
        weekly.put("Ven", 0);
        weekly.put("Sam", 0);

        String sql = "SELECT DAYOFWEEK(date_heure) AS jour, COUNT(*) AS nb " +
                "FROM vente WHERE YEARWEEK(date_heure, 1) = YEARWEEK(CURRENT_DATE, 1) AND id_user = ? " +
                "GROUP BY DAYOFWEEK(date_heure)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String label = mapDay(rs.getInt("jour"));
                    if (weekly.containsKey(label)) {
                        weekly.put(label, rs.getInt("nb"));
                    }
                }
            }
        }
        return weekly;
    }

    public Map<String, Integer> getRepartitionOrdonnanceSemaineCourante() throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("Avec ordonnance", 0);
        result.put("Sans ordonnance", 0);

        String sql = "SELECT sur_ordonnance, COUNT(*) AS nb FROM vente " +
                "WHERE YEARWEEK(date_heure, 1) = YEARWEEK(CURRENT_DATE, 1) " +
                "GROUP BY sur_ordonnance";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                boolean ord = rs.getBoolean("sur_ordonnance");
                int count = rs.getInt("nb");
                result.put(ord ? "Avec ordonnance" : "Sans ordonnance", count);
            }
        }
        return result;
    }

    public Map<String, Integer> getRepartitionOrdonnanceSemaineCouranteByUser(int idUser) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("Avec ordonnance", 0);
        result.put("Sans ordonnance", 0);

        String sql = "SELECT sur_ordonnance, COUNT(*) AS nb FROM vente " +
                "WHERE YEARWEEK(date_heure, 1) = YEARWEEK(CURRENT_DATE, 1) AND id_user = ? " +
                "GROUP BY sur_ordonnance";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getBoolean("sur_ordonnance") ? "Avec ordonnance" : "Sans ordonnance", rs.getInt("nb"));
                }
            }
        }
        return result;
    }

    public Map<Integer, Double> getChiffreAffairesParMoisAnneeCourante() throws SQLException {
        Map<Integer, Double> caParMois = new HashMap<>();
        String sql = "SELECT MONTH(date_heure) AS mois, SUM(montant_ttc) AS total " +
                     "FROM vente " +
                     "WHERE YEAR(date_heure) = YEAR(CURRENT_DATE) " +
                     "GROUP BY MONTH(date_heure)";

        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            caParMois.put(rs.getInt("mois"), rs.getDouble("total"));
        }
        return caParMois;
    }

    public Map<YearMonth, Double> getChiffreAffaires12DerniersMois() throws SQLException {
        Map<YearMonth, Double> caParMois = new LinkedHashMap<>();
        YearMonth current = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            caParMois.put(current.minusMonths(i), 0.0);
        }

        String sql = "SELECT DATE_FORMAT(date_heure, '%Y-%m') AS ym, SUM(montant_ttc) AS total " +
                "FROM vente " +
                "WHERE date_heure >= DATE_SUB(CURRENT_DATE, INTERVAL 11 MONTH) AND DATE(date_heure) <= CURRENT_DATE " +
                "GROUP BY DATE_FORMAT(date_heure, '%Y-%m')";

        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            YearMonth ym = YearMonth.parse(rs.getString("ym"), DateTimeFormatter.ofPattern("yyyy-MM"));
            if (caParMois.containsKey(ym)) {
                caParMois.put(ym, rs.getDouble("total"));
            }
        }
        return caParMois;
    }

    public Map<String, Integer> getRepartitionOrdonnanceMoisCourant() throws SQLException {
        Map<String, Integer> repartition = new LinkedHashMap<>();
        repartition.put("Avec ordonnance", 0);
        repartition.put("Sans ordonnance", 0);

        String sql = "SELECT sur_ordonnance, COUNT(*) AS nb " +
                "FROM vente " +
                "WHERE MONTH(date_heure) = MONTH(CURRENT_DATE) AND YEAR(date_heure) = YEAR(CURRENT_DATE) AND DATE(date_heure) <= CURRENT_DATE " +
                "GROUP BY sur_ordonnance";

        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            boolean surOrdonnance = rs.getBoolean("sur_ordonnance");
            int nb = rs.getInt("nb");
            repartition.put(surOrdonnance ? "Avec ordonnance" : "Sans ordonnance", nb);
        }
        return repartition;
    }

    public Map<String, Double> getTopMedicamentsTTCAnneeCourante(int limit) throws SQLException {
        Map<String, Double> top = new LinkedHashMap<>();
        String sql = "SELECT m.nom_commercial, SUM(lv.quantite * lv.prix_unitaire_ttc) AS total_ttc " +
                "FROM ligne_vente lv " +
                "JOIN vente v ON v.id = lv.id_vente " +
                "JOIN medicament m ON m.id = lv.id_medicament " +
                "WHERE YEAR(v.date_heure) = YEAR(CURRENT_DATE) AND DATE(v.date_heure) <= CURRENT_DATE " +
                "GROUP BY m.id, m.nom_commercial " +
                "ORDER BY total_ttc DESC " +
                "LIMIT ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, limit);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            top.put(rs.getString("nom_commercial"), rs.getDouble("total_ttc"));
        }
        return top;
    }

    public ObservableList<PredictionCommande> getSuggestionsCommandeML() throws SQLException {
        Map<Integer, MedicamentStats> statsByMedicament = loadHistoricQuantitiesByMedicament();
        ObservableList<PredictionCommande> result = FXCollections.observableArrayList();
        YearMonth current = YearMonth.now();

        for (MedicamentStats stats : statsByMedicament.values()) {
            List<Double> lastSixMonths = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                YearMonth month = current.minusMonths(i);
                lastSixMonths.add(stats.quantiteParMois.getOrDefault(month, 0.0));
            }

            double predicted = weightedMovingAverage(lastSixMonths);
            double safetyStock = Math.max(stats.stockMinimum, predicted * 0.35);
            int suggestedQty = Math.max((int) Math.ceil(predicted + safetyStock - stats.stockActuel), 0);

            String risk;
            if (predicted <= 0.01) {
                risk = stats.stockActuel <= stats.stockMinimum ? "Moyen" : "Faible";
            } else {
                double coverageMonths = stats.stockActuel / predicted;
                if (coverageMonths < 0.7) risk = "Élevé";
                else if (coverageMonths < 1.4) risk = "Moyen";
                else risk = "Faible";
            }

            result.add(new PredictionCommande(
                    stats.idMedicament,
                    stats.nomMedicament,
                    stats.stockActuel,
                    stats.stockMinimum,
                    round2(predicted),
                    suggestedQty,
                    risk
            ));
        }

        result.sort(Comparator
                .comparingInt(PredictionCommande::getQuantiteSuggeree).reversed()
                .thenComparing(PredictionCommande::getDemandePrediteMensuelle, Comparator.reverseOrder()));
        return result;
    }

    private Map<Integer, MedicamentStats> loadHistoricQuantitiesByMedicament() throws SQLException {
        Map<Integer, MedicamentStats> statsByMedicament = new LinkedHashMap<>();
        String sql = "SELECT m.id, m.nom_commercial, m.stock_actuel, m.stock_minimum, " +
                "DATE_FORMAT(v.date_heure, '%Y-%m') AS ym, SUM(lv.quantite) AS qte " +
                "FROM medicament m " +
                "LEFT JOIN ligne_vente lv ON lv.id_medicament = m.id " +
                "LEFT JOIN vente v ON v.id = lv.id_vente AND v.date_heure >= DATE_SUB(CURRENT_DATE, INTERVAL 6 MONTH) " +
                "GROUP BY m.id, m.nom_commercial, m.stock_actuel, m.stock_minimum, DATE_FORMAT(v.date_heure, '%Y-%m') " +
                "ORDER BY m.id";

        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            int id = rs.getInt("id");
            MedicamentStats stats = statsByMedicament.computeIfAbsent(id, key -> new MedicamentStats(
                    id,
                    rsSafeString(rs, "nom_commercial"),
                    rsSafeInt(rs, "stock_actuel"),
                    rsSafeInt(rs, "stock_minimum")
            ));

            String ymStr = rs.getString("ym");
            if (ymStr != null) {
                YearMonth ym = YearMonth.parse(ymStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                stats.quantiteParMois.put(ym, rs.getDouble("qte"));
            }
        }
        return statsByMedicament;
    }

    private String rsSafeString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return "";
        }
    }

    private int rsSafeInt(ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return 0;
        }
    }

    private double weightedMovingAverage(List<Double> values) {
        int[] weights = {1, 2, 3, 4, 5, 6}; // Plus de poids aux mois récents.
        double weightedSum = 0;
        int totalWeight = 0;
        for (int i = 0; i < values.size() && i < weights.length; i++) {
            weightedSum += values.get(i) * weights[i];
            totalWeight += weights[i];
        }
        if (totalWeight == 0) return 0;
        return weightedSum / totalWeight;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private List<LotAllocation> allocateLotsForSale(int selectedMedicamentId, int requestedQty, boolean useLatestExpiry) throws SQLException {
        List<LotAllocation> allocations = new ArrayList<>();
        if (requestedQty <= 0) {
            return allocations;
        }

        String sqlNom = "SELECT nom_commercial FROM medicament WHERE id = ?";
        String nomCommercial;
        try (PreparedStatement psNom = cnx.prepareStatement(sqlNom)) {
            psNom.setInt(1, selectedMedicamentId);
            try (ResultSet rs = psNom.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Médicament introuvable pour id=" + selectedMedicamentId);
                }
                nomCommercial = rs.getString("nom_commercial");
            }
        }

        String sqlLots = "SELECT id, stock_actuel, stock_minimum, prix_public, prix_achat_ht, taux_tva " +
                "FROM medicament " +
                "WHERE nom_commercial = ? AND stock_actuel > 0 " +
                "ORDER BY date_peremption " + (useLatestExpiry ? "DESC" : "ASC") + ", id " + (useLatestExpiry ? "DESC" : "ASC") + " FOR UPDATE";

        int remaining = requestedQty;
        try (PreparedStatement psLots = cnx.prepareStatement(sqlLots)) {
            psLots.setString(1, nomCommercial);
            try (ResultSet rs = psLots.executeQuery()) {
                while (rs.next() && remaining > 0) {
                    int stock = rs.getInt("stock_actuel");
                    if (stock <= 0) continue;
                    int take = Math.min(stock, remaining);
                    double prixHT = rs.getDouble("prix_public");
                    double coutAchatHT = rs.getDouble("prix_achat_ht");
                    double tauxTva = rs.getDouble("taux_tva");
                    double montantTva = prixHT * (tauxTva / 100.0);
                    double prixTtc = prixHT + montantTva;

                    allocations.add(new LotAllocation(
                            rs.getInt("id"),
                            take,
                            stock,
                            rs.getInt("stock_minimum"),
                            prixHT,
                            coutAchatHT,
                            tauxTva,
                            montantTva,
                            prixTtc
                    ));
                    remaining -= take;
                }
            }
        }

        if (remaining > 0) {
            throw new SQLException("Stock insuffisant pour " + nomCommercial + ". Quantité demandée: " + requestedQty + ".");
        }
        return allocations;
    }

    private static class MedicamentStats {
        private final int idMedicament;
        private final String nomMedicament;
        private final int stockActuel;
        private final int stockMinimum;
        private final Map<YearMonth, Double> quantiteParMois = new HashMap<>();

        private MedicamentStats(int idMedicament, String nomMedicament, int stockActuel, int stockMinimum) {
            this.idMedicament = idMedicament;
            this.nomMedicament = nomMedicament;
            this.stockActuel = stockActuel;
            this.stockMinimum = stockMinimum;
        }
    }

    private static class LotAllocation {
        private final int idMedicament;
        private final int quantiteVendue;
        private final int stockAvant;
        private final int stockMinimum;
        private final double prixUnitaireHT;
        private final double coutAchatUnitaireHT;
        private final double tauxTva;
        private final double montantTvaUnitaire;
        private final double prixUnitaireTTC;

        private LotAllocation(int idMedicament, int quantiteVendue, int stockAvant, int stockMinimum,
                              double prixUnitaireHT, double coutAchatUnitaireHT, double tauxTva,
                              double montantTvaUnitaire, double prixUnitaireTTC) {
            this.idMedicament = idMedicament;
            this.quantiteVendue = quantiteVendue;
            this.stockAvant = stockAvant;
            this.stockMinimum = stockMinimum;
            this.prixUnitaireHT = prixUnitaireHT;
            this.coutAchatUnitaireHT = coutAchatUnitaireHT;
            this.tauxTva = tauxTva;
            this.montantTvaUnitaire = montantTvaUnitaire;
            this.prixUnitaireTTC = prixUnitaireTTC;
        }
    }

    private Map<Integer, int[]> loadStocksBefore(Set<Integer> medicamentIds) throws SQLException {
        Map<Integer, int[]> map = new HashMap<>();
        if (medicamentIds.isEmpty()) {
            return map;
        }
        String sql = "SELECT id, stock_actuel, stock_minimum FROM medicament WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (Integer id : medicamentIds) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        map.put(id, new int[]{rs.getInt("stock_actuel"), rs.getInt("stock_minimum")});
                    }
                }
            }
        }
        return map;
    }

    private String mapDay(int mysqlDayOfWeek) {
        return switch (mysqlDayOfWeek) {
            case 2 -> "Lun";
            case 3 -> "Mar";
            case 4 -> "Mer";
            case 5 -> "Jeu";
            case 6 -> "Ven";
            case 7 -> "Sam";
            default -> "";
        };
    }

    private double getMonthAggregate(String aggregateExpression, int monthOffset) throws SQLException {
        String sql = "SELECT " + aggregateExpression + " FROM vente " +
                "WHERE DATE_FORMAT(date_heure, '%Y-%m') = DATE_FORMAT(DATE_ADD(CURRENT_DATE, INTERVAL ? MONTH), '%Y-%m')";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, monthOffset);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0;
    }
}
