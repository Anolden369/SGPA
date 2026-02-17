package sgpa.Entities;

import java.time.LocalDate;

public class Medicament {
    private int id;
    private String nomCommercial;
    private String principeActif;
    private String formeGalenique;
    private String dosage;
    private double prixPublic;
    private double prixAchatHT;
    private double tauxTva;
    private boolean necessiteOrdonnance;
    private LocalDate datePeremption;
    private int stockActuel;
    private int stockMinimum;
    private int quantiteCommande;

    public Medicament(int id, String nomCommercial, String principeActif, String formeGalenique, String dosage,
                      double prixPublic, double prixAchatHT, double tauxTva, boolean necessiteOrdonnance,
                      LocalDate datePeremption, int stockActuel, int stockMinimum) {
        this.id = id;
        this.nomCommercial = nomCommercial;
        this.principeActif = principeActif;
        this.formeGalenique = formeGalenique;
        this.dosage = dosage;
        this.prixPublic = prixPublic;
        this.prixAchatHT = prixAchatHT;
        this.tauxTva = tauxTva;
        this.necessiteOrdonnance = necessiteOrdonnance;
        this.datePeremption = datePeremption;
        this.stockActuel = stockActuel;
        this.stockMinimum = stockMinimum;
        this.quantiteCommande = 0;
    }

    public Medicament() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNomCommercial() { return nomCommercial; }
    public void setNomCommercial(String nomCommercial) { this.nomCommercial = nomCommercial; }
    public String getPrincipeActif() { return principeActif; }
    public void setPrincipeActif(String principeActif) { this.principeActif = principeActif; }
    public String getFormeGalenique() { return formeGalenique; }
    public void setFormeGalenique(String formeGalenique) { this.formeGalenique = formeGalenique; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public double getPrixPublic() { return prixPublic; }
    public void setPrixPublic(double prixPublic) { this.prixPublic = prixPublic; }
    public double getPrixAchatHT() { return prixAchatHT; }
    public void setPrixAchatHT(double prixAchatHT) { this.prixAchatHT = prixAchatHT; }
    public double getTauxTva() { return tauxTva; }
    public void setTauxTva(double tauxTva) { this.tauxTva = tauxTva; }
    public double getPrixTTC() { return prixPublic + (prixPublic * tauxTva / 100.0); }
    public double getMargeUnitaireHT() { return prixPublic - prixAchatHT; }
    public boolean isNecessiteOrdonnance() { return necessiteOrdonnance; }
    public void setNecessiteOrdonnance(boolean necessiteOrdonnance) { this.necessiteOrdonnance = necessiteOrdonnance; }
    public LocalDate getDatePeremption() { return datePeremption; }
    public void setDatePeremption(LocalDate datePeremption) { this.datePeremption = datePeremption; }
    public int getStockActuel() { return stockActuel; }
    public void setStockActuel(int stockActuel) { this.stockActuel = stockActuel; }
    public int getStockMinimum() { return stockMinimum; }
    public void setStockMinimum(int stockMinimum) { this.stockMinimum = stockMinimum; }
    public int getQuantiteCommande() { return quantiteCommande; }
    public void setQuantiteCommande(int quantiteCommande) { this.quantiteCommande = quantiteCommande; }

    @Override
    public String toString() {
        return nomCommercial;
    }
}
