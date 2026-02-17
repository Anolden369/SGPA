package sgpa.Entities;

import java.time.LocalDate;

public class LigneCommande {
    private int idCommande;
    private int idMedicament;
    private String nomMedicament;
    private int quantite;
    private LocalDate datePeremption;

    public LigneCommande(int idCommande, int idMedicament, String nomMedicament, int quantite) {
        this.idCommande = idCommande;
        this.idMedicament = idMedicament;
        this.nomMedicament = nomMedicament;
        this.quantite = quantite;
        this.datePeremption = null;
    }

    public LigneCommande(int idCommande, int idMedicament, String nomMedicament, int quantite, LocalDate datePeremption) {
        this.idCommande = idCommande;
        this.idMedicament = idMedicament;
        this.nomMedicament = nomMedicament;
        this.quantite = quantite;
        this.datePeremption = datePeremption;
    }

    public int getIdCommande() { return idCommande; }
    public int getIdMedicament() { return idMedicament; }
    public String getNomMedicament() { return nomMedicament; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public LocalDate getDatePeremption() { return datePeremption; }
    public void setDatePeremption(LocalDate datePeremption) { this.datePeremption = datePeremption; }
}
