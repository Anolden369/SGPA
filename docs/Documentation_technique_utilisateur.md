# SGPA - Documentation Technique et Utilisateur (Lancement)

## 1. Objectif

Ce document explique comment:

1. Installer les prérequis
2. Démarrer l'environnement local (MySQL + phpMyAdmin)
3. Importer la base de données SGPA
4. Lancer l'application JavaFX
5. Vérifier rapidement que tout fonctionne


## 2. Prérequis

## 2.1. Outils communs (Mac/Windows)

- JDK 21 (LTS)
- IntelliJ IDEA (Community ou Ultimate)
- Maven (optionnel si vous utilisez Maven intégré d'IntelliJ)
- Un serveur MySQL local avec phpMyAdmin:
  - Mac: MAMP recommandé
  - Windows: UwAmp, XAMPP ou WampServer

## 2.2. Versions conseillées

- Java: 21
- JavaFX (géré par Maven): 21.0.2
- MySQL: 8.x


## 3. Base de données à utiliser

Fichier SQL recommandé (propre et complet):

- `docs/database/bdd-sgpa.sql`

Copie externe disponible:

- `docs/database/bdd-sgpa.sql`

Ce script contient:

- la suppression/recréation des tables
- les données de test actuelles
- les colonnes nécessaires à la marge (`prix_achat_ht`, `cout_achat_unitaire_ht`)
- les paramètres SMTP déjà renseignés dans `app_settings`


## 4. Démarrage MySQL + phpMyAdmin

## 4.1. Sur Mac (MAMP)

1. Ouvrir MAMP
2. Démarrer `Apache` et `MySQL`
3. Ouvrir phpMyAdmin:
   - URL habituelle MAMP: `http://localhost:8888/phpMyAdmin`
4. Vérifier le port MySQL:
   - généralement `8889` dans MAMP

Note:
- Le code SGPA est actuellement configuré pour `localhost:8889`, user `root`, password `root`.
- Fichier concerné: `src/main/java/sgpa/Services/ConnexionBDD.java`

## 4.2. Sur Windows (UwAmp / XAMPP / WampServer)

1. Lancer l'outil (UwAmp / XAMPP / Wamp)
2. Démarrer `Apache` et `MySQL`
3. Ouvrir phpMyAdmin:
   - souvent `http://localhost/phpmyadmin`
4. Vérifier le port MySQL:
   - souvent `3306`

Si votre MySQL tourne sur `3306`, adapter `ConnexionBDD.java`:

```java
cnx = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/bdd_sgpa?serverTimezone=" + TimeZone.getDefault().getID(),
    "root",
    ""
);
```

Adaptez `user/password` selon votre configuration locale.


## 5. Import de la base via phpMyAdmin

1. Ouvrir phpMyAdmin
2. Créer une base nommée `bdd_sgpa` (collation `utf8mb4_general_ci` ou équivalent)
3. Ouvrir l'onglet `Importer`
4. Sélectionner `bdd-sgpa.sql`
5. Lancer l'import
6. Vérifier qu'il n'y a pas d'erreur SQL

Après import, vérifier que vous voyez les tables:

- `role`, `user`, `medicament`, `fournisseur`
- `commande`, `ligne_commande`
- `vente`, `ligne_vente`
- `app_settings`

Tables principales (version actuelle):

- `role`, `user`
- `medicament` (avec `prix_achat_ht`)
- `fournisseur`
- `commande`, `ligne_commande`
- `vente`, `ligne_vente` (avec `cout_achat_unitaire_ht`)
- `app_settings`


## 6. Lancement du projet Java

## 6.1. Depuis IntelliJ (recommandé)

1. Ouvrir le dossier projet:
   - `.../Project`
2. Vérifier le SDK:
   - `Project SDK = JDK 21`
3. Charger le projet Maven (`pom.xml`)
4. Lancer:
   - `Maven -> javafx -> javafx:run`
   - ou exécuter la classe `sgpa.SGPApplication`

## 6.2. Depuis terminal

Dans le dossier projet:

```bash
mvn clean javafx:run
```


## 7. Comptes de connexion (jeu de test)

Comptes typiques présents dans le dump:

- Admin: `admin@pharmacie.fr`
- Vendeur: `jean@pharmacie.fr`
- Vendeuse: `clara@pharmacie.fr`

Si besoin, test rapide avec l'admin (selon le seed utilisé):

- mot de passe souvent utilisé: `admin123`


## 8. Vérification fonctionnelle rapide (checklist)

1. Connexion admin OK
2. `Tableau de bord` affiche des KPI et graphiques
3. `Médicaments` affiche les lots groupés
4. `Ventes`:
   - ajout panier
   - validation vente
   - génération devis/facture PDF
   - facture depuis historique
   - envoi email optionnel du document
5. `Rapports financiers`:
   - HT / TVA / TTC / coût d'achat / marge
   - export PDF/Excel
6. `Paramètres`:
   - test SMTP (si configuré)


## 9. SMTP (optionnel pour envoi de documents)

Configurer dans `Paramètres` (admin):

- `smtp_enabled = true`
- `smtp_host` (ex: `smtp.gmail.com`)
- `smtp_port` (ex: `587`)
- `smtp_username`
- `smtp_password` (mot de passe d'application recommandé)
- `smtp_from`

Ensuite:

- utiliser le bouton de test email
- puis tester l'envoi de facture/devis avec la case `Envoyer par email`


## 10. Problèmes fréquents

## 10.1. Erreur de connexion MySQL

Vérifier:

- MySQL démarré
- port correct (`8889` MAMP / `3306` Windows)
- identifiants dans `ConnexionBDD.java`
- base `bdd_sgpa` bien importée

## 10.2. "Module not found" / JavaFX runtime

Vérifier:

- projet Maven bien rechargé
- JDK 21 bien sélectionné
- lancement via `mvn javafx:run` recommandé

## 10.3. Ressource image non trouvée

Vérifier la présence des images sous:

- `src/main/resources/Images`


## 11. Structure utile

- Application:
  - `src/main/java/sgpa/SGPApplication.java`
- Connexion DB:
  - `src/main/java/sgpa/Services/ConnexionBDD.java`
- Vues FXML:
  - `src/main/resources/sgpa/View`
- Styles CSS:
  - `src/main/resources/CSS/MesStyles.css`
- Scripts SQL:
  - `docs/database`
- Diagrammes UML (codes à jour):
  - `docs/diagrams/diagramme-classe-sgpa-v3.puml`
  - `docs/diagrams/diagramme-cas-utilisation-sgpa-v3.puml`

## 12. Rendu des diagrammes (optionnel)

Si PlantUML est installé, vous pouvez générer les exports:

```bash
plantuml docs/diagrams/diagramme-classe-sgpa-v3.puml
plantuml docs/diagrams/diagramme-cas-utilisation-sgpa-v3.puml
```
