package sgpa.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import sgpa.Entities.Fournisseur;
import sgpa.Services.ServicesFournisseur;
import sgpa.Utils.TableCellUtils;
import sgpa.Utils.TableViewUtils;

import java.sql.SQLException;

public class FournisseurController {
    @FXML private TableView<Fournisseur> tvFournisseurs;
    @FXML private TableColumn<Fournisseur, Integer> colId;
    @FXML private TableColumn<Fournisseur, String> colNom;
    @FXML private TableColumn<Fournisseur, String> colContact;
    @FXML private TableColumn<Fournisseur, String> colAdresse;

    @FXML private TextField txtNom;
    @FXML private TextField txtContact;
    @FXML private TextArea txtAdresse;

    private ServicesFournisseur servicesFournisseur;

    public void initialize() {
        servicesFournisseur = new ServicesFournisseur();
        
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        colNom.setCellFactory(TableCellUtils.tooltipIfTruncated());
        colContact.setCellFactory(TableCellUtils.tooltipIfTruncated());
        colAdresse.setCellFactory(TableCellUtils.tooltipIfTruncated());
        TableViewUtils.applyConstrainedResize(tvFournisseurs);

        loadFournisseurs();

        tvFournisseurs.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillFields(newSelection);
            }
        });
    }

    private void loadFournisseurs() {
        try {
            tvFournisseurs.setItems(servicesFournisseur.getAllFournisseurs());
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    private void fillFields(Fournisseur f) {
        txtNom.setText(f.getNom());
        txtContact.setText(f.getContact());
        txtAdresse.setText(f.getAdresse());
    }

    @FXML
    private void handleAdd(ActionEvent event) {
        if (txtNom.getText().isEmpty() || txtContact.getText().isEmpty() || txtAdresse.getText().isEmpty()) {
            showError("Champs manquants", "Veuillez remplir tous les champs (Nom, Contact, Adresse).");
            return;
        }
        try {
            Fournisseur f = new Fournisseur(0, txtNom.getText(), txtContact.getText(), txtAdresse.getText());
            servicesFournisseur.addFournisseur(f);
            loadFournisseurs();
            handleClear(null);
        } catch (SQLException e) {
            showError("Erreur d'ajout", e.getMessage());
        }
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        Fournisseur selected = tvFournisseurs.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection", "Veuillez sélectionner un fournisseur à modifier.");
            return;
        }
        if (txtNom.getText().isEmpty() || txtContact.getText().isEmpty() || txtAdresse.getText().isEmpty()) {
            showError("Champs manquants", "Veuillez remplir tous les champs.");
            return;
        }
        try {
            Fournisseur f = new Fournisseur(selected.getId(), txtNom.getText(), txtContact.getText(), txtAdresse.getText());
            servicesFournisseur.updateFournisseur(f);
            loadFournisseurs();
        } catch (SQLException e) {
            showError("Erreur de modification", e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        Fournisseur selected = tvFournisseurs.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            servicesFournisseur.deleteFournisseur(selected.getId());
            loadFournisseurs();
            handleClear(null);
        } catch (SQLException e) {
            showError("Erreur de suppression", e.getMessage());
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        txtNom.clear();
        txtContact.clear();
        txtAdresse.clear();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
