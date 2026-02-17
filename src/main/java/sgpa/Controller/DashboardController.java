package sgpa.Controller;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import sgpa.Entities.User;
import sgpa.Services.ServicesCommande;
import sgpa.Services.ServicesMedicament;
import sgpa.Services.ServicesVente;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardController {
    @FXML private Label lblDashboardTitle;
    @FXML private Label lblKpi1Title;
    @FXML private Label lblKpi1Value;
    @FXML private Label lblKpi2Title;
    @FXML private Label lblKpi2Value;
    @FXML private Label lblKpi3Title;
    @FXML private Label lblKpi3Value;
    @FXML private Label lblKpi4Title;
    @FXML private Label lblKpi4Value;
    @FXML private VBox cardKpi1;
    @FXML private VBox cardKpi2;
    @FXML private VBox cardKpi3;
    @FXML private VBox cardKpi4;
    @FXML private Label lblWeekChartTitle;
    @FXML private Label lblSecondaryChartTitle;
    @FXML private BarChart<String, Number> chartWeekly;
    @FXML private PieChart chartSecondary;

    private final ServicesVente servicesVente = new ServicesVente();
    private final ServicesCommande servicesCommande = new ServicesCommande();
    private final ServicesMedicament servicesMedicament = new ServicesMedicament();
    private static final NumberFormat EURO = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    private User currentUser;

    public void initialize() {
        currentUser = LoginController.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            if ("Pharmacien".equals(currentUser.getRole())) {
                loadAdminDashboard();
            } else {
                loadSellerDashboard();
            }
        } catch (SQLException e) {
            lblDashboardTitle.setText("Tableau de bord indisponible");
        }
    }

    private void loadAdminDashboard() throws SQLException {
        lblDashboardTitle.setText("Tableau de bord Administrateur");
        lblKpi1Title.setText("Ventes aujourd'hui");
        int ventesJour = servicesVente.getNbVentesJour();
        lblKpi1Value.setText(String.valueOf(ventesJour));
        setTrendCard(cardKpi1, ventesJour, servicesVente.getNbVentesHier());

        lblKpi2Title.setText("CA semaine TTC");
        double caSemaine = servicesVente.getChiffreAffaireSemaine();
        lblKpi2Value.setText(EURO.format(caSemaine));
        setTrendCard(cardKpi2, caSemaine, servicesVente.getChiffreAffaireSemainePrecedente());

        lblKpi3Title.setText("Commandes en attente");
        lblKpi3Value.setText(String.valueOf(servicesCommande.countCommandesEnAttente()));
        setNeutralCard(cardKpi3);

        lblKpi4Title.setText("Produits en stock bas");
        lblKpi4Value.setText(String.valueOf(servicesMedicament.countStockBas()));
        setNeutralCard(cardKpi4);

        lblWeekChartTitle.setText("Ventes de la semaine (Lundi-Samedi)");
        Map<String, Integer> weekly = servicesVente.getVentesSemaineCourante();
        chartWeekly.getData().setAll(buildWeeklySeries("Ventes", weekly));

        lblSecondaryChartTitle.setText("Répartition des statuts de commande");
        Map<String, Integer> status = servicesCommande.getRepartitionStatuts();
        setPieData(status);
        installChartTooltips();
    }

    private void loadSellerDashboard() throws SQLException {
        int userId = currentUser.getId();
        lblDashboardTitle.setText("Mon tableau de bord vendeur");
        lblKpi1Title.setText("Mes ventes aujourd'hui");
        int ventesJour = servicesVente.getNbVentesJourByUser(userId);
        lblKpi1Value.setText(String.valueOf(ventesJour));
        setTrendCard(cardKpi1, ventesJour, servicesVente.getNbVentesHierByUser(userId));

        lblKpi2Title.setText("Mon CA semaine TTC");
        double caSemaine = servicesVente.getChiffreAffaireSemaineByUser(userId);
        lblKpi2Value.setText(EURO.format(caSemaine));
        setTrendCard(cardKpi2, caSemaine, servicesVente.getChiffreAffaireSemainePrecedenteByUser(userId));

        lblKpi3Title.setText("Mes ventes semaine");
        lblKpi3Value.setText(String.valueOf(servicesVente.getNbVentesSemaineByUser(userId)));
        setNeutralCard(cardKpi3);

        lblKpi4Title.setText("Mes ventes du mois");
        lblKpi4Value.setText(String.valueOf(servicesVente.getNbVentesMoisByUser(userId)));
        setNeutralCard(cardKpi4);

        lblWeekChartTitle.setText("Mes ventes de la semaine (Lundi-Samedi)");
        Map<String, Integer> weekly = servicesVente.getVentesSemaineCouranteByUser(userId);
        chartWeekly.getData().setAll(buildWeeklySeries("Mes ventes", weekly));

        lblSecondaryChartTitle.setText("Répartition de l'état du stock");
        Map<String, Integer> stock = servicesMedicament.getRepartitionEtatStock();
        setPieData(stock);
        installChartTooltips();
    }

    private XYChart.Series<String, Number> buildWeeklySeries(String name, Map<String, Integer> weekly) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        Map<String, Integer> ordered = new LinkedHashMap<>();
        ordered.put("Lun", weekly.getOrDefault("Lun", 0));
        ordered.put("Mar", weekly.getOrDefault("Mar", 0));
        ordered.put("Mer", weekly.getOrDefault("Mer", 0));
        ordered.put("Jeu", weekly.getOrDefault("Jeu", 0));
        ordered.put("Ven", weekly.getOrDefault("Ven", 0));
        ordered.put("Sam", weekly.getOrDefault("Sam", 0));
        for (Map.Entry<String, Integer> entry : ordered.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        return series;
    }

    private void setPieData(Map<String, Integer> values) {
        chartSecondary.getData().clear();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            chartSecondary.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
    }

    private void installChartTooltips() {
        Platform.runLater(() -> {
            for (XYChart.Series<String, Number> series : chartWeekly.getData()) {
                for (XYChart.Data<String, Number> point : series.getData()) {
                    if (point.getNode() != null) {
                        Tooltip.install(point.getNode(), new Tooltip(point.getXValue() + " : " + point.getYValue().intValue() + " vente(s)"));
                    }
                }
            }
            for (PieChart.Data slice : chartSecondary.getData()) {
                if (slice.getNode() != null) {
                    Tooltip.install(slice.getNode(), new Tooltip(slice.getName() + " : " + (int) slice.getPieValue()));
                }
            }
        });
    }

    private void setNeutralCard(VBox card) {
        if (card == null) return;
        card.getStyleClass().removeAll("kpi-positive", "kpi-negative", "kpi-stable");
        card.getStyleClass().add("kpi-stable");
    }

    private void setTrendCard(VBox card, double current, double previous) {
        if (card == null) return;
        card.getStyleClass().removeAll("kpi-positive", "kpi-negative", "kpi-stable");
        if (Math.abs(current - previous) < 0.01) {
            card.getStyleClass().add("kpi-stable");
        } else if (current > previous) {
            card.getStyleClass().add("kpi-positive");
        } else {
            card.getStyleClass().add("kpi-negative");
        }
    }
}
