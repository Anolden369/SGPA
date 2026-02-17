package sgpa.Services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import sgpa.Entities.Medicament;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class ServicesNotification {
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicesSettings settingsService;

    public ServicesNotification() {
        this.settingsService = new ServicesSettings();
    }

    public boolean sendLowStockAlertByIds(List<Integer> medicamentIds) throws SQLException, MessagingException {
        if (medicamentIds == null || medicamentIds.isEmpty()) {
            return false;
        }
        ServicesMedicament medicamentService = new ServicesMedicament();
        List<Medicament> medicaments = medicamentService.getMedicamentsByIds(medicamentIds);
        if (medicaments.isEmpty()) {
            return false;
        }

        MailConfig config = loadConfig();
        if (!config.enabled || config.recipients.isEmpty()) {
            return false;
        }

        String subject = "[SGPA] Alerte stock bas - " + medicaments.size() + " produit(s)";
        String htmlBody = buildLowStockHtmlBody(medicaments);
        sendMail(config, subject, htmlBody, true);
        return true;
    }

    public void sendTestMail() throws SQLException, MessagingException {
        MailConfig config = loadConfig();
        if (!config.enabled || config.recipients.isEmpty()) {
            throw new MessagingException("Notifications email désactivées ou destinataires absents.");
        }
        String htmlBody = "<html><body style='font-family:Segoe UI,Arial,sans-serif;color:#1f2937'>" +
                "<h2 style='margin:0 0 8px 0;color:#14532d'>Email de test SGPA</h2>" +
                "<p style='margin:0 0 12px 0'>Date: " + LocalDateTime.now().format(DATE_FR) + "</p>" +
                "<p style='margin:0'>La configuration SMTP est opérationnelle.</p>" +
                "</body></html>";
        sendMail(config, "[SGPA] Test SMTP", htmlBody, true);
    }

    public void sendDocumentMail(String recipient, String subject, String htmlBody, File attachment) throws SQLException, MessagingException {
        if (recipient == null || recipient.isBlank()) {
            throw new MessagingException("Destinataire email manquant.");
        }
        MailConfig config = loadConfig();
        if (!config.enabled) {
            throw new MessagingException("Notifications email désactivées dans Paramètres.");
        }
        sendMailWithAttachment(config, List.of(recipient.trim()), subject, htmlBody, attachment);
    }

    private MailConfig loadConfig() throws SQLException {
        Map<String, String> values = settingsService.getAll();
        MailConfig config = new MailConfig();
        config.enabled = parseBoolean(values.getOrDefault("smtp_enabled", "false"));
        config.host = values.getOrDefault("smtp_host", "");
        config.port = Integer.parseInt(values.getOrDefault("smtp_port", "587"));
        config.username = values.getOrDefault("smtp_username", "");
        config.password = values.getOrDefault("smtp_password", "");
        config.from = values.getOrDefault("smtp_from", "");
        config.recipients = splitRecipients(values.getOrDefault("stock_alert_recipients", ""));
        return config;
    }

    private void sendMail(MailConfig config, String subject, String body, boolean isHtml) throws MessagingException {
        if (isBlank(config.host) || isBlank(config.from)) {
            throw new MessagingException("Paramètres SMTP incomplets (host/from).");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", config.host);
        props.put("mail.smtp.port", String.valueOf(config.port));
        props.put("mail.smtp.auth", String.valueOf(!isBlank(config.username)));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", config.host);

        Session session;
        if (!isBlank(config.username)) {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.username, config.password);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.from));
        for (String recipient : config.recipients) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }
        message.setSubject(subject, "UTF-8");
        if (isHtml) {
            message.setContent(body, "text/html; charset=UTF-8");
        } else {
            message.setText(body, "UTF-8");
        }

        Transport.send(message);
    }

    private void sendMailWithAttachment(MailConfig config, List<String> recipients, String subject, String htmlBody, File attachment) throws MessagingException {
        if (isBlank(config.host) || isBlank(config.from)) {
            throw new MessagingException("Paramètres SMTP incomplets (host/from).");
        }
        if (attachment == null || !attachment.exists()) {
            throw new MessagingException("Pièce jointe introuvable.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", config.host);
        props.put("mail.smtp.port", String.valueOf(config.port));
        props.put("mail.smtp.auth", String.valueOf(!isBlank(config.username)));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", config.host);

        Session session;
        if (!isBlank(config.username)) {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.username, config.password);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.from));
        for (String recipient : recipients) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }
        message.setSubject(subject, "UTF-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

        MimeBodyPart attachmentPart = new MimeBodyPart();
        try {
            attachmentPart.attachFile(attachment);
        } catch (Exception e) {
            throw new MessagingException("Impossible d'ajouter la pièce jointe: " + e.getMessage());
        }

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(htmlPart);
        multipart.addBodyPart(attachmentPart);
        message.setContent(multipart);

        Transport.send(message);
    }

    private String buildLowStockHtmlBody(List<Medicament> medicaments) {
        StringBuilder rows = new StringBuilder();
        for (Medicament m : medicaments) {
            String status = m.getStockActuel() <= 0 ? "Rupture" : "Stock bas";
            String statusColor = m.getStockActuel() <= 0 ? "#b91c1c" : "#92400e";
            rows.append("<tr>")
                    .append("<td style='padding:10px;border:1px solid #d1d5db;'>")
                    .append(escapeHtml(m.getNomCommercial()))
                    .append("</td>")
                    .append("<td style='padding:10px;border:1px solid #d1d5db;text-align:center;'>")
                    .append(m.getStockActuel())
                    .append("</td>")
                    .append("<td style='padding:10px;border:1px solid #d1d5db;text-align:center;'>")
                    .append(m.getStockMinimum())
                    .append("</td>")
                    .append("<td style='padding:10px;border:1px solid #d1d5db;font-weight:700;color:")
                    .append(statusColor)
                    .append(";'>")
                    .append(status)
                    .append("</td>")
                    .append("</tr>");
        }

        return "<html><body style='font-family:Segoe UI,Arial,sans-serif;color:#1f2937;'>" +
                "<h2 style='margin:0 0 8px 0;color:#14532d;'>Alerte automatique SGPA</h2>" +
                "<p style='margin:0 0 12px 0;'>Date: " + LocalDateTime.now().format(DATE_FR) + "</p>" +
                "<p style='margin:0 0 12px 0;'>Les produits suivants ont atteint ou dépassé le seuil minimum :</p>" +
                "<table style='border-collapse:collapse;width:100%;max-width:760px;background:#ffffff;'>" +
                "<thead>" +
                "<tr style='background:#dcfce7;'>" +
                "<th style='padding:10px;border:1px solid #d1d5db;text-align:left;'>Médicament</th>" +
                "<th style='padding:10px;border:1px solid #d1d5db;text-align:center;'>Stock actuel</th>" +
                "<th style='padding:10px;border:1px solid #d1d5db;text-align:center;'>Stock minimum</th>" +
                "<th style='padding:10px;border:1px solid #d1d5db;text-align:left;'>Statut</th>" +
                "</tr>" +
                "</thead>" +
                "<tbody>" + rows + "</tbody>" +
                "</table>" +
                "<p style='margin:12px 0 0 0;'>Action recommandée : préparer une commande fournisseur.</p>" +
                "</body></html>";
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private List<String> splitRecipients(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return List.of(value.split(","))
                .stream()
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .collect(Collectors.toList());
    }

    private boolean parseBoolean(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static class MailConfig {
        private boolean enabled;
        private String host;
        private int port;
        private String username;
        private String password;
        private String from;
        private List<String> recipients = new ArrayList<>();
    }
}
