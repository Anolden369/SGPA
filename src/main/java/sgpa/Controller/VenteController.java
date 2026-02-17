package sgpa.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;
import sgpa.Entities.Client;
import sgpa.Entities.LigneVente;
import sgpa.Entities.Medicament;
import sgpa.Entities.Vente;
import sgpa.Services.ServicesDocumentVente;
import sgpa.Services.ServicesMedicament;
import sgpa.Services.ServicesNotification;
import sgpa.Services.ServicesVente;
import sgpa.Utils.TableCellUtils;
import sgpa.Utils.TableViewUtils;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class VenteController {
    @FXML private ComboBox<Medicament> cbMedicaments;
    @FXML private ComboBox<Medicament> cbLotsPeremption;
    @FXML private TextField txtQuantite;
    @FXML private TableView<LigneVente> tvPanier;
    @FXML private TableColumn<LigneVente, String> colPanierNom;
    @FXML private TableColumn<LigneVente, Double> colPanierPrixHT;
    @FXML private TableColumn<LigneVente, Double> colPanierTVA;
    @FXML private TableColumn<LigneVente, Double> colPanierPrixTTC;
    @FXML private TableColumn<LigneVente, Integer> colPanierQte;
    @FXML private TableColumn<LigneVente, Double> colPanierTotalHT;
    @FXML private TableColumn<LigneVente, Double> colPanierTotalTTC;
    @FXML private Label lblTotalHT;
    @FXML private Label lblTotalTVA;
    @FXML private Label lblTotalTTC;
    @FXML private CheckBox chkOrdonnance;

    @FXML private TableView<Vente> tvHistorique;
    @FXML private TableColumn<Vente, String> colHistId;
    @FXML private TableColumn<Vente, LocalDateTime> colHistDate;
    @FXML private TableColumn<Vente, Double> colHistHT;
    @FXML private TableColumn<Vente, Double> colHistTVA;
    @FXML private TableColumn<Vente, Double> colHistTTC;
    @FXML private TableColumn<Vente, Boolean> colHistOrd;
    @FXML private TableColumn<Vente, String> colHistVendeur;

    @FXML private TableView<LigneVente> tvDetails;
    @FXML private TableColumn<LigneVente, String> colDetNom;
    @FXML private TableColumn<LigneVente, Integer> colDetQte;
    @FXML private TableColumn<LigneVente, Double> colDetPrixHT;
    @FXML private TableColumn<LigneVente, Double> colDetTVA;
    @FXML private TableColumn<LigneVente, Double> colDetPrixTTC;

    private ServicesVente servicesVente;
    private ServicesMedicament servicesMedicament;
    private ServicesDocumentVente servicesDocumentVente;
    private ServicesNotification servicesNotification;
    private ObservableList<LigneVente> panier;
    private ObservableList<Medicament> medicinesByName;
    private ObservableList<Medicament> allMedicaments;
    private double montantTotalHT = 0.0;
    private double montantTotalTVA = 0.0;
    private double montantTotalTTC = 0.0;

    public void initialize() {
        servicesVente = new ServicesVente();
        servicesMedicament = new ServicesMedicament();
        servicesDocumentVente = new ServicesDocumentVente();
        servicesNotification = new ServicesNotification();
        panier = FXCollections.observableArrayList();
        medicinesByName = FXCollections.observableArrayList();
        allMedicaments = FXCollections.observableArrayList();

        colPanierNom.setCellValueFactory(new PropertyValueFactory<>("nomMedicament"));
        colPanierPrixHT.setCellValueFactory(new PropertyValueFactory<>("prixUnitaireHT"));
        colPanierTVA.setCellValueFactory(new PropertyValueFactory<>("tauxTva"));
        colPanierPrixTTC.setCellValueFactory(new PropertyValueFactory<>("prixUnitaireTTC"));
        colPanierQte.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colPanierTotalHT.setCellValueFactory(new PropertyValueFactory<>("sousTotalHT"));
        colPanierTotalTTC.setCellValueFactory(new PropertyValueFactory<>("sousTotalTTC"));
        tvPanier.setItems(panier);

        colHistId.setCellValueFactory(new PropertyValueFactory<>("reference"));
        colHistDate.setCellValueFactory(new PropertyValueFactory<>("dateHeure"));
        colHistHT.setCellValueFactory(new PropertyValueFactory<>("montantHT"));
        colHistTVA.setCellValueFactory(new PropertyValueFactory<>("montantTVA"));
        colHistTTC.setCellValueFactory(new PropertyValueFactory<>("montantTTC"));
        colHistOrd.setCellValueFactory(new PropertyValueFactory<>("surOrdonnance"));
        colHistVendeur.setCellValueFactory(new PropertyValueFactory<>("nomVendeur"));

        colDetNom.setCellValueFactory(new PropertyValueFactory<>("nomMedicament"));
        colDetQte.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colDetPrixHT.setCellValueFactory(new PropertyValueFactory<>("prixUnitaireHT"));
        colDetTVA.setCellValueFactory(new PropertyValueFactory<>("tauxTva"));
        colDetPrixTTC.setCellValueFactory(new PropertyValueFactory<>("prixUnitaireTTC"));
        configureRenderers();
        TableViewUtils.applyConstrainedResize(tvPanier, tvHistorique, tvDetails);
        cbLotsPeremption.setConverter(new StringConverter<>() {
            @Override
            public String toString(Medicament m) {
                if (m == null) return "";
                return String.format("Exp. %s | Stock %d | HT %.2f€",
                        m.getDatePeremption().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        m.getStockActuel(),
                        m.getPrixPublic());
            }

            @Override
            public Medicament fromString(String string) {
                return null;
            }
        });
        cbMedicaments.valueProperty().addListener((obs, oldVal, newVal) -> refreshLotsForSelectedName());

        tvHistorique.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadDetails(newVal.getId());
            }
        });

        loadData();
    }

    private void loadDetails(int idVente) {
        try {
            tvDetails.setItems(servicesVente.getLignesVente(idVente));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        try {
            ObservableList<Medicament> all = servicesMedicament.getAllMedicaments();
            allMedicaments.setAll(all);
            java.util.Map<String, Medicament> byName = new java.util.LinkedHashMap<>();
            for (Medicament m : all) {
                byName.putIfAbsent(m.getNomCommercial(), m);
            }
            medicinesByName.setAll(byName.values());
            cbMedicaments.setItems(medicinesByName);
            cbMedicaments.setConverter(new StringConverter<>() {
                @Override
                public String toString(Medicament m) {
                    if (m == null) return "";
                    return m.getNomCommercial();
                }

                @Override
                public Medicament fromString(String string) {
                    return null;
                }
            });
            if (!medicinesByName.isEmpty()) {
                cbMedicaments.getSelectionModel().selectFirst();
                refreshLotsForSelectedName();
            }
            tvHistorique.setItems(servicesVente.getHistoriqueVentes());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void configureRenderers() {
        colPanierNom.setCellFactory(TableCellUtils.tooltipIfTruncated());
        colPanierPrixHT.setCellFactory(column -> currencyCell());
        colPanierPrixTTC.setCellFactory(column -> currencyCell());
        colPanierTotalHT.setCellFactory(column -> currencyCell());
        colPanierTotalTTC.setCellFactory(column -> currencyCell());
        colPanierTVA.setCellFactory(column -> percentCell());

        colHistHT.setCellFactory(column -> currencyCell());
        colHistTVA.setCellFactory(column -> currencyCell());
        colHistTTC.setCellFactory(column -> currencyCell());
        colHistOrd.setCellFactory(TableCellUtils.booleanOuiNonCell());
        colHistVendeur.setCellFactory(TableCellUtils.tooltipIfTruncated());
        colHistDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
        });

        colDetNom.setCellFactory(TableCellUtils.tooltipIfTruncated());
        colDetPrixHT.setCellFactory(column -> currencyCell());
        colDetPrixTTC.setCellFactory(column -> currencyCell());
        colDetTVA.setCellFactory(column -> percentCell());
    }

    private <S> TableCell<S, Double> currencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f €", item));
            }
        };
    }

    private <S> TableCell<S, Double> percentCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f %%", item));
            }
        };
    }

    @FXML
    private void handleAddToPanier(ActionEvent event) {
        Medicament m = cbLotsPeremption.getSelectionModel().getSelectedItem();
        String qteStr = txtQuantite.getText();

        if (m == null) {
            showError("Sélection", "Veuillez sélectionner un lot (péremption).");
            return;
        }
        if (qteStr.isEmpty()) {
            showError("Quantité", "Veuillez saisir une quantité.");
            return;
        }

        int qte;
        try {
            qte = Integer.parseInt(qteStr);
            if (qte <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Quantité invalide", "La quantité doit être un nombre entier positif.");
            return;
        }

        if (m.isNecessiteOrdonnance() && !chkOrdonnance.isSelected()) {
            showError("Ordonnance requise", "Ce médicament nécessite une ordonnance. Veuillez cocher la case 'Vente sur Ordonnance'.");
            return;
        }

        int stockLot = m.getStockActuel();
        int dejaDansPanier = panier.stream()
                .filter(l -> l.getIdMedicament() == m.getId())
                .mapToInt(LigneVente::getQuantite)
                .sum();
        if (qte + dejaDansPanier > stockLot) {
            showError("Stock insuffisant", "Stock disponible pour ce lot : " + stockLot +
                    " (déjà au panier : " + dejaDansPanier + ").");
            return;
        }
        
        double prixHT = m.getPrixPublic();
        double tauxTVA = m.getTauxTva();
        double montantTVA = prixHT * (tauxTVA / 100.0);
        double prixTTC = prixHT + montantTVA;

        Optional<LigneVente> ligneExistante = panier.stream()
                .filter(l -> l.getIdMedicament() == m.getId())
                .findFirst();
        if (ligneExistante.isPresent()) {
            LigneVente existante = ligneExistante.get();
            existante.setQuantite(existante.getQuantite() + qte);
            tvPanier.refresh();
        } else {
            LigneVente lv = new LigneVente(0, m.getId(), m.getNomCommercial(), qte, prixHT, tauxTVA, montantTVA, prixTTC);
            panier.add(lv);
        }
        updateTotal();
        txtQuantite.clear();
    }

    private void updateTotal() {
        montantTotalHT = 0.0;
        montantTotalTVA = 0.0;
        montantTotalTTC = 0.0;
        for (LigneVente lv : panier) {
            montantTotalHT += lv.getSousTotalHT();
            montantTotalTVA += lv.getSousTotalTVA();
            montantTotalTTC += lv.getSousTotalTTC();
        }
        lblTotalHT.setText(String.format("%.2f €", montantTotalHT));
        lblTotalTVA.setText(String.format("%.2f €", montantTotalTVA));
        lblTotalTTC.setText(String.format("%.2f €", montantTotalTTC));
    }

    @FXML
    private void handleGenerateDevisPdf(ActionEvent event) {
        if (panier.isEmpty()) {
            showError("Devis", "Le panier est vide.");
            return;
        }
        Client clientInfo = askClientDocumentInfo("Générer un devis");
        if (clientInfo == null) return;

        File file = pickOutputFile("Enregistrer le devis PDF", "Devis_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now()));
        if (file == null) return;

        try {
            servicesDocumentVente.generateDevisPdf(file, clientInfo, FXCollections.observableArrayList(panier));
            String mailStatus = maybeSendDocumentByMail(clientInfo, file, "Devis SGPA", "Veuillez trouver ci-joint votre devis.");
            showInfo("Succès", "Devis PDF généré.\n" + mailStatus);
        } catch (Exception e) {
            showError("Erreur devis", e.getMessage());
        }
    }

    @FXML
    private void handleGenerateFacturePdf(ActionEvent event) {
        if (panier.isEmpty()) {
            showError("Facture", "Le panier est vide.");
            return;
        }
        Client clientInfo = askClientDocumentInfo("Générer une facture");
        if (clientInfo == null) return;

        File file = pickOutputFile("Enregistrer la facture PDF", "Facture_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now()));
        if (file == null) return;

        try {
            servicesDocumentVente.generateFacturePdf(file, clientInfo, null, FXCollections.observableArrayList(panier));
            String mailStatus = maybeSendDocumentByMail(clientInfo, file, "Facture SGPA", "Veuillez trouver ci-joint votre facture.");
            showInfo("Succès", "Facture PDF générée.\n" + mailStatus);
        } catch (Exception e) {
            showError("Erreur facture", e.getMessage());
        }
    }

    @FXML
    private void handleGenerateFactureFromHistory(ActionEvent event) {
        Vente selected = tvHistorique.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Facture", "Sélectionnez une vente dans l'historique.");
            return;
        }

        Client clientInfo = askClientDocumentInfo("Générer la facture de " + selected.getReference());
        if (clientInfo == null) return;

        File file = pickOutputFile("Enregistrer la facture PDF", "Facture_" + selected.getReference());
        if (file == null) return;

        try {
            List<LigneVente> lines = servicesVente.getLignesVente(selected.getId());
            servicesDocumentVente.generateFacturePdf(file, clientInfo, selected, lines);
            String mailStatus = maybeSendDocumentByMail(clientInfo, file, "Facture " + selected.getReference(), "Veuillez trouver ci-joint votre facture.");
            showInfo("Succès", "Facture PDF générée depuis l'historique.\n" + mailStatus);
        } catch (Exception e) {
            showError("Erreur facture", e.getMessage());
        }
    }

    private File pickOutputFile(String title, String baseFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName(baseFileName + ".pdf");
        return chooser.showSaveDialog(tvPanier.getScene() == null ? null : tvPanier.getScene().getWindow());
    }

    private Client askClientDocumentInfo(String title) {
        final Client[] resultHolder = new Client[1];
        TextField txtNom = new TextField();
        TextField txtAdresse = new TextField();
        TextField txtVille = new TextField();
        TextField txtCarteVitale = new TextField();
        CheckBox chkSendMail = new CheckBox("Envoyer par email");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("client@exemple.fr");
        txtEmail.setDisable(true);
        chkSendMail.selectedProperty().addListener((obs, oldV, selected) -> txtEmail.setDisable(!selected));

        txtNom.setPromptText("Nom et prénom");
        txtAdresse.setPromptText("Adresse client");
        txtVille.setPromptText("Code postal et ville");
        txtCarteVitale.setPromptText("N° carte vitale (optionnel)");
        Label lblErrorInline = new Label();
        lblErrorInline.getStyleClass().add("login-error");
        lblErrorInline.setVisible(false);
        lblErrorInline.setManaged(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("doc-form-grid");
        grid.add(new Label("Destinataire*:"), 0, 0); grid.add(txtNom, 1, 0);
        grid.add(new Label("Adresse*:"), 0, 1); grid.add(txtAdresse, 1, 1);
        grid.add(new Label("Ville*:"), 0, 2); grid.add(txtVille, 1, 2);
        grid.add(new Label("Carte vitale:"), 0, 3); grid.add(txtCarteVitale, 1, 3);
        grid.add(chkSendMail, 1, 4);
        grid.add(new Label("Email destinataire:"), 0, 5); grid.add(txtEmail, 1, 5);
        GridPane.setHgrow(txtNom, Priority.ALWAYS);
        GridPane.setHgrow(txtAdresse, Priority.ALWAYS);
        GridPane.setHgrow(txtVille, Priority.ALWAYS);
        GridPane.setHgrow(txtCarteVitale, Priority.ALWAYS);
        GridPane.setHgrow(txtEmail, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("module-title");
        Label subtitleLabel = new Label("Informations client (non enregistrées en base)");
        subtitleLabel.getStyleClass().add("muted-label");

        Button btnCancel = new Button("Annuler");
        btnCancel.getStyleClass().add("btn-ghost-soft");
        Button btnValidate = new Button("Générer");
        btnValidate.getStyleClass().add("btn-download");

        HBox actions = new HBox(10, btnCancel, btnValidate);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("sales-action-bar");

        VBox root = new VBox(12, titleLabel, subtitleLabel, grid, lblErrorInline, actions);
        root.setPadding(new Insets(20));
        root.setPrefWidth(560);
        root.getStyleClass().addAll("card", "doc-form-root");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/CSS/MesStyles.css").toExternalForm());

        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.initModality(Modality.WINDOW_MODAL);
        final double[] dragOffsetX = new double[1];
        final double[] dragOffsetY = new double[1];
        root.setOnMousePressed(e -> {
            dragOffsetX[0] = e.getSceneX();
            dragOffsetY[0] = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX[0]);
            stage.setY(e.getScreenY() - dragOffsetY[0]);
        });
        Window owner = tvPanier.getScene() == null ? null : tvPanier.getScene().getWindow();
        if (owner != null) {
            stage.initOwner(owner);
            stage.setOnShown(e -> {
                stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2);
            });
        }

        btnCancel.setOnAction(e -> stage.close());
        btnValidate.setOnAction(e -> {
            String nom = txtNom.getText() == null ? "" : txtNom.getText().trim();
            String adresse = txtAdresse.getText() == null ? "" : txtAdresse.getText().trim();
            String ville = txtVille.getText() == null ? "" : txtVille.getText().trim();
            String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
            boolean sendMail = chkSendMail.isSelected();

            if (nom.isBlank() || adresse.isBlank() || ville.isBlank()) {
                lblErrorInline.setText("Les champs destinataire, adresse et ville sont obligatoires.");
                lblErrorInline.setVisible(true);
                lblErrorInline.setManaged(true);
                return;
            }
            if (sendMail && email.isBlank()) {
                lblErrorInline.setText("Saisissez un email destinataire ou décochez l'envoi email.");
                lblErrorInline.setVisible(true);
                lblErrorInline.setManaged(true);
                return;
            }
            if (sendMail && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                lblErrorInline.setText("Format email invalide.");
                lblErrorInline.setVisible(true);
                lblErrorInline.setManaged(true);
                return;
            }
            resultHolder[0] = new Client(
                    nom,
                    adresse,
                    ville,
                    txtCarteVitale.getText() == null ? "" : txtCarteVitale.getText().trim(),
                    sendMail,
                    email
            );
            stage.close();
        });

        stage.showAndWait();
        return resultHolder[0];
    }

    private String maybeSendDocumentByMail(Client clientInfo, File attachment, String subject, String message) {
        if (!clientInfo.isSendByMail()) {
            return "Email: non envoyé (option non cochée).";
        }
        try {
            String htmlBody = "<html><body style='font-family:Verdana,Arial,sans-serif'>" +
                    "<p>" + message + "</p>" +
                    "<p>Cordialement,<br/>Pharmacie SGPA</p>" +
                    "</body></html>";
            servicesNotification.sendDocumentMail(clientInfo.getEmailDestinataire(), subject, htmlBody, attachment);
            return "Email: envoyé à " + clientInfo.getEmailDestinataire() + ".";
        } catch (Exception e) {
            return "Email: échec d'envoi (" + e.getMessage() + ").";
        }
    }

    @FXML
    private void handleValiderVente(ActionEvent event) {
        if (panier.isEmpty()) return;
        try {
            int idUser = LoginController.getCurrentUser().getId();
            servicesVente.enregistrerVente(idUser, montantTotalHT, montantTotalTVA, montantTotalTTC, chkOrdonnance.isSelected(), panier);
            panier.clear();
            updateTotal();
            loadData();
            showInfo("Succès", "Vente enregistrée avec succès.");
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    private void refreshLotsForSelectedName() {
        Medicament selectedName = cbMedicaments.getSelectionModel().getSelectedItem();
        if (selectedName == null) {
            cbLotsPeremption.getItems().clear();
            return;
        }
        ObservableList<Medicament> lots = FXCollections.observableArrayList(
                allMedicaments.stream()
                        .filter(m -> m.getNomCommercial().equalsIgnoreCase(selectedName.getNomCommercial()))
                        .sorted((a, b) -> a.getDatePeremption().compareTo(b.getDatePeremption()))
                        .toList()
        );
        cbLotsPeremption.setItems(lots);
        if (!lots.isEmpty()) {
            cbLotsPeremption.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleClearPanier(ActionEvent event) {
        panier.clear();
        updateTotal();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
