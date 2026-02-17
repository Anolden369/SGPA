package sgpa.Services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.TimeZone;

public class ConnexionBDD {
    private static Connection cnx;

    public static void connect() throws ClassNotFoundException, SQLException {
        String pilote = "com.mysql.cj.jdbc.Driver";
        Class.forName(pilote);
        if (cnx != null && !cnx.isClosed()) {
            return;
        }
        String url = "jdbc:mysql://localhost:8889/bdd_sgpa?serverTimezone=" + TimeZone.getDefault().getID()
                + "&connectTimeout=2500&socketTimeout=4000";
        cnx = DriverManager.getConnection(url, "root", "root");
        System.out.println("Connexion à la base de données SGPA réussie");
    }

    public static Connection getCnx() {
        return cnx;
    }
}
