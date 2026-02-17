package sgpa;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sgpa.Services.ConnexionBDD;

import java.io.IOException;
import java.sql.SQLException;

public class SGPApplication extends Application {
    private static Stage mainStage;
    private static final String MAIN_STYLESHEET = SGPApplication.class.getResource("/CSS/MesStyles.css").toExternalForm();

    @Override
    public void start(Stage stage) throws Exception {
        try {
            ConnexionBDD.connect();
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Erreur de connexion à la base de données : " + e.getMessage());
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
        scene.getStylesheets().add(MAIN_STYLESHEET);
        mainStage.setScene(scene);
        mainStage.show();
    }

    public static void showMainScene() throws IOException {
        FXMLLoader loader = new FXMLLoader(SGPApplication.class.getResource("View/main-view.fxml"));
        Parent root = loader.load();
        mainStage.setTitle("SGPA - Système de Gestion Pharmacie Avancé");
        Scene scene = new Scene(root);
        scene.getStylesheets().add(MAIN_STYLESHEET);
        mainStage.setScene(scene);
        mainStage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch();
    }
}
