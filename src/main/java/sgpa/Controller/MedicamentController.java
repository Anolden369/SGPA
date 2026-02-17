package sgpa.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import sgpa.Entities.Medicament;
import sgpa.Services.ServicesMedicament;
import sgpa.Utils.TableCellUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MedicamentController {
    @FXML private TextField txtRecherche;
    @FXML private TreeTableView<Medicament> tvMedicaments;
    @FXML private TreeTableColumn<Medicament, String> colNom;
    @FXML private TreeTableColumn<Medicament, String> colPrincipe;
    @FXML private TreeTableColumn<Medicament, String> colForme;
    @FXML private TreeTableColumn<Medicament, String> colDosage;
    @FXML private TreeTableColumn<Medicament, Double> colPrix;
    @FXML private TreeTableColumn<Medicament, Double> colPrixAchat;
    @FXML private TreeTableColumn<Medicament, Double> colPrixTTC;
    @FXML private TreeTableColumn<Medicament, Double> colTva;
    @FXML private TreeTableColumn<Medicament, Boolean> colOrdonnance;
    @FXML private TreeTableColumn<Medicament, LocalDate> colPeremption;
    @FXML private TreeTableColumn<Medicament, Integer> colStock;
    @FXML private TreeTableColumn<Medicament, Integer> colStockMin;

    @FXML private TextField txtNom;
    @FXML private TextField txtPrincipe;
    @FXML private ComboBox<String> cbForme;
    @FXML private TextField txtDosage;
    @FXML private TextField txtPrix;
    @FXML private TextField txtPrixAchat;
    @FXML private TextField txtTva;
    @FXML private DatePicker dpPeremption;
    @FXML private CheckBox chkOrdonnance;
    @FXML private TextField txtStockMin;
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;

    private ServicesMedicament servicesMedicament;
    private ObservableList<Medicament> allMedicaments;

    public void initialize() {
        servicesMedicament = new ServicesMedicament();
        cbForme.getItems().addAll("Comprimé", "Sirop", "Crème", "Gélule", "Sachet", "Injectable");
        
        colNom.setCellValueFactory(new TreeItemPropertyValueFactory<>("nomCommercial"));
        colPrincipe.setCellValueFactory(new TreeItemPropertyValueFactory<>("principeActif"));
        colForme.setCellValueFactory(new TreeItemPropertyValueFactory<>("formeGalenique"));
        colDosage.setCellValueFactory(new TreeItemPropertyValueFactory<>("dosage"));
        colPrix.setCellValueFactory(new TreeItemPropertyValueFactory<>("prixPublic"));
        colPrixAchat.setCellValueFactory(new TreeItemPropertyValueFactory<>("prixAchatHT"));
        colPrixTTC.setCellValueFactory(new TreeItemPropertyValueFactory<>("prixTTC"));
        colTva.setCellValueFactory(new TreeItemPropertyValueFactory<>("tauxTva"));
        colOrdonnance.setCellValueFactory(new TreeItemPropertyValueFactory<>("necessiteOrdonnance"));
        colPeremption.setCellValueFactory(new TreeItemPropertyValueFactory<>("datePeremption"));
        colStock.setCellValueFactory(new TreeItemPropertyValueFactory<>("stockActuel"));
        colStockMin.setCellValueFactory(new TreeItemPropertyValueFactory<>("stockMinimum"));
        configureRenderers();
        tvMedicaments.setRoot(new TreeItem<>(new Medicament()));
        tvMedicaments.setShowRoot(false);
        tvMedicaments.setRowFactory(tv -> {
            TreeTableRow<Medicament> row = new TreeTableRow<>() {
                @Override
                protected void updateItem(Medicament item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("parent-group-row");
                    if (!empty && getTreeItem() != null && !getTreeItem().isLeaf()) {
                        getStyleClass().add("parent-group-row");
                    }
                }
            };
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                TreeItem<Medicament> item = row.getTreeItem();
                if (item != null && !item.isLeaf()) {
                    item.setExpanded(!item.isExpanded());
                    event.consume();
                }
            });
            return row;
        });

        loadMedicaments();

        tvMedicaments.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && newSelection.isLeaf()) {
                fillFields(newSelection.getValue());
            }
        });
    }

    private void configureRenderers() {
        colOrdonnance.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                boolean isParent = getTreeTableRow() != null && getTreeTableRow().getTreeItem() != null && !getTreeTableRow().getTreeItem().isLeaf();
                setText(empty || item == null || isParent ? null : (item ? "Oui" : "Non"));
            }
        });
        colPrix.setCellFactory(column -> currencyCell());
        colPrixAchat.setCellFactory(column -> currencyCell());
        colPrixTTC.setCellFactory(column -> currencyCell());
        colTva.setCellFactory(column -> percentCell());
        colPeremption.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                boolean isParent = getTreeTableRow() != null && getTreeTableRow().getTreeItem() != null && !getTreeTableRow().getTreeItem().isLeaf();
                setText(empty || item == null || isParent ? null : item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });
        colStockMin.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                boolean isParent = getTreeTableRow() != null && getTreeTableRow().getTreeItem() != null && !getTreeTableRow().getTreeItem().isLeaf();
                setText(empty || item == null || isParent ? null : String.valueOf(item));
            }
        });
    }

    private TreeTableCell<Medicament, Double> currencyCell() {
        return new TreeTableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                boolean isParent = getTreeTableRow() != null && getTreeTableRow().getTreeItem() != null && !getTreeTableRow().getTreeItem().isLeaf();
                setText(empty || item == null || isParent ? null : String.format("%.2f €", item));
            }
        };
    }

    private TreeTableCell<Medicament, Double> percentCell() {
        return new TreeTableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                boolean isParent = getTreeTableRow() != null && getTreeTableRow().getTreeItem() != null && !getTreeTableRow().getTreeItem().isLeaf();
                setText(empty || item == null || isParent ? null : String.format("%.2f %%", item));
            }
        };
    }

    private void loadMedicaments() {
        try {
            allMedicaments = servicesMedicament.getAllMedicaments();
            rebuildTree(allMedicaments);
        } catch (SQLException e) {
            showError("Erreur de chargement", e.getMessage());
        }
    }

    private void fillFields(Medicament m) {
        txtNom.setText(m.getNomCommercial());
        txtPrincipe.setText(m.getPrincipeActif());
        cbForme.setValue(m.getFormeGalenique());
        txtDosage.setText(m.getDosage());
        txtPrix.setText(String.valueOf(m.getPrixPublic()));
        txtPrixAchat.setText(String.valueOf(m.getPrixAchatHT()));
        txtTva.setText(String.valueOf(m.getTauxTva()));
        dpPeremption.setValue(m.getDatePeremption());
        chkOrdonnance.setSelected(m.isNecessiteOrdonnance());
        txtStockMin.setText(String.valueOf(m.getStockMinimum()));
    }

    @FXML
    private void handleAdd(ActionEvent event) {
        try {
            Medicament m = getMedicamentFromFields();
            servicesMedicament.addMedicament(m);
            loadMedicaments();
            handleClear(null);
        } catch (Exception e) {
            showError("Erreur d'ajout", e.getMessage());
        }
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        TreeItem<Medicament> selectedItem = tvMedicaments.getSelectionModel().getSelectedItem();
        Medicament selected = selectedItem == null ? null : selectedItem.getValue();
        if (selectedItem != null && !selectedItem.isLeaf()) selected = null;
        if (selected == null) return;
        try {
            Medicament m = getMedicamentFromFields();
            m.setId(selected.getId());
            servicesMedicament.updateMedicament(m);
            loadMedicaments();
        } catch (Exception e) {
            showError("Erreur de modification", e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        TreeItem<Medicament> selectedItem = tvMedicaments.getSelectionModel().getSelectedItem();
        Medicament selected = selectedItem == null ? null : selectedItem.getValue();
        if (selectedItem != null && !selectedItem.isLeaf()) selected = null;
        if (selected == null) return;
        try {
            servicesMedicament.deleteMedicament(selected.getId());
            loadMedicaments();
            handleClear(null);
        } catch (SQLException e) {
            showError("Erreur de suppression", e.getMessage());
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        txtNom.clear();
        txtPrincipe.clear();
        cbForme.getSelectionModel().clearSelection();
        txtDosage.clear();
        txtPrix.clear();
        txtPrixAchat.clear();
        txtTva.setText("20.00");
        dpPeremption.setValue(null);
        chkOrdonnance.setSelected(false);
        txtStockMin.clear();
    }

    @FXML
    private void handleRecherche(ActionEvent event) {
        String filter = txtRecherche.getText().toLowerCase();
        if (filter.isEmpty()) {
            rebuildTree(allMedicaments);
        } else {
            FilteredList<Medicament> filteredData = new FilteredList<>(allMedicaments, p ->
                    p.getNomCommercial().toLowerCase().contains(filter) ||
                            p.getPrincipeActif().toLowerCase().contains(filter)
            );
            rebuildTree(FXCollections.observableArrayList(filteredData));
        }
    }

    private void rebuildTree(ObservableList<Medicament> source) {
        TreeItem<Medicament> root = new TreeItem<>(new Medicament());
        Map<String, List<Medicament>> grouped = source.stream()
                .collect(Collectors.groupingBy(Medicament::getNomCommercial, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<Medicament>> entry : grouped.entrySet()) {
            List<Medicament> lots = entry.getValue().stream()
                    .sorted((a, b) -> a.getDatePeremption().compareTo(b.getDatePeremption()))
                    .toList();
            if (lots.size() == 1) {
                root.getChildren().add(new TreeItem<>(lots.get(0)));
            } else {
                int totalStock = lots.stream().mapToInt(Medicament::getStockActuel).sum();
                Medicament parent = new Medicament();
                parent.setId(0);
                parent.setNomCommercial(entry.getKey());
                parent.setPrincipeActif("Lots: " + lots.size());
                parent.setStockActuel(totalStock);
                parent.setStockMinimum(lots.stream().mapToInt(Medicament::getStockMinimum).max().orElse(0));
                TreeItem<Medicament> parentItem = new TreeItem<>(parent);

                for (Medicament lot : lots) {
                    parentItem.getChildren().add(new TreeItem<>(lot));
                }
                parentItem.setExpanded(false);
                root.getChildren().add(parentItem);
            }
        }
        tvMedicaments.setRoot(root);
    }

    private Medicament getMedicamentFromFields() throws Exception {
        if (txtNom.getText().isEmpty()) throw new Exception("Le nom commercial est obligatoire.");
        if (txtPrincipe.getText().isEmpty()) throw new Exception("Le principe actif est obligatoire.");
        if (cbForme.getValue() == null) throw new Exception("La forme galénique est obligatoire.");
        if (txtDosage.getText().isEmpty()) throw new Exception("Le dosage est obligatoire.");
        if (txtPrix.getText().isEmpty()) throw new Exception("Le prix est obligatoire.");
        if (txtPrixAchat.getText().isEmpty()) throw new Exception("Le prix d'achat HT est obligatoire.");
        if (txtTva.getText().isEmpty()) throw new Exception("Le taux de TVA est obligatoire.");
        if (dpPeremption.getValue() == null) throw new Exception("La date de péremption est obligatoire.");
        if (txtStockMin.getText().isEmpty()) throw new Exception("Le stock minimum est obligatoire.");

        double prix;
        try {
            prix = Double.parseDouble(txtPrix.getText().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new Exception("Le prix doit être un nombre valide (ex: 10.50).");
        }

        double prixAchat;
        try {
            prixAchat = Double.parseDouble(txtPrixAchat.getText().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new Exception("Le prix d'achat HT doit être un nombre valide (ex: 7.20).");
        }
        if (prixAchat < 0) {
            throw new Exception("Le prix d'achat HT ne peut pas être négatif.");
        }
        if (prixAchat > prix) {
            throw new Exception("Le prix d'achat HT ne peut pas dépasser le prix public HT.");
        }

        double tva;
        try {
            tva = Double.parseDouble(txtTva.getText().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new Exception("Le taux de TVA doit être un nombre valide (ex: 20.0).");
        }

        int stockMin;
        try {
            stockMin = Integer.parseInt(txtStockMin.getText());
        } catch (NumberFormatException e) {
            throw new Exception("Le stock minimum doit être un nombre entier.");
        }

        Medicament m = new Medicament();
        m.setNomCommercial(txtNom.getText());
        m.setPrincipeActif(txtPrincipe.getText());
        m.setFormeGalenique(cbForme.getValue());
        m.setDosage(txtDosage.getText());
        m.setPrixPublic(prix);
        m.setPrixAchatHT(prixAchat);
        m.setTauxTva(tva);
        m.setDatePeremption(dpPeremption.getValue());
        m.setNecessiteOrdonnance(chkOrdonnance.isSelected());
        m.setStockMinimum(stockMin);
        m.setStockActuel(0); // Nouveau médicament, stock 0 par défaut
        return m;
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
