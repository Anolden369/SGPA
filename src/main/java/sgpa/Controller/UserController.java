package sgpa.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import sgpa.Entities.User;
import sgpa.SGPApplication;
import sgpa.Services.ServicesUser;
import sgpa.Utils.TableCellUtils;
import sgpa.Utils.TableViewUtils;

import java.io.IOException;
import java.sql.SQLException;

public class UserController {
    @FXML private TableView<User> tvUsers;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;

    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbRole;

    private ServicesUser servicesUser;

    public void initialize() {
        servicesUser = new ServicesUser();
        cbRole.getItems().addAll("Pharmacien", "Preparateur/Vendeur");

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colNom.setCellFactory(TableCellUtils.tooltipIfTruncated());
        colPrenom.setCellFactory(TableCellUtils.tooltipIfTruncated());
        colEmail.setCellFactory(TableCellUtils.tooltipIfTruncated());
        colRole.setCellFactory(TableCellUtils.tooltipIfTruncated());
        TableViewUtils.applyConstrainedResize(tvUsers);

        loadUsers();

        tvUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillFields(newSelection);
            }
        });
    }

    private void loadUsers() {
        try {
            tvUsers.setItems(servicesUser.getAllUsers());
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    private void fillFields(User u) {
        txtNom.setText(u.getNom());
        txtPrenom.setText(u.getPrenom());
        txtEmail.setText(u.getEmail());
        cbRole.setValue(u.getRole());
        txtPassword.clear();
    }

    @FXML
    private void handleAdd(ActionEvent event) {
        if (txtNom.getText().isEmpty() || txtPrenom.getText().isEmpty() || txtEmail.getText().isEmpty() || txtPassword.getText().isEmpty() || cbRole.getValue() == null) {
            showError("Champs manquants", "Veuillez remplir tous les champs, y compris le mot de passe.");
            return;
        }
        try {
            User u = new User(0, txtNom.getText(), txtPrenom.getText(), txtEmail.getText(), cbRole.getValue());
            int idRole = cbRole.getValue().equals("Pharmacien") ? 1 : 2;
            servicesUser.addUser(u, txtPassword.getText(), idRole);
            loadUsers();
            handleClear(null);
        } catch (SQLException e) {
            showError("Erreur d'ajout", e.getMessage());
        }
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        User selected = tvUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection", "Veuillez sélectionner un utilisateur à modifier.");
            return;
        }
        if (txtNom.getText().isEmpty() || txtPrenom.getText().isEmpty() || txtEmail.getText().isEmpty() || cbRole.getValue() == null) {
            showError("Champs manquants", "Veuillez remplir tous les champs obligatoires (le mot de passe peut être laissé vide pour conserver l'ancien).");
            return;
        }
        try {
            User currentUser = LoginController.getCurrentUser();
            boolean isSelf = currentUser != null && currentUser.getId() == selected.getId();
            String previousRole = selected.getRole();
            String newRole = cbRole.getValue();
            boolean selfRoleChanged = isSelf && previousRole != null && !previousRole.equals(newRole);

            User u = new User(selected.getId(), txtNom.getText(), txtPrenom.getText(), txtEmail.getText(), cbRole.getValue());
            int idRole = cbRole.getValue().equals("Pharmacien") ? 1 : 2;
            servicesUser.updateUser(u, idRole);
            if (!txtPassword.getText().isEmpty()) {
                servicesUser.updatePassword(selected.getId(), txtPassword.getText());
            }
            loadUsers();
            txtPassword.clear();

            if (selfRoleChanged) {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Rôle modifié");
                info.setHeaderText(null);
                info.setContentText("Votre rôle a été modifié. Vous allez être déconnecté pour recharger vos permissions.");
                info.showAndWait();

                LoginController.clearCurrentUser();
                SGPApplication.showLoginScene();
            }
        } catch (SQLException e) {
            showError("Erreur de modification", e.getMessage());
        } catch (IOException e) {
            showError("Erreur de navigation", e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        User selected = tvUsers.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            servicesUser.deleteUser(selected.getId());
            loadUsers();
            handleClear(null);
        } catch (SQLException e) {
            showError("Erreur de suppression", e.getMessage());
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        txtNom.clear();
        txtPrenom.clear();
        txtEmail.clear();
        txtPassword.clear();
        cbRole.getSelectionModel().clearSelection();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
