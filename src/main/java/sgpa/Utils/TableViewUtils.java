package sgpa.Utils;

import javafx.scene.control.TableView;

public final class TableViewUtils {
    private TableViewUtils() {
    }

    public static void applyConstrainedResize(TableView<?>... tables) {
        for (TableView<?> table : tables) {
            if (table != null) {
                table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            }
        }
    }
}
