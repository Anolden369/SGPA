package sgpa.Services;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import sgpa.Entities.Client;
import sgpa.Entities.LigneVente;
import sgpa.Entities.Vente;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ServicesDocumentVente {
    private static final DateTimeFormatter DATE_TIME_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String PHARMACY_NAME = "Pharmacie SGPA";
    private static final String PHARMACY_ADDRESS_1 = "24 Avenue Victor Hugo";
    private static final String PHARMACY_ADDRESS_2 = "75016 Paris";
    private static final String PHARMACY_PHONE = "01 44 00 16 16";

    public void generateDevisPdf(File file, Client clientInfo, List<LigneVente> lignes) throws Exception {
        String number = "DEV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        generateDocumentPdf(file, "DEVIS", number, null, null, clientInfo, lignes);
    }

    public void generateFacturePdf(File file, Client clientInfo, Vente vente, List<LigneVente> lignes) throws Exception {
        String number = vente == null ? "FAC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                : vente.getReference().replace("VTE-", "FAC-");
        generateDocumentPdf(file, "FACTURE", number, vente, LoginControllerSafe.currentUserDisplay(), clientInfo, lignes);
    }

    private void generateDocumentPdf(File file, String docType, String docNumber, Vente vente, String vendeurName,
                                     Client clientInfo, List<LigneVente> lignes) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD);
        Font sectionFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);

        PdfPTable top = new PdfPTable(new float[]{2.4f, 2.0f});
        top.setWidthPercentage(100);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph(PHARMACY_NAME, sectionFont));
        left.addElement(new Paragraph(PHARMACY_ADDRESS_1, normalFont));
        left.addElement(new Paragraph(PHARMACY_ADDRESS_2, normalFont));
        left.addElement(new Paragraph("Tel: " + PHARMACY_PHONE, smallFont));
        top.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph docTitle = new Paragraph(docType, titleFont);
        docTitle.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(docTitle);
        right.addElement(new Paragraph("Numero: " + docNumber, normalFont));
        right.addElement(new Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont));
        if (vente != null && vente.getDateHeure() != null) {
            right.addElement(new Paragraph("Vente: " + vente.getDateHeure().format(DATE_TIME_FR), normalFont));
        }
        top.addCell(right);
        document.add(top);

        document.add(new Paragraph(" "));

        PdfPTable recipient = new PdfPTable(1);
        recipient.setWidthPercentage(100);
        PdfPCell rec = new PdfPCell();
        rec.setBorderColor(new java.awt.Color(220, 228, 236));
        rec.setPadding(10f);
        rec.setBackgroundColor(new java.awt.Color(248, 252, 255));
        rec.addElement(new Paragraph("Destinataire", sectionFont));
        rec.addElement(new Paragraph(safe(clientInfo.getNomComplet()), normalFont));
        rec.addElement(new Paragraph(safe(clientInfo.getAdresse()), normalFont));
        rec.addElement(new Paragraph(safe(clientInfo.getCodePostalVille()), normalFont));
        if (!safe(clientInfo.getNumeroCarteVitale()).isBlank()) {
            rec.addElement(new Paragraph("N° carte vitale: " + safe(clientInfo.getNumeroCarteVitale()), normalFont));
        }
        recipient.addCell(rec);
        document.add(recipient);

        document.add(new Paragraph(" "));

        PdfPTable details = new PdfPTable(new float[]{2.8f, 0.8f, 1.2f, 0.9f, 1.2f, 1.3f});
        details.setWidthPercentage(100);
        addHeader(details, "Medicament", headerFont);
        addHeader(details, "Qte", headerFont);
        addHeader(details, "PU HT", headerFont);
        addHeader(details, "TVA", headerFont);
        addHeader(details, "PU TTC", headerFont);
        addHeader(details, "Total TTC", headerFont);

        double totalHT = 0.0;
        double totalTVA = 0.0;
        double totalTTC = 0.0;
        for (LigneVente line : lignes) {
            addCell(details, line.getNomMedicament(), normalFont, Element.ALIGN_LEFT);
            addCell(details, String.valueOf(line.getQuantite()), normalFont, Element.ALIGN_CENTER);
            addCell(details, euro(line.getPrixUnitaireHT()), normalFont, Element.ALIGN_RIGHT);
            addCell(details, String.format(Locale.FRANCE, "%.2f %%", line.getTauxTva()), normalFont, Element.ALIGN_RIGHT);
            addCell(details, euro(line.getPrixUnitaireTTC()), normalFont, Element.ALIGN_RIGHT);
            addCell(details, euro(line.getSousTotalTTC()), normalFont, Element.ALIGN_RIGHT);

            totalHT += line.getSousTotalHT();
            totalTVA += line.getSousTotalTVA();
            totalTTC += line.getSousTotalTTC();
        }
        document.add(details);

        document.add(new Paragraph(" "));

        PdfPTable totals = new PdfPTable(new float[]{3.4f, 1.2f});
        totals.setWidthPercentage(48);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        addTotalLine(totals, "Total HT", euro(totalHT), headerFont, normalFont);
        addTotalLine(totals, "Total TVA", euro(totalTVA), headerFont, normalFont);
        addTotalLine(totals, "Total TTC", euro(totalTTC), headerFont, headerFont);
        document.add(totals);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Document genere automatiquement par SGPA.", smallFont));
        if (vendeurName != null && !vendeurName.isBlank()) {
            document.add(new Paragraph("Vendeur: " + vendeurName, smallFont));
        }
        if ("DEVIS".equals(docType)) {
            document.add(new Paragraph("Validite du devis: 30 jours.", smallFont));
        }

        document.close();
    }

    private String euro(double amount) {
        return String.format(Locale.FRANCE, "%,.2f €", amount).replace(',', 'X').replace('.', ',').replace('X', '.');
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void addHeader(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6f);
        cell.setBackgroundColor(new java.awt.Color(31, 138, 91));
        cell.setBorderColor(new java.awt.Color(213, 227, 220));
        cell.setPhrase(new Phrase(value, new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.WHITE)));
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String value, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(5f);
        cell.setBorderColor(new java.awt.Color(223, 231, 238));
        table.addCell(cell);
    }

    private void addTotalLine(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell l = new PdfPCell(new Phrase(label, labelFont));
        l.setHorizontalAlignment(Element.ALIGN_LEFT);
        l.setPadding(6f);
        l.setBorderColor(new java.awt.Color(213, 227, 220));
        table.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value, valueFont));
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(6f);
        v.setBorderColor(new java.awt.Color(213, 227, 220));
        table.addCell(v);
    }

    private static class LoginControllerSafe {
        private static String currentUserDisplay() {
            try {
                sgpa.Entities.User user = sgpa.Controller.LoginController.getCurrentUser();
                if (user == null) return "";
                return user.getPrenom() + " " + user.getNom();
            } catch (Exception e) {
                return "";
            }
        }
    }
}
