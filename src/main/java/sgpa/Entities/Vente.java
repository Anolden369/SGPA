package sgpa.Entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Vente {
    private int id;
    private LocalDateTime dateHeure;
    private double montantHT;
    private double montantTVA;
    private double montantTTC;
    private boolean surOrdonnance;
    private int idUser;
    private String nomVendeur;
    private List<LigneVente> lignes;

    public Vente(int id, LocalDateTime dateHeure, double montantHT, double montantTVA, double montantTTC, boolean surOrdonnance, int idUser, String nomVendeur) {
        this.id = id;
        this.dateHeure = dateHeure;
        this.montantHT = montantHT;
        this.montantTVA = montantTVA;
        this.montantTTC = montantTTC;
        this.surOrdonnance = surOrdonnance;
        this.idUser = idUser;
        this.nomVendeur = nomVendeur;
        this.lignes = new ArrayList<>();
    }

    public Vente() {
        this.lignes = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getReference() { return String.format("VTE-%06d", id); }
    public void setId(int id) { this.id = id; }
    public LocalDateTime getDateHeure() { return dateHeure; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }
    public double getMontantHT() { return montantHT; }
    public void setMontantHT(double montantHT) { this.montantHT = montantHT; }
    public double getMontantTVA() { return montantTVA; }
    public void setMontantTVA(double montantTVA) { this.montantTVA = montantTVA; }
    public double getMontantTTC() { return montantTTC; }
    public void setMontantTTC(double montantTTC) { this.montantTTC = montantTTC; }
    public boolean isSurOrdonnance() { return surOrdonnance; }
    public void setSurOrdonnance(boolean surOrdonnance) { this.surOrdonnance = surOrdonnance; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public String getNomVendeur() { return nomVendeur; }
    public void setNomVendeur(String nomVendeur) { this.nomVendeur = nomVendeur; }
    public List<LigneVente> getLignes() { return lignes; }
    public void setLignes(List<LigneVente> lignes) { this.lignes = lignes; }
}
