package sgpa.Entities;

public class LigneVente {
    private int idVente;
    private int idMedicament;
    private String nomMedicament;
    private int quantite;
    private double prixUnitaireHT;
    private double tauxTva;
    private double montantTva;
    private double prixUnitaireTTC;

    public LigneVente(int idVente, int idMedicament, String nomMedicament, int quantite, double prixUnitaireHT, double tauxTva, double montantTva, double prixUnitaireTTC) {
        this.idVente = idVente;
        this.idMedicament = idMedicament;
        this.nomMedicament = nomMedicament;
        this.quantite = quantite;
        this.prixUnitaireHT = prixUnitaireHT;
        this.tauxTva = tauxTva;
        this.montantTva = montantTva;
        this.prixUnitaireTTC = prixUnitaireTTC;
    }

    public int getIdVente() { return idVente; }
    public int getIdMedicament() { return idMedicament; }
    public String getNomMedicament() { return nomMedicament; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public double getPrixUnitaireHT() { return prixUnitaireHT; }
    public double getTauxTva() { return tauxTva; }
    public double getMontantTva() { return montantTva; }
    public double getPrixUnitaireTTC() { return prixUnitaireTTC; }
    public double getSousTotalHT() { return quantite * prixUnitaireHT; }
    public double getSousTotalTTC() { return quantite * prixUnitaireTTC; }
    public double getSousTotalTVA() { return quantite * montantTva; }
}
