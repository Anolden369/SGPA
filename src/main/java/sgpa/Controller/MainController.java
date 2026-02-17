package sgpa.Controller;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import sgpa.Entities.User;
import sgpa.SGPApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController {
    @FXML
    private Label lblWelcome;
    @FXML
    private Label lblStatus;
    @FXML
    private StackPane contentPane;
    @FXML
    private VBox dashboardPane;
    @FXML
    private Button btnMedicaments;
    @FXML
    private Button btnFournisseurs;
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnVentes;
    @FXML
    private Button btnStocks;
    @FXML
    private Button btnCommandes;
    @FXML
    private Button btnUsers;
    @FXML
    private Button btnRapports;
    @FXML
    private Button btnML;
    @FXML
    private Button btnSettings;
    @FXML
    private Button btnLogout;

    private User currentUser;

    public void initialize() {
        setupMenuVisuals();
        currentUser = LoginController.getCurrentUser();
        if (currentUser != null) {
            lblWelcome.setText("Bienvenue, " + currentUser.getPrenom() + " " + currentUser.getNom());
            lblStatus.setText("Connecté en tant que : " + currentUser.getEmail() + " (" + currentUser.getRole() + ")");
            
            // Restriction des droits selon le rôle
            if (currentUser.getRole().equals("Preparateur/Vendeur")) {
                hideButton(btnMedicaments);
                hideButton(btnFournisseurs);
                hideButton(btnCommandes);
                hideButton(btnUsers);
                hideButton(btnRapports);
                hideButton(btnML);
            }
        }
        showDashboard();
    }

    private void hideButton(Button button) {
        button.setVisible(false);
        button.setManaged(false);
    }

    private void setupMenuVisuals() {
        setButtonSvgIcon(btnDashboard, "/Images/nav-home.svg");
        setButtonSvgIcon(btnMedicaments, "/Images/nav-medicament.svg");
        setButtonSvgIcon(btnVentes, "/Images/nav-vente.svg");
        setButtonImageIcon(btnStocks, "/Images/nav-stock.png");
        setButtonSvgIcon(btnFournisseurs, "/Images/nav-fournisseur.svg");
        setButtonSvgIcon(btnCommandes, "/Images/nav-commande.svg");
        setButtonSvgIcon(btnUsers, "/Images/nav-users.svg");
        setButtonImageIcon(btnRapports, "/Images/nav-rapport.png");
        setButtonImageIcon(btnML, "/Images/nav-rapport.png");
        setButtonImageIcon(btnSettings, "/Images/nav-settings.png");
        setButtonSvgIcon(btnLogout, "/Images/nav-logout.svg");

        List<Button> menuButtons = List.of(
            btnDashboard, btnMedicaments, btnVentes, btnStocks,
            btnFournisseurs, btnCommandes, btnUsers, btnRapports, btnML, btnSettings, btnLogout
        );
        for (Button button : menuButtons) {
            addHoverAnimation(button);
        }
    }

    private void setButtonSvgIcon(Button button, String resourcePath) {
        if (button == null) return;
        Group iconGroup = buildSvgIcon(resourcePath);
        if (iconGroup == null) return;
        button.setGraphic(iconGroup);
        button.setGraphicTextGap(8);
    }

    private void setButtonImageIcon(Button button, String resourcePath) {
        if (button == null) return;
        try (InputStream stream = SGPApplication.class.getResourceAsStream(resourcePath)) {
            if (stream == null) return;
            ImageView icon = new ImageView(new Image(stream));
            icon.setFitWidth(18);
            icon.setFitHeight(18);
            icon.setPreserveRatio(true);
            button.setGraphic(icon);
            button.setGraphicTextGap(8);
        } catch (IOException ignored) {
        }
    }

    private Group buildSvgIcon(String resourcePath) {
        try (InputStream stream = SGPApplication.class.getResourceAsStream(resourcePath)) {
            if (stream == null) return null;

            StringBuilder svg = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    svg.append(line);
                }
            }

            Pattern pathPattern = Pattern.compile("d=\"([^\"]+)\"");
            Matcher matcher = pathPattern.matcher(svg.toString());

            Group group = new Group();
            while (matcher.find()) {
                SVGPath path = new SVGPath();
                path.setContent(matcher.group(1));
                path.setFill(Color.web("#F3FBFF"));
                path.setStroke(Color.web("#D6F1FF"));
                path.setStrokeWidth(0.45);
                group.getChildren().add(path);
            }
            group.setScaleX(0.78);
            group.setScaleY(0.78);
            return group.getChildren().isEmpty() ? null : group;
        } catch (IOException e) {
            return null;
        }
    }

    private void addHoverAnimation(Button button) {
        if (button == null) return;
        ScaleTransition enlarge = new ScaleTransition(Duration.millis(140), button);
        enlarge.setToX(1.02);
        enlarge.setToY(1.02);

        ScaleTransition reset = new ScaleTransition(Duration.millis(140), button);
        reset.setToX(1.0);
        reset.setToY(1.0);

        button.setOnMouseEntered(event -> enlarge.playFromStart());
        button.setOnMouseExited(event -> reset.playFromStart());
    }

    private void revealNode(Region region) {
        if (region == null) return;
        region.setOpacity(0);
        FadeTransition transition = new FadeTransition(Duration.millis(250), region);
        transition.setFromValue(0);
        transition.setToValue(1);
        transition.play();
    }

    private void revealNode(Parent view) {
        if (view == null) return;
        view.setOpacity(0);
        FadeTransition transition = new FadeTransition(Duration.millis(240), view);
        transition.setFromValue(0);
        transition.setToValue(1);
        transition.play();
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        showDashboard();
    }

    private void showDashboard() {
        loadView("dashboard-view.fxml");
    }

    @FXML
    private void handleMedicaments(ActionEvent event) {
        loadView("medicament-view.fxml");
    }

    private void loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(SGPApplication.class.getResource("View/" + fxml));
            Parent view = loader.load();
            contentPane.getChildren().clear();
            contentPane.getChildren().add(view);
            revealNode(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleStocks(ActionEvent event) {
        loadView("stock-view.fxml");
    }

    @FXML
    private void handleFournisseurs(ActionEvent event) {
        loadView("fournisseur-view.fxml");
    }

    @FXML
    private void handleCommandes(ActionEvent event) {
        loadView("commande-view.fxml");
    }

    @FXML
    private void handleVentes(ActionEvent event) {
        loadView("vente-view.fxml");
    }

    @FXML
    private void handleUsers(ActionEvent event) {
        loadView("user-view.fxml");
    }

    @FXML
    private void handleRapports(ActionEvent event) {
        loadView("rapport-view.fxml");
    }

    @FXML
    private void handleML(ActionEvent event) {
        if (currentUser == null || !"Pharmacien".equals(currentUser.getRole())) {
            return;
        }
        loadView("ml-view.fxml");
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        loadView("settings-view.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        LoginController.clearCurrentUser();
        SGPApplication.showLoginScene();
    }
}
