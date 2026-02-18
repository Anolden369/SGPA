package sgpa.Controller;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import sgpa.Entities.Fournisseur;
import sgpa.Entities.LigneCommande;
import sgpa.Entities.PredictionCommande;
import sgpa.Services.ServicesCommande;
import sgpa.Services.ServicesFournisseur;
import sgpa.Services.ServicesVente;
import sgpa.Utils.InAppDialog;
import sgpa.Utils.TableCellUtils;
import sgpa.Utils.TableViewUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MLController {
    @FXML private TableView<PredictionCommande> tvPredictions;
    @FXML private TableColumn<PredictionCommande, Integer> colPredId;
    @FXML private TableColumn<PredictionCommande, String> colPredNom;
    @FXML private TableColumn<PredictionCommande, Integer> colPredStockActuel;
    @FXML private TableColumn<PredictionCommande, Integer> colPredStockMin;
    @FXML private TableColumn<PredictionCommande, Double> colPredDemande;
    @FXML private TableColumn<PredictionCommande, Integer> colPredQte;
    @FXML private TableColumn<PredictionCommande, String> colPredRisque;

    private ServicesVente servicesVente;
    private ServicesFournisseur servicesFournisseur;
    private ServicesCommande servicesCommande;

    public void initialize() {
        servicesVente = new ServicesVente();
        servicesFournisseur = new ServicesFournisseur();
        servicesCommande = new ServicesCommande();

        colPredId.setCellValueFactory(new PropertyValueFactory<>("idMedicament"));
        colPredNom.setCellValueFactory(new PropertyValueFactory<>("nomMedicament"));
        colPredStockActuel.setCellValueFactory(new PropertyValueFactory<>("stockActuel"));
        colPredStockMin.setCellValueFactory(new PropertyValueFactory<>("stockMinimum"));
        colPredDemande.setCellValueFactory(new PropertyValueFactory<>("demandePrediteMensuelle"));
        colPredQte.setCellValueFactory(new PropertyValueFactory<>("quantiteSuggeree"));
        colPredRisque.setCellValueFactory(new PropertyValueFactory<>("niveauRisque"));

        colPredNom.setCellFactory(TableCellUtils.tooltipIfTruncated());
        colPredDemande.setCellFactory(column -> decimalCell());
        colPredRisque.setCellFactory(column -> riskCell());
        TableViewUtils.applyConstrainedResize(tvPredictions);

        loadPredictions();
    }

    private void loadPredictions() {
        try {
            tvPredictions.setItems(servicesVente.getSuggestionsCommandeML());
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les prédictions ML: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshPredictions(ActionEvent event) {
        loadPredictions();
    }

    @FXML
    private void handleNewCommandeFromML(ActionEvent event) {
        try {
            ObservableList<PredictionCommande> predictions = tvPredictions.getItems();
            if (predictions == null || predictions.isEmpty()) {
                showInfo("Info", "Aucune prédiction disponible.");
                return;
            }

            List<LigneCommande> lignes = new ArrayList<>();
            for (PredictionCommande p : predictions) {
                if (p.getQuantiteSuggeree() > 0) {
                    lignes.add(new LigneCommande(0, p.getIdMedicament(), p.getNomMedicament(), p.getQuantiteSuggeree()));
                }
            }
            if (lignes.isEmpty()) {
                showInfo("Info", "Les prédictions n'imposent aucune commande immédiate.");
                return;
            }

            ObservableList<Fournisseur> fournisseurs = servicesFournisseur.getAllFournisseurs();
            if (fournisseurs.isEmpty()) {
                showError("Fournisseur manquant", "Ajoutez au moins un fournisseur avant de générer une commande ML.");
                return;
            }

            boolean confirmed = InAppDialog.confirm(
                    tvPredictions,
                    "Commande recommandée (ML)",
                    "Le système va proposer " + lignes.size() + " ligne(s) de commande.\n"
                            + "Fournisseur par défaut : " + fournisseurs.getFirst().getNom(),
                    "Créer",
                    "Annuler"
            );

            if (confirmed) {
                servicesCommande.creerCommande(fournisseurs.getFirst().getId(), lignes);
                showInfo("Succès", "Commande ML générée avec succès.");
            }
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    private TableCell<PredictionCommande, Double> decimalCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
            }
        };
    }

    private TableCell<PredictionCommande, String> riskCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                if ("Élevé".equals(item)) {
                    setStyle("-fx-text-fill: #bf2f45; -fx-font-weight: 700;");
                } else if ("Moyen".equals(item)) {
                    setStyle("-fx-text-fill: #a36a1d; -fx-font-weight: 700;");
                } else {
                    setStyle("-fx-text-fill: #1f7a57; -fx-font-weight: 700;");
                }
            }
        };
    }

    private void showInfo(String title, String content) {
        InAppDialog.success(tvPredictions, title, content);
    }

    private void showError(String title, String content) {
        InAppDialog.error(tvPredictions, title, content);
    }
}
