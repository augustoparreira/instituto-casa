package br.edu.unespar.trabalho.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Conexão JDBC configurada externamente, sem credenciais no código-fonte. */
public class ConnectionFactory {
    /* Configure casa.db.url/user/password na JVM ou CASA_DB_URL/USER/PASSWORD no ambiente.
       Não versione credenciais. A compilação não precisa de conexão com o PostgreSQL. */
    public static Connection getConnection() {
        String url = configuracao("casa.db.url", "CASA_DB_URL");
        String usuario = configuracao("casa.db.user", "CASA_DB_USER");
        String senha = configuracao("casa.db.password", "CASA_DB_PASSWORD");
        if (url == null || url.isBlank() || usuario == null || usuario.isBlank() || senha == null)
            throw new IllegalStateException("Configure a conexão PostgreSQL no ambiente de execução.");
        try { return DriverManager.getConnection(url, usuario, senha); }
        catch (SQLException e) { throw new IllegalStateException("Não foi possível conectar ao PostgreSQL. Confira a configuração e o serviço."); }
    }

    private static String configuracao(String propriedade, String ambiente) {
        String valor = System.getProperty(propriedade);
        return valor != null ? valor : System.getenv(ambiente);
    }
}
