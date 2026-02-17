package sgpa.Entities;

public class PredictionCommande {
    private int idMedicament;
    private String nomMedicament;
    private int stockActuel;
    private int stockMinimum;
    private double demandePrediteMensuelle;
    private int quantiteSuggeree;
    private String niveauRisque;

    public PredictionCommande(int idMedicament, String nomMedicament, int stockActuel, int stockMinimum,
                              double demandePrediteMensuelle, int quantiteSuggeree, String niveauRisque) {
        this.idMedicament = idMedicament;
        this.nomMedicament = nomMedicament;
        this.stockActuel = stockActuel;
        this.stockMinimum = stockMinimum;
        this.demandePrediteMensuelle = demandePrediteMensuelle;
        this.quantiteSuggeree = quantiteSuggeree;
        this.niveauRisque = niveauRisque;
    }

    public int getIdMedicament() {
        return idMedicament;
    }

    public String getNomMedicament() {
        return nomMedicament;
    }

    public int getStockActuel() {
        return stockActuel;
    }

    public int getStockMinimum() {
        return stockMinimum;
    }

    public double getDemandePrediteMensuelle() {
        return demandePrediteMensuelle;
    }

    public int getQuantiteSuggeree() {
        return quantiteSuggeree;
    }

    public String getNiveauRisque() {
        return niveauRisque;
    }
}
