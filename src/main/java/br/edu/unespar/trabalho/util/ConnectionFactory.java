package br.edu.unespar.trabalho.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {

    private static final String URL = "jdbc:postgresql://localhost:5432/db_instituto_casa";
    private static final String USER = "postgres";
    private static final String PASS = "admin123";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(System.getProperty("casa.db.url", URL),
                    System.getProperty("casa.db.user", USER), System.getProperty("casa.db.password", PASS));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar no banco db_instituto_casa", e);
        }
    }
}