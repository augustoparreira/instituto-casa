package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Adolescente;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AdolescenteDAO {

    public boolean inserir(Adolescente adolescente) {
        String sqlPessoa = "INSERT INTO Pessoa (cpf, nome_completo, data_nascimento, contato, email) VALUES (?, ?, ?, ?, ?)";
        String sqlAdolescente = "INSERT INTO Adolescente (cpf_adolescente, naturalidade, genero, cor_raca, status) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;

        try {
            conn = ConnectionFactory.getConnection();
            // Desliga a gravação automática para criar a Transação
            conn.setAutoCommit(false);

            // 1. Insere na tabela Pessoa
            try (PreparedStatement stmtPessoa = conn.prepareStatement(sqlPessoa)) {
                stmtPessoa.setLong(1, adolescente.getCpf());
                stmtPessoa.setString(2, adolescente.getNomeCompleto());
                stmtPessoa.setDate(3, Date.valueOf(adolescente.getDataNascimento())); // Converte LocalDate para Date do SQL
                stmtPessoa.setString(4, adolescente.getContato());
                stmtPessoa.setString(5, adolescente.getEmail());
                stmtPessoa.executeUpdate();
            }

            // 2. Insere na tabela Adolescente
            try (PreparedStatement stmtAdolescente = conn.prepareStatement(sqlAdolescente)) {
                stmtAdolescente.setLong(1, adolescente.getCpf()); // O CPF é a chave que liga as duas tabelas
                stmtAdolescente.setString(2, adolescente.getNaturalidade());
                stmtAdolescente.setString(3, adolescente.getGenero());
                stmtAdolescente.setString(4, adolescente.getCorRaca());
                stmtAdolescente.setString(5, adolescente.getStatus());
                stmtAdolescente.executeUpdate();
            }

            // Se chegou até aqui sem dar erro, efetiva a gravação nas duas tabelas
            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar adolescente: " + e.getMessage());
            try {
                if (conn != null) {
                    // Se deu qualquer erro, desfaz tudo (Rollback)
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Erro ao fazer rollback: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
}