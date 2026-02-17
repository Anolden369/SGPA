package sgpa.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.collections.ObservableList;
import javafx.util.converter.IntegerStringConverter;
import sgpa.Entities.Fournisseur;
import sgpa.Entities.LigneCommande;
import sgpa.Entities.Medicament;
import sgpa.Services.ServicesCommande;
import sgpa.Services.ServicesFournisseur;
import sgpa.Services.ServicesMedicament;
import sgpa.Utils.TableCellUtils;
import sgpa.Utils.TableViewUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.stream.Collectors;

public class StockController {
    @FXML private TreeTableView<Medicament> tvAlertesStock;
    @FXML private TreeTableColumn<Medicament, String> colStockNom;
    @FXML private TreeTableColumn<Medicament, LocalDate> colStockPeremption;
    @FXML private TreeTableColumn<Medicament, Integer> colStockActuel;
    @FXML private TreeTableColumn<Medicament, Integer> colStockMin;
    @FXML private TreeTableColumn<Medicament, Integer> colStockQteCmd;

    @FXML private TreeTableView<Medicament> tvAlertesPeremption;
    @FXML private TreeTableColumn<Medicament, String> colPerNom;
    @FXML private TreeTableColumn<Medicament, LocalDate> colPerDate;
    @FXML private TreeTableColumn<Medicament, Integer> colPerStock;
    @FXML private Button btnLancerCommande;

    private ServicesMedicament servicesMedicament;

    private ServicesFournisseur servicesFournisseur;
    private ServicesCommande servicesCommande;

    public void initialize() {
        servicesMedicament = new ServicesMedicament();
        servicesFournisseur = new ServicesFournisseur();
        servicesCommande = new ServicesCommande();

        colStockNom.setCellValueFactory(new TreeItemPropertyValueFactory<>("nomCommercial"));
        colStockPeremption.setCellValueFactory(new TreeItemPropertyValueFactory<>("datePeremption"));
        colStockActuel.setCellValueFactory(new TreeItemPropertyValueFactory<>("stockActuel"));
        colStockMin.setCellValueFactory(new TreeItemPropertyValueFactory<>("stockMinimum"));
        colStockQteCmd.setCellValueFactory(new TreeItemPropertyValueFactory<>("quantiteCommande"));

        colPerNom.setCellValueFactory(new TreeItemPropertyValueFactory<>("nomCommercial"));
        colPerDate.setCellValueFactory(new TreeItemPropertyValueFactory<>("datePeremption"));
        colPerStock.setCellValueFactory(new TreeItemPropertyValueFactory<>("stockActuel"));

        colStockPeremption.setCellFactory(column -> dateCell());
        colStockQteCmd.setCellFactory(column -> new TextFieldTreeTableCell<>(new IntegerStringConverter()) {
            @Override
            public void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                TreeItem<Medicament> treeItem = getTreeTableRow() == null ? null : getTreeTableRow().getTreeItem();
                if (empty || treeItem == null || !treeItem.isLeaf()) {
                    setText(empty ? null : "");
                    setEditable(false);
                } else {
                    setEditable(true);
                    if (!isEditing()) {
                        setText(item == null ? "0" : String.valueOf(item));
                    }
                }
            }
        });
        colStockQteCmd.setOnEditCommit(event -> {
            TreeItem<Medicament> treeItem = event.getRowValue();
            if (treeItem == null || !treeItem.isLeaf()) return;
            Integer value = event.getNewValue();
            if (value == null || value < 0) {
                treeItem.getValue().setQuantiteCommande(0);
            } else {
                treeItem.getValue().setQuantiteCommande(value);
            }
            tvAlertesStock.refresh();
        });
        colStockQteCmd.setEditable(true);
        colPerDate.setCellFactory(column -> dateCell());
        tvAlertesStock.setRoot(new TreeItem<>(new Medicament()));
        tvAlertesStock.setShowRoot(false);
        tvAlertesPeremption.setRoot(new TreeItem<>(new Medicament()));
        tvAlertesPeremption.setShowRoot(false);
        tvAlertesStock.setRowFactory(tv -> parentAwareRow(tvAlertesStock));
        tvAlertesPeremption.setRowFactory(tv -> parentAwareRow(tvAlertesPeremption));

        loadAlertes();
        applyRoleRestrictions();
    }

    private void applyRoleRestrictions() {
        var user = LoginController.getCurrentUser();
        boolean isPharmacien = user != null && "Pharmacien".equals(user.getRole());
        if (btnLancerCommande != null) {
            btnLancerCommande.setVisible(isPharmacien);
            btnLancerCommande.setManaged(isPharmacien);
        }
        if (colStockQteCmd != null) {
            colStockQteCmd.setVisible(isPharmacien);
            colStockQteCmd.setEditable(isPharmacien);
        }
        if (tvAlertesStock != null) {
            tvAlertesStock.setEditable(isPharmacien);
        }
    }

    private void loadAlertes() {
        try {
            rebuildTree(tvAlertesStock, servicesMedicament.getAlertesStock());
            rebuildTree(tvAlertesPeremption, servicesMedicament.getAlertesPeremption());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNewCommandeFromStock(ActionEvent event) {
        var user = LoginController.getCurrentUser();
        if (user == null || !"Pharmacien".equals(user.getRole())) {
            showError("Accès refusé", "Seul le Pharmacien peut lancer une commande fournisseur.");
            return;
        }
        try {
            List<Medicament> basStocks = collectLeafMedicaments(tvAlertesStock.getRoot());
            if (basStocks.isEmpty()) {
                showInfo("Info", "Aucun produit en stock bas.");
                return;
            }

            ObservableList<Fournisseur> fournisseurs = servicesFournisseur.getAllFournisseurs();
            if (fournisseurs.isEmpty()) {
                showError("Fournisseur manquant", "Ajoutez au moins un fournisseur avant de générer une commande.");
                return;
            }
            
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Commande Automatique");
            alert.setHeaderText("Générer un bon de commande ?");
            alert.setContentText("Voulez-vous générer une commande pour tous les produits en alerte stock ?\nLe fournisseur par défaut sera : "
                + fournisseurs.get(0).getNom());

            if (alert.showAndWait().get() == ButtonType.OK) {
                List<LigneCommande> lignes = new ArrayList<>();
                for (Medicament med : basStocks) {
                    if (med.getQuantiteCommande() > 0) {
                        lignes.add(new LigneCommande(0, med.getId(), med.getNomCommercial(), med.getQuantiteCommande(), med.getDatePeremption()));
                    }
                }
                if (lignes.isEmpty()) {
                    showInfo("Aucune commande", "Toutes les quantités sont à 0. Aucun réapprovisionnement lancé.");
                    return;
                }

                servicesCommande.creerCommande(fournisseurs.get(0).getId(), lignes);
                showInfo("Succès", "Commande générée automatiquement pour " + lignes.size() + " médicament(s).");
                loadAlertes();
            }
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    private List<Medicament> collectLeafMedicaments(TreeItem<Medicament> root) {
        List<Medicament> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        collectLeafMedicaments(root, result);
        return result;
    }

    private void collectLeafMedicaments(TreeItem<Medicament> node, List<Medicament> out) {
        if (node == null) {
            return;
        }
        if (node != tvAlertesStock.getRoot() && node.isLeaf() && node.getValue() != null) {
            out.add(node.getValue());
            return;
        }
        for (TreeItem<Medicament> child : node.getChildren()) {
            collectLeafMedicaments(child, out);
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

    private javafx.scene.control.TreeTableCell<Medicament, LocalDate> dateCell() {
        return new javafx.scene.control.TreeTableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        };
    }

    private void rebuildTree(TreeTableView<Medicament> table, ObservableList<Medicament> source) {
        TreeItem<Medicament> root = new TreeItem<>(new Medicament());
        Map<String, List<Medicament>> grouped = source.stream()
                .collect(Collectors.groupingBy(Medicament::getNomCommercial, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<Medicament>> entry : grouped.entrySet()) {
            List<Medicament> lots = entry.getValue().stream()
                    .sorted((a, b) -> a.getDatePeremption().compareTo(b.getDatePeremption()))
                    .toList();
            if (lots.size() == 1) {
                lots.get(0).setQuantiteCommande(0);
                root.getChildren().add(new TreeItem<>(lots.get(0)));
            } else {
                Medicament parent = new Medicament();
                parent.setNomCommercial(entry.getKey());
                parent.setStockActuel(lots.stream().mapToInt(Medicament::getStockActuel).sum());
                parent.setStockMinimum(lots.stream().mapToInt(Medicament::getStockMinimum).max().orElse(0));
                parent.setQuantiteCommande(0);

                TreeItem<Medicament> parentItem = new TreeItem<>(parent);
                lots.forEach(lot -> {
                    lot.setQuantiteCommande(0);
                    parentItem.getChildren().add(new TreeItem<>(lot));
                });
                parentItem.setExpanded(false);
                root.getChildren().add(parentItem);
            }
        }
        table.setRoot(root);
    }

    private TreeTableRow<Medicament> parentAwareRow(TreeTableView<Medicament> table) {
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
    }
}
