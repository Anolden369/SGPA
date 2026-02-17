package sgpa.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import sgpa.Entities.User;
import sgpa.Services.ServicesNotification;
import sgpa.Services.ServicesSettings;
import sgpa.Services.ServicesUser;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SettingsController {
    @FXML private Label lblUserInfo;
    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML private VBox adminMailPane;
    @FXML private Label lblMailInfo;
    @FXML private CheckBox chkMailEnabled;
    @FXML private TextField txtSmtpHost;
    @FXML private TextField txtSmtpPort;
    @FXML private TextField txtSmtpUser;
    @FXML private PasswordField txtSmtpPassword;
    @FXML private TextField txtSmtpFrom;
    @FXML private TextField txtSmtpRecipients;

    private final ServicesUser servicesUser = new ServicesUser();
    private final ServicesSettings servicesSettings = new ServicesSettings();
    private final ServicesNotification servicesNotification = new ServicesNotification();
    private User currentUser;

    public void initialize() {
        currentUser = LoginController.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        lblUserInfo.setText(currentUser.getPrenom() + " " + currentUser.getNom() + " - " + currentUser.getEmail());
        boolean isAdmin = "Pharmacien".equals(currentUser.getRole());
        adminMailPane.setManaged(isAdmin);
        adminMailPane.setVisible(isAdmin);
        lblMailInfo.setVisible(!isAdmin);
        lblMailInfo.setManaged(!isAdmin);

        if (isAdmin) {
            loadMailSettings();
        }
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        String current = txtCurrentPassword.getText();
        String next = txtNewPassword.getText();
        String confirm = txtConfirmPassword.getText();

        if (current.isBlank() || next.isBlank() || confirm.isBlank()) {
            showError("Champs manquants", "Renseignez tous les champs de mot de passe.");
            return;
        }
        if (!next.equals(confirm)) {
            showError("Confirmation invalide", "Le nouveau mot de passe et sa confirmation sont différents.");
            return;
        }
        if (next.length() < 8) {
            showError("Mot de passe faible", "Le nouveau mot de passe doit contenir au moins 8 caractères.");
            return;
        }

        try {
            User authenticated = servicesUser.verifLogin(currentUser.getEmail(), current);
            if (authenticated == null) {
                showError("Mot de passe incorrect", "Le mot de passe actuel est incorrect.");
                return;
            }
            servicesUser.updatePassword(currentUser.getId(), next);
            txtCurrentPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();
            showInfo("Succès", "Mot de passe modifié.");
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleSaveMailSettings(ActionEvent event) {
        try {
            Map<String, String> values = new HashMap<>();
            values.put("smtp_enabled", String.valueOf(chkMailEnabled.isSelected()));
            values.put("smtp_host", txtSmtpHost.getText());
            values.put("smtp_port", txtSmtpPort.getText().isBlank() ? "587" : txtSmtpPort.getText().trim());
            values.put("smtp_username", txtSmtpUser.getText());
            values.put("smtp_password", txtSmtpPassword.getText());
            values.put("smtp_from", txtSmtpFrom.getText());
            values.put("stock_alert_recipients", txtSmtpRecipients.getText());
            servicesSettings.putAll(values);
            showInfo("Succès", "Paramètres email enregistrés.");
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleTestMail(ActionEvent event) {
        try {
            handleSaveMailSettings(null);
            servicesNotification.sendTestMail();
            showInfo("Test envoyé", "Email de test envoyé avec succès.");
        } catch (Exception e) {
            showError("Erreur envoi", e.getMessage());
        }
    }

    private void loadMailSettings() {
        try {
            Map<String, String> values = servicesSettings.getAll();
            chkMailEnabled.setSelected("true".equalsIgnoreCase(values.getOrDefault("smtp_enabled", "false")) || "1".equals(values.get("smtp_enabled")));
            txtSmtpHost.setText(values.getOrDefault("smtp_host", ""));
            txtSmtpPort.setText(values.getOrDefault("smtp_port", "587"));
            txtSmtpUser.setText(values.getOrDefault("smtp_username", ""));
            txtSmtpPassword.setText(values.getOrDefault("smtp_password", ""));
            txtSmtpFrom.setText(values.getOrDefault("smtp_from", ""));
            txtSmtpRecipients.setText(values.getOrDefault("stock_alert_recipients", ""));
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
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
}
