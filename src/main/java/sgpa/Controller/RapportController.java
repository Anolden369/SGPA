package sgpa.Controller;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import sgpa.Entities.Vente;
import sgpa.SGPApplication;
import sgpa.Services.ServicesVente;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

public class RapportController {
    @FXML private Label lblTotalHT;
    @FXML private Label lblTotalTVA;
    @FXML private Label lblTotalTTC;
    @FXML private Label lblNbVentesMois;
    @FXML private Label lblCoutAchatHT;
    @FXML private Label lblMargeHT;
    @FXML private VBox cardRapHT;
    @FXML private VBox cardRapTVA;
    @FXML private VBox cardRapTTC;
    @FXML private VBox cardRapNb;
    @FXML private VBox cardRapCout;
    @FXML private VBox cardRapMarge;
    @FXML private BarChart<String, Number> chartVentes;
    @FXML private PieChart chartOrdonnance;
    @FXML private LineChart<String, Number> chartTopMedicaments;

    private ServicesVente servicesVente;
    private static final NumberFormat EURO_FORMAT = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    private static final DateTimeFormatter DATE_TIME_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void initialize() {
        servicesVente = new ServicesVente();
        loadData();
    }

    private void loadData() {
        try {
            double totalHT = servicesVente.getTotalHTMois();
            lblTotalHT.setText(formatEuro(totalHT));
            setTrendCard(cardRapHT, totalHT, servicesVente.getTotalHTMoisPrecedent());

            double totalTVA = servicesVente.getTotalTVAMois();
            lblTotalTVA.setText(formatEuro(totalTVA));
            setTrendCard(cardRapTVA, totalTVA, servicesVente.getTotalTVAMoisPrecedent());

            double totalTTC = servicesVente.getChiffreAffaireMois();
            lblTotalTTC.setText(formatEuro(totalTTC));
            setTrendCard(cardRapTTC, totalTTC, servicesVente.getChiffreAffaireMoisPrecedent());

            int nbVentes = servicesVente.getNbVentesMois();
            lblNbVentesMois.setText(String.valueOf(nbVentes));
            setTrendCard(cardRapNb, nbVentes, servicesVente.getNbVentesMoisPrecedent());

            double coutAchat = servicesVente.getCoutAchatMois();
            lblCoutAchatHT.setText(formatEuro(coutAchat));
            setTrendCard(cardRapCout, coutAchat, servicesVente.getCoutAchatMoisPrecedent());

            double margeHT = servicesVente.getMargeHTMois();
            lblMargeHT.setText(formatEuro(margeHT));
            setTrendCard(cardRapMarge, margeHT, servicesVente.getMargeHTMoisPrecedent());

            loadChartVentes12Mois();
            loadChartOrdonnance();
            loadChartTopMedicaments();
            installChartTooltips();
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les données de rapport: " + e.getMessage());
        }
    }

    private void loadChartVentes12Mois() throws SQLException {
        Map<YearMonth, Double> caParMois = servicesVente.getChiffreAffaires12DerniersMois();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("CA TTC");

        for (Map.Entry<YearMonth, Double> entry : caParMois.entrySet()) {
            YearMonth ym = entry.getKey();
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRANCE) + " " + ym.getYear();
            series.getData().add(new XYChart.Data<>(capitalize(label), entry.getValue()));
        }

        chartVentes.getData().clear();
        chartVentes.getData().add(series);
    }

    private void loadChartOrdonnance() throws SQLException {
        Map<String, Integer> data = servicesVente.getRepartitionOrdonnanceMoisCourant();
        chartOrdonnance.getData().clear();
        chartOrdonnance.getData().add(new PieChart.Data("Avec ordonnance", data.getOrDefault("Avec ordonnance", 0)));
        chartOrdonnance.getData().add(new PieChart.Data("Sans ordonnance", data.getOrDefault("Sans ordonnance", 0)));
    }

    private void loadChartTopMedicaments() throws SQLException {
        Map<String, Double> top = servicesVente.getTopMedicamentsTTCAnneeCourante(8);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("CA TTC");

        if (top.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Aucune vente", 0));
        } else {
            for (Map.Entry<String, Double> entry : top.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
        }

        chartTopMedicaments.getData().clear();
        chartTopMedicaments.getData().add(series);
    }

    private void installChartTooltips() {
        Platform.runLater(() -> {
            for (XYChart.Series<String, Number> series : chartVentes.getData()) {
                for (XYChart.Data<String, Number> point : series.getData()) {
                    if (point.getNode() != null) {
                        Tooltip.install(point.getNode(), new Tooltip(point.getXValue() + " : " + formatEuro(point.getYValue().doubleValue())));
                    }
                }
            }

            for (PieChart.Data slice : chartOrdonnance.getData()) {
                if (slice.getNode() != null) {
                    Tooltip.install(slice.getNode(), new Tooltip(slice.getName() + " : " + (int) slice.getPieValue() + " vente(s)"));
                }
            }

            for (XYChart.Series<String, Number> series : chartTopMedicaments.getData()) {
                for (XYChart.Data<String, Number> point : series.getData()) {
                    if (point.getNode() != null) {
                        Tooltip.install(point.getNode(), new Tooltip(point.getXValue() + " : " + formatEuro(point.getYValue().doubleValue())));
                    }
                }
            }
        });
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) return value;
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    @FXML
    private void handleExportPDF(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("Rapport_Financier_" + LocalDate.now() + ".pdf");
        File file = fileChooser.showSaveDialog(null);

        if (file == null) return;

        try {
            ObservableList<Vente> ventes = servicesVente.getHistoriqueVentes();
            generatePdfReport(file, ventes);
            showInfo("Export PDF", "Le rapport PDF a été exporté avec succès.");
        } catch (Exception e) {
            showError("Erreur Export PDF", e.getMessage());
        }
    }

    @FXML
    private void handleExportExcel(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("Rapport_Financier_" + LocalDate.now() + ".xlsx");
        File file = fileChooser.showSaveDialog(null);

        if (file == null) return;

        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(file)) {
            ObservableList<Vente> ventes = servicesVente.getHistoriqueVentes();
            generateExcelReport(workbook, ventes);
            workbook.write(fileOut);
            showInfo("Export Excel", "Le rapport Excel a été exporté avec succès.");
        } catch (Exception e) {
            showError("Erreur Export Excel", e.getMessage());
        }
    }

    private void generatePdfReport(File file, ObservableList<Vente> ventes) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, java.awt.Color.WHITE);
        Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new java.awt.Color(230, 248, 239));
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, new java.awt.Color(20, 94, 76));
        Font normalFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new java.awt.Color(35, 56, 50));
        Font boldFont = new Font(Font.HELVETICA, 9, Font.BOLD, new java.awt.Color(24, 66, 54));

        PdfPTable header = new PdfPTable(new float[]{1.1f, 3.3f, 2.1f});
        header.setWidthPercentage(100);
        header.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setBackgroundColor(new java.awt.Color(20, 105, 82));
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        var logoStream = SGPApplication.class.getResourceAsStream("/Images/pharmacy.png");
        if (logoStream != null) {
            byte[] bytes = logoStream.readAllBytes();
            Image logo = Image.getInstance(bytes);
            logo.scaleToFit(60, 60);
            logoCell.addElement(logo);
        }
        header.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setBackgroundColor(new java.awt.Color(20, 105, 82));
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.addElement(new Paragraph("SGPA - Rapport Financier", titleFont));
        titleCell.addElement(new Paragraph("Pharmacie SGPA - 12 Avenue Victor Hugo, 75016 Paris", subtitleFont));
        header.addCell(titleCell);

        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.setBackgroundColor(new java.awt.Color(20, 105, 82));
        metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(new Paragraph("Date: " + LocalDate.now().format(DATE_FR), subtitleFont));
        metaCell.addElement(new Paragraph("Heure: " + LocalDateTime.now().format(DATE_TIME_FR), subtitleFont));
        metaCell.addElement(new Paragraph("Période: Mois courant", subtitleFont));
        header.addCell(metaCell);

        document.add(header);
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Synthèse financière", sectionFont));
        PdfPTable summaryTable = new PdfPTable(6);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(8f);
        summaryTable.setWidths(new float[]{2.1f, 1.8f, 1.8f, 1.5f, 1.8f, 1.8f});
        addSummaryCell(summaryTable, "Chiffre d'affaires HT", lblTotalHT.getText(), boldFont, normalFont);
        addSummaryCell(summaryTable, "Total TVA", lblTotalTVA.getText(), boldFont, normalFont);
        addSummaryCell(summaryTable, "Chiffre d'affaires TTC", lblTotalTTC.getText(), boldFont, normalFont);
        addSummaryCell(summaryTable, "Nombre de ventes", lblNbVentesMois.getText(), boldFont, normalFont);
        addSummaryCell(summaryTable, "Coût d'achat HT", lblCoutAchatHT.getText(), boldFont, normalFont);
        addSummaryCell(summaryTable, "Marge HT réalisée", lblMargeHT.getText(), boldFont, normalFont);
        document.add(summaryTable);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Détail des ventes", sectionFont));

        PdfPTable detailsTable = new PdfPTable(new float[]{0.9f, 1.8f, 1.2f, 1.2f, 1.2f, 1.4f, 1.1f});
        detailsTable.setWidthPercentage(100);
        detailsTable.setSpacingBefore(8f);
        addHeaderCell(detailsTable, "Référence", boldFont);
        addHeaderCell(detailsTable, "Date / Heure", boldFont);
        addHeaderCell(detailsTable, "HT", boldFont);
        addHeaderCell(detailsTable, "TVA", boldFont);
        addHeaderCell(detailsTable, "TTC", boldFont);
        addHeaderCell(detailsTable, "Vendeur", boldFont);
        addHeaderCell(detailsTable, "Ordonnance", boldFont);

        int rowIndex = 0;
        for (Vente vente : ventes) {
            java.awt.Color rowColor = rowIndex % 2 == 0 ? new java.awt.Color(248, 252, 250) : new java.awt.Color(238, 247, 242);
            addDetailCell(detailsTable, vente.getReference(), normalFont, Element.ALIGN_CENTER, rowColor);
            addDetailCell(detailsTable, vente.getDateHeure().format(DATE_TIME_FR), normalFont, Element.ALIGN_LEFT, rowColor);
            addDetailCell(detailsTable, formatEuro(vente.getMontantHT()), normalFont, Element.ALIGN_RIGHT, rowColor);
            addDetailCell(detailsTable, formatEuro(vente.getMontantTVA()), normalFont, Element.ALIGN_RIGHT, rowColor);
            addDetailCell(detailsTable, formatEuro(vente.getMontantTTC()), normalFont, Element.ALIGN_RIGHT, rowColor);
            addDetailCell(detailsTable, vente.getNomVendeur(), normalFont, Element.ALIGN_LEFT, rowColor);
            addDetailCell(detailsTable, vente.isSurOrdonnance() ? "Oui" : "Non", normalFont, Element.ALIGN_CENTER, rowColor);
            rowIndex++;
        }

        if (!ventes.isEmpty()) {
            double totalHt = ventes.stream().mapToDouble(Vente::getMontantHT).sum();
            double totalTva = ventes.stream().mapToDouble(Vente::getMontantTVA).sum();
            double totalTtc = ventes.stream().mapToDouble(Vente::getMontantTTC).sum();
            addFooterTotalRow(detailsTable, totalHt, totalTva, totalTtc, boldFont);
        }

        document.add(detailsTable);
        document.add(new Paragraph(" "));
        Paragraph footer = new Paragraph("Document généré automatiquement par SGPA.", subtitleFont);
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);
        document.close();
    }

    private void generateExcelReport(Workbook workbook, ObservableList<Vente> ventes) {
        Sheet sheet = workbook.createSheet("Rapport Financier");

        XSSFCellStyle titleStyle = (XSSFCellStyle) workbook.createCellStyle();
        titleStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setBorderBottom(BorderStyle.THIN);
        titleStyle.setBorderTop(BorderStyle.THIN);
        titleStyle.setBorderLeft(BorderStyle.THIN);
        titleStyle.setBorderRight(BorderStyle.THIN);
        XSSFFont titleFont = (XSSFFont) workbook.createFont();
        titleFont.setBold(true);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        XSSFFont headerFont = (XSSFFont) workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);

        XSSFCellStyle cellStyle = (XSSFCellStyle) workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setAlignment(HorizontalAlignment.LEFT);

        XSSFCellStyle cellStyleAlt = (XSSFCellStyle) workbook.createCellStyle();
        cellStyleAlt.cloneStyleFrom(cellStyle);
        cellStyleAlt.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        cellStyleAlt.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFCellStyle currencyStyle = (XSSFCellStyle) workbook.createCellStyle();
        currencyStyle.cloneStyleFrom(cellStyle);
        currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00 [$€-fr-FR]"));

        XSSFCellStyle currencyStyleAlt = (XSSFCellStyle) workbook.createCellStyle();
        currencyStyleAlt.cloneStyleFrom(cellStyleAlt);
        currencyStyleAlt.setAlignment(HorizontalAlignment.RIGHT);
        currencyStyleAlt.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00 [$€-fr-FR]"));

        XSSFCellStyle dateStyle = (XSSFCellStyle) workbook.createCellStyle();
        dateStyle.cloneStyleFrom(cellStyle);
        dateStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle dateStyleAlt = (XSSFCellStyle) workbook.createCellStyle();
        dateStyleAlt.cloneStyleFrom(cellStyleAlt);
        dateStyleAlt.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle summaryLabelStyle = (XSSFCellStyle) workbook.createCellStyle();
        summaryLabelStyle.cloneStyleFrom(cellStyle);
        summaryLabelStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        summaryLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont summaryLabelFont = (XSSFFont) workbook.createFont();
        summaryLabelFont.setBold(true);
        summaryLabelStyle.setFont(summaryLabelFont);

        XSSFCellStyle summaryValueStyle = (XSSFCellStyle) workbook.createCellStyle();
        summaryValueStyle.cloneStyleFrom(currencyStyle);
        XSSFFont summaryValueFont = (XSSFFont) workbook.createFont();
        summaryValueFont.setBold(true);
        summaryValueStyle.setFont(summaryValueFont);

        Row row0 = sheet.createRow(0);
        createCell(row0, 0, "SGPA - Rapport Financier", titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        Row row1 = sheet.createRow(1);
        createCell(row1, 0, "Pharmacie SGPA - 12 Avenue Victor Hugo, 75016 Paris", cellStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));
        createCell(row1, 4, "Date d'export : " + LocalDateTime.now().format(DATE_TIME_FR), cellStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 4, 7));

        Row row2 = sheet.createRow(2);
        createCell(row2, 0, "Période : Mois courant", cellStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 7));

        Row row4 = sheet.createRow(4);
        createCell(row4, 0, "Synthèse KPI", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 7));

        Row row5 = sheet.createRow(5);
        createCell(row5, 0, "Chiffre d'affaires HT", summaryLabelStyle);
        createCell(row5, 1, parseEuroLabel(lblTotalHT.getText()), summaryValueStyle);
        createCell(row5, 2, "Total TVA", summaryLabelStyle);
        createCell(row5, 3, parseEuroLabel(lblTotalTVA.getText()), summaryValueStyle);
        createCell(row5, 4, "Chiffre d'affaires TTC", summaryLabelStyle);
        createCell(row5, 5, parseEuroLabel(lblTotalTTC.getText()), summaryValueStyle);
        createCell(row5, 6, "Nombre de ventes", summaryLabelStyle);
        createCell(row5, 7, Integer.parseInt(lblNbVentesMois.getText()), summaryValueStyle);

        Row row6 = sheet.createRow(6);
        createCell(row6, 0, "Coût d'achat HT", summaryLabelStyle);
        createCell(row6, 1, parseEuroLabel(lblCoutAchatHT.getText()), summaryValueStyle);
        createCell(row6, 2, "Marge HT réalisée", summaryLabelStyle);
        createCell(row6, 3, parseEuroLabel(lblMargeHT.getText()), summaryValueStyle);

        int headerRowIndex = 8;
        Row headerRow = sheet.createRow(headerRowIndex);
        String[] headers = {"Référence", "Date / Heure", "Montant HT", "Montant TVA", "Montant TTC", "Vendeur", "Ordonnance"};
        for (int i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], headerStyle);
        }

        int rowNum = headerRowIndex + 1;
        boolean odd = false;
        for (Vente vente : ventes) {
            Row row = sheet.createRow(rowNum++);
            CellStyle textStyle = odd ? cellStyleAlt : cellStyle;
            CellStyle moneyStyle = odd ? currencyStyleAlt : currencyStyle;
            CellStyle dtStyle = odd ? dateStyleAlt : dateStyle;

            createCell(row, 0, vente.getReference(), textStyle);
            Cell dateCell = row.createCell(1);
            dateCell.setCellValue(vente.getDateHeure().format(DATE_TIME_FR));
            dateCell.setCellStyle(dtStyle);

            createCell(row, 2, vente.getMontantHT(), moneyStyle);
            createCell(row, 3, vente.getMontantTVA(), moneyStyle);
            createCell(row, 4, vente.getMontantTTC(), moneyStyle);
            createCell(row, 5, vente.getNomVendeur(), textStyle);
            createCell(row, 6, vente.isSurOrdonnance() ? "Oui" : "Non", textStyle);
            odd = !odd;
        }

        if (!ventes.isEmpty()) {
            double totalHt = ventes.stream().mapToDouble(Vente::getMontantHT).sum();
            double totalTva = ventes.stream().mapToDouble(Vente::getMontantTVA).sum();
            double totalTtc = ventes.stream().mapToDouble(Vente::getMontantTTC).sum();

            Row totalRow = sheet.createRow(rowNum + 1);
            createCell(totalRow, 1, "TOTAL", headerStyle);
            createCell(totalRow, 2, totalHt, summaryValueStyle);
            createCell(totalRow, 3, totalTva, summaryValueStyle);
            createCell(totalRow, 4, totalTtc, summaryValueStyle);
        }

        sheet.createFreezePane(0, headerRowIndex + 1);
        sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, headerRowIndex, 0, headers.length - 1));

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1100, 17000));
        }
    }

    private void addSummaryCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8f);
        cell.setBorderColor(new java.awt.Color(201, 225, 214));
        cell.setBackgroundColor(new java.awt.Color(244, 251, 247));
        Paragraph pLabel = new Paragraph(label, labelFont);
        Paragraph pValue = new Paragraph(value, valueFont);
        pValue.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(pLabel);
        cell.addElement(pValue);
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 9, Font.BOLD, java.awt.Color.WHITE)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBackgroundColor(new java.awt.Color(28, 116, 89));
        cell.setBorderColor(new java.awt.Color(184, 215, 201));
        table.addCell(cell);
    }

    private void addDetailCell(PdfPTable table, String value, Font font, int align) {
        addDetailCell(table, value, font, align, null);
    }

    private void addDetailCell(PdfPTable table, String value, Font font, int align, java.awt.Color rowColor) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        if (rowColor != null) {
            cell.setBackgroundColor(rowColor);
        }
        cell.setBorderColor(new java.awt.Color(216, 231, 223));
        table.addCell(cell);
    }

    private void addFooterTotalRow(PdfPTable table, double totalHt, double totalTva, double totalTtc, Font boldFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase("TOTAL", new Font(Font.HELVETICA, 9, Font.BOLD, java.awt.Color.WHITE)));
        labelCell.setColspan(2);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(6f);
        labelCell.setBackgroundColor(new java.awt.Color(23, 106, 82));
        labelCell.setBorderColor(new java.awt.Color(182, 213, 200));
        table.addCell(labelCell);

        java.awt.Color totalBg = new java.awt.Color(236, 247, 241);
        addDetailCell(table, formatEuro(totalHt), boldFont, Element.ALIGN_RIGHT, totalBg);
        addDetailCell(table, formatEuro(totalTva), boldFont, Element.ALIGN_RIGHT, totalBg);
        addDetailCell(table, formatEuro(totalTtc), boldFont, Element.ALIGN_RIGHT, totalBg);
        addDetailCell(table, "-", boldFont, Element.ALIGN_CENTER, totalBg);
        addDetailCell(table, "-", boldFont, Element.ALIGN_CENTER, totalBg);
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int column, int value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String formatEuro(double amount) {
        return EURO_FORMAT.format(amount);
    }

    private double parseEuroLabel(String label) {
        String cleaned = label.replace("€", "").replace("\u00A0", "").replace(" ", "").replace(",", ".");
        return Double.parseDouble(cleaned);
    }

    private void setTrendCard(VBox card, double current, double previous) {
        if (card == null) return;
        card.getStyleClass().removeAll("kpi-positive", "kpi-stable", "kpi-negative");
        if (Math.abs(current - previous) < 0.01) {
            card.getStyleClass().add("kpi-stable");
        } else if (current > previous) {
            card.getStyleClass().add("kpi-positive");
        } else {
            card.getStyleClass().add("kpi-negative");
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
