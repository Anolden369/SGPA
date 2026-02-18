package sgpa;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sgpa.Controller.LoginController;
import sgpa.Services.ConnexionBDD;

import java.io.IOException;
import java.sql.SQLException;

public class SGPApplication extends Application {
    private static Stage mainStage;
    private static final String MAIN_STYLESHEET = resolveStylesheet();

    private static String resolveStylesheet() {
        var css = SGPApplication.class.getResource("/CSS/MesStyles.css");
        return css == null ? null : css.toExternalForm();
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            ConnexionBDD.connect();
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Erreur de connexion à la base de données : " + e.getMessage());
            LoginController.setStartupErrorMessage(
                    "Connexion à la base de données impossible au démarrage. Vérifiez MySQL et la configuration."
            );
        }

        mainStage = stage;
        var iconStream = SGPApplication.class.getResourceAsStream("/Images/pharmacy.png");
        if (iconStream != null) {
            mainStage.getIcons().add(new Image(iconStream));
        }
        showLoginScene();
    }

    public static void showLoginScene() throws IOException {
        FXMLLoader loader = new FXMLLoader(SGPApplication.class.getResource("View/login-view.fxml"));
        Parent root = loader.load();
        mainStage.setTitle("SGPA - Connexion");
        Scene scene = new Scene(root);
        if (MAIN_STYLESHEET != null) {
            scene.getStylesheets().add(MAIN_STYLESHEET);
        }
        mainStage.setFullScreen(false);
        mainStage.setMaximized(false);
        mainStage.setScene(scene);
        mainStage.setWidth(1180);
        mainStage.setHeight(760);
        mainStage.centerOnScreen();
        mainStage.show();
    }

    public static void showMainScene() throws IOException {
        FXMLLoader loader = new FXMLLoader(SGPApplication.class.getResource("View/main-view.fxml"));
        Parent root = loader.load();
        mainStage.setTitle("SGPA - Système de Gestion Pharmacie Avancé");
        Scene scene = new Scene(root);
        if (MAIN_STYLESHEET != null) {
            scene.getStylesheets().add(MAIN_STYLESHEET);
        }
        mainStage.setScene(scene);
        mainStage.setFullScreenExitHint("");
        mainStage.setFullScreen(true);
        mainStage.show();
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        launch(args);
    }
}
