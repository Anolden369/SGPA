package sgpa.Entities;

public class Fournisseur {
    private int id;
    private String nom;
    private String contact;
    private String adresse;

    public Fournisseur(int id, String nom, String contact, String adresse) {
        this.id = id;
        this.nom = nom;
        this.contact = contact;
        this.adresse = adresse;
    }

    public Fournisseur() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    @Override
    public String toString() {
        return nom;
    }
}
