package sgpa.Controller;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import sgpa.Entities.User;
import sgpa.SGPApplication;
import sgpa.Services.ServicesUser;
import sgpa.Utils.InAppDialog;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Button btnLogin;
    @FXML
    private Label lblError;
    @FXML
    private ImageView imgLoginVisual;
    @FXML
    private VBox heroPane;
    @FXML
    private VBox cardPane;

    private ServicesUser servicesUser;
    private static User currentUser;
    private static String startupErrorMessage;

    public void initialize() {
        servicesUser = new ServicesUser();
        var stream = SGPApplication.class.getResourceAsStream("/Images/pharmacy.png");
        if (stream != null) {
            imgLoginVisual.setImage(new Image(stream, 0, 0, true, true));
        }

        playEntranceAnimations();
        playStartupBounce();

        if (startupErrorMessage != null && !startupErrorMessage.isBlank()) {
            String message = startupErrorMessage;
            startupErrorMessage = null;
            lblError.setText(message);
            Platform.runLater(() -> InAppDialog.error(cardPane, "Erreur de connexion BDD", message));
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        if (email.isEmpty() || password.isEmpty()) {
            lblError.setText("Veuillez remplir tous les champs.");
            return;
        }

        try {
            User user = servicesUser.verifLogin(email, password);
            if (user != null) {
                currentUser = user;
                SGPApplication.showMainScene();
            } else {
                lblError.setText("Email ou mot de passe incorrect.");
            }
        } catch (SQLException | IOException e) {
            lblError.setText("Erreur technique : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clearCurrentUser() {
        currentUser = null;
    }

    public static void setStartupErrorMessage(String message) {
        startupErrorMessage = message;
    }

    private void playEntranceAnimations() {
        heroPane.setOpacity(0);
        heroPane.setTranslateX(-28);
        cardPane.setOpacity(0);
        cardPane.setTranslateX(28);

        FadeTransition heroFade = new FadeTransition(Duration.millis(220), heroPane);
        heroFade.setFromValue(0);
        heroFade.setToValue(1);
        TranslateTransition heroSlide = new TranslateTransition(Duration.millis(240), heroPane);
        heroSlide.setFromX(-28);
        heroSlide.setToX(0);
        heroSlide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition cardFade = new FadeTransition(Duration.millis(240), cardPane);
        cardFade.setFromValue(0);
        cardFade.setToValue(1);
        TranslateTransition cardSlide = new TranslateTransition(Duration.millis(260), cardPane);
        cardSlide.setFromX(28);
        cardSlide.setToX(0);
        cardSlide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(heroFade, heroSlide, cardFade, cardSlide).play();
    }

    private void playStartupBounce() {
        TranslateTransition imgBounceY = new TranslateTransition(Duration.millis(150), imgLoginVisual);
        imgBounceY.setFromY(0);
        imgBounceY.setToY(-4);
        imgBounceY.setAutoReverse(true);
        imgBounceY.setCycleCount(4); // 2 rebonds au chargement
        imgBounceY.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition btnBounceY = new TranslateTransition(Duration.millis(150), btnLogin);
        btnBounceY.setFromY(0);
        btnBounceY.setToY(-2);
        btnBounceY.setAutoReverse(true);
        btnBounceY.setCycleCount(4);
        btnBounceY.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition startupBounce = new ParallelTransition(imgBounceY, btnBounceY);
        startupBounce.setDelay(Duration.millis(80));
        startupBounce.play();
    }
}
