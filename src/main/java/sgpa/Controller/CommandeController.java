package sgpa.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.MouseEvent;
import sgpa.Entities.Commande;
import sgpa.Entities.Fournisseur;
import sgpa.Entities.LigneCommande;
import sgpa.Entities.Medicament;
import sgpa.Services.ServicesCommande;
import sgpa.Services.ServicesFournisseur;
import sgpa.Services.ServicesMedicament;
import sgpa.Utils.TableCellUtils;
import sgpa.Utils.TableViewUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CommandeController {
    @FXML private ComboBox<Fournisseur> cbFournisseurs;
    @FXML private ComboBox<Medicament> cbMedicaments;
    @FXML private TextField txtQuantite;
    @FXML private TreeTableView<CommandePanierRow> tvPanier;
    @FXML private TreeTableColumn<CommandePanierRow, String> colPanierNom;
    @FXML private TreeTableColumn<CommandePanierRow, String> colPanierPeremption;
    @FXML private TreeTableColumn<CommandePanierRow, Integer> colPanierQte;

    @FXML private TableView<Commande> tvCommandes;
    @FXML private TableColumn<Commande, String> colCmdId;
    @FXML private TableColumn<Commande, String> colCmdFournisseur;
    @FXML private TableColumn<Commande, LocalDate> colCmdDate;
    @FXML private TableColumn<Commande, String> colCmdStatut;
    @FXML private TableView<LigneCommande> tvLignesCommande;
    @FXML private TableColumn<LigneCommande, String> colDetNom;
    @FXML private TableColumn<LigneCommande, LocalDate> colDetPeremption;
    @FXML private TableColumn<LigneCommande, Integer> colDetQte;
    @FXML private Button btnReceptionner;
    @FXML private Button btnValiderCommande;

    private ServicesCommande servicesCommande;
    private ServicesFournisseur servicesFournisseur;
    private ServicesMedicament servicesMedicament;
    private ObservableList<LigneCommande> panier;
    private final DateTimeFormatter peremptionFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void initialize() {
        servicesCommande = new ServicesCommande();
        servicesFournisseur = new ServicesFournisseur();
        servicesMedicament = new ServicesMedicament();
        panier = FXCollections.observableArrayList();

        colPanierNom.setCellValueFactory(new TreeItemPropertyValueFactory<>("label"));
        colPanierPeremption.setCellValueFactory(new TreeItemPropertyValueFactory<>("peremption"));
        colPanierQte.setCellValueFactory(new TreeItemPropertyValueFactory<>("quantite"));
        tvPanier.setRoot(new TreeItem<>(new CommandePanierRow("", "", 0)));
        tvPanier.setRowFactory(tv -> {
            TreeTableRow<CommandePanierRow> row = new TreeTableRow<>() {
                @Override
                protected void updateItem(CommandePanierRow item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("parent-group-row");
                    if (!empty && getTreeItem() != null && !getTreeItem().isLeaf()) {
                        getStyleClass().add("parent-group-row");
                    }
                }
            };
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                TreeItem<CommandePanierRow> item = row.getTreeItem();
                if (item != null && !item.isLeaf()) {
                    item.setExpanded(!item.isExpanded());
                    event.consume();
                }
            });
            return row;
        });

        colCmdId.setCellValueFactory(new PropertyValueFactory<>("reference"));
        colCmdFournisseur.setCellValueFactory(new PropertyValueFactory<>("nomFournisseur"));
        colCmdDate.setCellValueFactory(new PropertyValueFactory<>("dateCommande"));
        colCmdStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colCmdFournisseur.setCellFactory(TableCellUtils.tooltipIfTruncated());
        colCmdDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });
        colCmdStatut.setCellFactory(TableCellUtils.tooltipIfTruncated());
        TableViewUtils.applyConstrainedResize(tvCommandes, tvLignesCommande);

        colDetNom.setCellValueFactory(new PropertyValueFactory<>("nomMedicament"));
        colDetPeremption.setCellValueFactory(new PropertyValueFactory<>("datePeremption"));
        colDetQte.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colDetNom.setCellFactory(TableCellUtils.tooltipIfTruncated());
        colDetPeremption.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "-" : item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });

        tvCommandes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                tvLignesCommande.getItems().clear();
                return;
            }
            try {
                tvLignesCommande.setItems(servicesCommande.getLignesCommande(newVal.getId()));
            } catch (SQLException e) {
                showError("Erreur", "Impossible de charger les lignes de commande: " + e.getMessage());
            }
        });

        loadData();
        applyRoleRestrictions();
    }

    private void applyRoleRestrictions() {
        var user = LoginController.getCurrentUser();
        boolean isPharmacien = user != null && "Pharmacien".equals(user.getRole());
        if (btnValiderCommande != null) {
            btnValiderCommande.setVisible(isPharmacien);
            btnValiderCommande.setManaged(isPharmacien);
        }
    }

    private void loadData() {
        try {
            cbFournisseurs.setItems(servicesFournisseur.getAllFournisseurs());
            ObservableList<Medicament> all = servicesMedicament.getAllMedicaments();
            Map<String, Medicament> uniqByName = new LinkedHashMap<>();
            for (Medicament m : all) {
                uniqByName.putIfAbsent(m.getNomCommercial(), m);
            }
            cbMedicaments.setItems(FXCollections.observableArrayList(uniqByName.values()));
            cbMedicaments.setConverter(new javafx.util.StringConverter<>() {
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
            tvCommandes.setItems(servicesCommande.getAllCommandes());
            tvLignesCommande.getItems().clear();
            refreshPanierTree();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddLigne(ActionEvent event) {
        Medicament m = cbMedicaments.getSelectionModel().getSelectedItem();
        String qteStr = txtQuantite.getText();

        if (m == null) {
            showError("Sélection", "Veuillez sélectionner un médicament.");
            return;
        }
        if (qteStr.isEmpty()) {
            showError("Quantité", "Veuillez saisir une quantité.");
            return;
        }

        try {
            int qte = Integer.parseInt(qteStr);
            if (qte <= 0) throw new NumberFormatException();

            Optional<LigneCommande> existing = panier.stream()
                    .filter(l -> l.getIdMedicament() == m.getId())
                    .findFirst();
            if (existing.isPresent()) {
                LigneCommande line = existing.get();
                line.setQuantite(line.getQuantite() + qte);
                tvPanier.refresh();
            } else {
                panier.add(new LigneCommande(0, m.getId(), m.getNomCommercial(), qte, m.getDatePeremption()));
            }
            refreshPanierTree();
            txtQuantite.clear();
        } catch (NumberFormatException e) {
            showError("Quantité invalide", "La quantité doit être un nombre entier positif.");
        }
    }

    @FXML
    private void handleValiderCommande(ActionEvent event) {
        var user = LoginController.getCurrentUser();
        if (user == null || !"Pharmacien".equals(user.getRole())) {
            showError("Accès refusé", "Seul le Pharmacien peut lancer une commande fournisseur.");
            return;
        }

        Fournisseur f = cbFournisseurs.getSelectionModel().getSelectedItem();
        if (f == null) {
            showError("Fournisseur", "Veuillez sélectionner un fournisseur.");
            return;
        }
        if (panier.isEmpty()) {
            showError("Bon de commande vide", "Veuillez ajouter au moins un médicament au bon de commande.");
            return;
        }

        try {
            servicesCommande.creerCommande(f.getId(), panier);
            panier.clear();
            loadData();
            showInfo("Succès", "Commande créée avec succès");
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleViderPanier(ActionEvent event) {
        panier.clear();
        refreshPanierTree();
    }

    @FXML
    private void handleReception(ActionEvent event) {
        Commande selected = tvCommandes.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getStatut().equals("RECUE")) {
                showError("Erreur", "Cette commande a déjà été réceptionnée.");
                return;
            }
            try {
                servicesCommande.receptionnerCommande(selected.getId());
                loadData();
                showInfo("Succès", "Commande réceptionnée, stock mis à jour.");
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
            }
        }
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

    private void refreshPanierTree() {
        TreeItem<CommandePanierRow> root = new TreeItem<>(new CommandePanierRow("", "", 0));
        Map<String, List<LigneCommande>> grouped = new LinkedHashMap<>();
        for (LigneCommande line : panier) {
            grouped.computeIfAbsent(line.getNomMedicament(), k -> new java.util.ArrayList<>()).add(line);
        }

        for (Map.Entry<String, List<LigneCommande>> entry : grouped.entrySet()) {
            int total = entry.getValue().stream().mapToInt(LigneCommande::getQuantite).sum();
            List<LigneCommande> lines = entry.getValue().stream()
                    .sorted((a, b) -> {
                        if (a.getDatePeremption() == null && b.getDatePeremption() == null) return 0;
                        if (a.getDatePeremption() == null) return 1;
                        if (b.getDatePeremption() == null) return -1;
                        return a.getDatePeremption().compareTo(b.getDatePeremption());
                    })
                    .toList();

            if (lines.size() == 1) {
                LigneCommande line = lines.get(0);
                String p = line.getDatePeremption() == null ? "-" : line.getDatePeremption().format(peremptionFmt);
                root.getChildren().add(new TreeItem<>(new CommandePanierRow(entry.getKey(), p, line.getQuantite())));
            } else {
                TreeItem<CommandePanierRow> parent = new TreeItem<>(new CommandePanierRow(entry.getKey(), "Total", total));
                lines.forEach(line -> {
                    String p = line.getDatePeremption() == null ? "-" : line.getDatePeremption().format(peremptionFmt);
                    parent.getChildren().add(new TreeItem<>(new CommandePanierRow("Lot #" + line.getIdMedicament(), p, line.getQuantite())));
                });
                parent.setExpanded(false);
                root.getChildren().add(parent);
            }
        }

        tvPanier.setRoot(root);
    }

    public static class CommandePanierRow {
        private final String label;
        private final String peremption;
        private final Integer quantite;

        public CommandePanierRow(String label, String peremption, Integer quantite) {
            this.label = label;
            this.peremption = peremption;
            this.quantite = quantite;
        }

        public String getLabel() { return label; }
        public String getPeremption() { return peremption; }
        public Integer getQuantite() { return quantite; }
    }
}
