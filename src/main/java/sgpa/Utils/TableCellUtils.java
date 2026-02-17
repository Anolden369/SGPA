package sgpa.Utils;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import javafx.util.Callback;

public final class TableCellUtils {
    private TableCellUtils() {
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> tooltipIfTruncated() {
        return column -> new TableCell<>() {
            private final Tooltip tooltip = new Tooltip();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setTooltip(null);
                    return;
                }

                setText(item);

                Text probe = new Text(item);
                probe.setFont(getFont());
                double textWidth = probe.getLayoutBounds().getWidth();
                double availableWidth = getTableColumn() == null ? 0 : getTableColumn().getWidth() - 20;

                if (textWidth > availableWidth) {
                    tooltip.setText(item);
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(520);
                    setTooltip(tooltip);
                } else {
                    setTooltip(null);
                }
            }
        };
    }

    public static <S> Callback<TableColumn<S, Boolean>, TableCell<S, Boolean>> booleanOuiNonCell() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Oui" : "Non");
                }
            }
        };
    }
}
