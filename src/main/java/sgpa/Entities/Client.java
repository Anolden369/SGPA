package sgpa.Entities;

public class Client {
    private final String nomComplet;
    private final String adresse;
    private final String codePostalVille;
    private final String numeroCarteVitale;
    private final boolean sendByMail;
    private final String emailDestinataire;

    public Client(String nomComplet, String adresse, String codePostalVille,
                  String numeroCarteVitale, boolean sendByMail, String emailDestinataire) {
        this.nomComplet = nomComplet;
        this.adresse = adresse;
        this.codePostalVille = codePostalVille;
        this.numeroCarteVitale = numeroCarteVitale;
        this.sendByMail = sendByMail;
        this.emailDestinataire = emailDestinataire;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public String getAdresse() {
        return adresse;
    }

    public String getCodePostalVille() {
        return codePostalVille;
    }

    public String getNumeroCarteVitale() {
        return numeroCarteVitale;
    }

    public boolean isSendByMail() {
        return sendByMail;
    }

    public String getEmailDestinataire() {
        return emailDestinataire;
    }
}
