package br.edu.unespar.trabalho.util;

import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;

public class TesteConexao {
    public static void main(String[] args) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            System.out.println("Conexão com o banco db_instituto_casa realizada com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro ao conectar ao banco: " + e.getMessage());
        }
    }
}