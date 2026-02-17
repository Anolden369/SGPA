package sgpa.Entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Commande {
    private int id;
    private int idFournisseur;
    private String nomFournisseur;
    private LocalDate dateCommande;
    private String statut;
    private List<LigneCommande> lignes;

    public Commande(int id, int idFournisseur, String nomFournisseur, LocalDate dateCommande, String statut) {
        this.id = id;
        this.idFournisseur = idFournisseur;
        this.nomFournisseur = nomFournisseur;
        this.dateCommande = dateCommande;
        this.statut = statut;
        this.lignes = new ArrayList<>();
    }

    public Commande() {
        this.lignes = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getReference() { return String.format("CMD-%06d", id); }
    public void setId(int id) { this.id = id; }
    public int getIdFournisseur() { return idFournisseur; }
    public void setIdFournisseur(int idFournisseur) { this.idFournisseur = idFournisseur; }
    public String getNomFournisseur() { return nomFournisseur; }
    public void setNomFournisseur(String nomFournisseur) { this.nomFournisseur = nomFournisseur; }
    public LocalDate getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDate dateCommande) { this.dateCommande = dateCommande; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public List<LigneCommande> getLignes() { return lignes; }
    public void setLignes(List<LigneCommande> lignes) { this.lignes = lignes; }
}
