package sgpa.Utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.concurrent.atomic.AtomicBoolean;

public final class InAppDialog {
    private InAppDialog() {
    }

    public static void info(Node anchor, String title, String message) {
        show(anchor, title, message, "inapp-dialog-info");
    }

    public static void success(Node anchor, String title, String message) {
        show(anchor, title, message, "inapp-dialog-success");
    }

    public static void error(Node anchor, String title, String message) {
        show(anchor, title, message, "inapp-dialog-error");
    }

    public static boolean confirm(Node anchor, String title, String message, String confirmLabel, String cancelLabel) {
        Pane root = resolveRoot(anchor);
        if (root == null) {
            return false;
        }

        AtomicBoolean confirmed = new AtomicBoolean(false);
        Object loopKey = new Object();

        StackPane overlay = createOverlay(root);
        VBox dialog = createDialog(title, message, "inapp-dialog-confirm");

        Button btnCancel = new Button(cancelLabel == null || cancelLabel.isBlank() ? "Annuler" : cancelLabel);
        btnCancel.getStyleClass().add("btn-ghost-soft");

        Button btnConfirm = new Button(confirmLabel == null || confirmLabel.isBlank() ? "Confirmer" : confirmLabel);
        btnConfirm.getStyleClass().add("btn-primary");

        HBox actions = new HBox(10, btnCancel, btnConfirm);
        actions.setAlignment(Pos.CENTER_RIGHT);
        dialog.getChildren().add(actions);

        btnCancel.setOnAction(e -> {
            removeOverlay(root, overlay);
            Platform.exitNestedEventLoop(loopKey, null);
        });
        btnConfirm.setOnAction(e -> {
            confirmed.set(true);
            removeOverlay(root, overlay);
            Platform.exitNestedEventLoop(loopKey, null);
        });

        overlay.getChildren().add(dialog);
        root.getChildren().add(overlay);
        Platform.enterNestedEventLoop(loopKey);
        return confirmed.get();
    }

    private static void show(Node anchor, String title, String message, String variantClass) {
        Pane root = resolveRoot(anchor);
        if (root == null) {
            return;
        }

        StackPane overlay = createOverlay(root);
        VBox dialog = createDialog(title, message, variantClass);

        Button btnOk = new Button("OK");
        btnOk.getStyleClass().add("btn-primary");
        HBox actions = new HBox(btnOk);
        actions.setAlignment(Pos.CENTER_RIGHT);
        dialog.getChildren().add(actions);

        btnOk.setOnAction(e -> removeOverlay(root, overlay));
        overlay.getChildren().add(dialog);
        root.getChildren().add(overlay);
    }

    private static VBox createDialog(String title, String message, String variantClass) {
        Label lblTitle = new Label(title == null || title.isBlank() ? "Information" : title);
        lblTitle.getStyleClass().add("inapp-dialog-title");

        Label lblMessage = new Label(message == null ? "" : message);
        lblMessage.getStyleClass().add("inapp-dialog-message");
        lblMessage.setWrapText(true);

        VBox dialog = new VBox(14, lblTitle, lblMessage);
        dialog.setPrefWidth(440);
        dialog.setMinWidth(360);
        dialog.setMaxWidth(520);
        dialog.setMinHeight(Region.USE_PREF_SIZE);
        dialog.setMaxHeight(Region.USE_PREF_SIZE);
        dialog.setPadding(new Insets(20));
        dialog.getStyleClass().addAll("inapp-dialog", variantClass);
        return dialog;
    }

    private static StackPane createOverlay(Pane root) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inapp-overlay");
        overlay.setManaged(false);
        overlay.setPickOnBounds(true);
        overlay.setLayoutX(0.0);
        overlay.setLayoutY(0.0);
        Scene scene = root.getScene();
        if (scene != null) {
            scene.widthProperty().addListener((obs, oldV, newV) -> {
                double w = newV == null ? 0.0 : newV.doubleValue();
                double h = scene.getHeight();
                overlay.resizeRelocate(0.0, 0.0, w, h);
            });
            scene.heightProperty().addListener((obs, oldV, newV) -> {
                double w = scene.getWidth();
                double h = newV == null ? 0.0 : newV.doubleValue();
                overlay.resizeRelocate(0.0, 0.0, w, h);
            });
            overlay.resizeRelocate(0.0, 0.0, scene.getWidth(), scene.getHeight());
        } else {
            root.widthProperty().addListener((obs, oldV, newV) -> {
                double w = newV == null ? 0.0 : newV.doubleValue();
                double h = root.getHeight();
                overlay.resizeRelocate(0.0, 0.0, w, h);
            });
            root.heightProperty().addListener((obs, oldV, newV) -> {
                double w = root.getWidth();
                double h = newV == null ? 0.0 : newV.doubleValue();
                overlay.resizeRelocate(0.0, 0.0, w, h);
            });
            overlay.resizeRelocate(0.0, 0.0, root.getWidth(), root.getHeight());
        }
        return overlay;
    }

    private static void removeOverlay(Pane root, StackPane overlay) {
        root.getChildren().remove(overlay);
    }

    private static Pane resolveRoot(Node anchor) {
        if (anchor == null) {
            return null;
        }
        Scene scene = anchor.getScene();
        if (scene == null || !(scene.getRoot() instanceof Pane paneRoot)) {
            return null;
        }
        return paneRoot;
    }
}
