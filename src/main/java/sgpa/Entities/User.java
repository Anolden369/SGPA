package sgpa.Entities;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String role; // Libellé du rôle

    public User(int id, String nom, String prenom, String email, String role) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
