package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Responsavel;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResponsavelDAO {

    public boolean inserir(Responsavel responsavel, long cpfAdolescente) {
        String sqlPessoa = "INSERT INTO Pessoa (cpf, nome_completo, data_nascimento, contato, email) VALUES (?, ?, ?, ?, ?)";
        String sqlResponsavel = "INSERT INTO Responsavel (cpf_responsavel, parentesco, contato_principal) VALUES (?, ?, ?)";
        String sqlResponsabiliza = "INSERT INTO Responsabiliza (cpf_adolescente, cpf_responsavel) VALUES (?, ?)";

        Connection conn = null;

        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false); // Inicia a transação

            // 1. Grava Pessoa
            try (PreparedStatement stmtPessoa = conn.prepareStatement(sqlPessoa)) {
                stmtPessoa.setLong(1, responsavel.getCpf());
                stmtPessoa.setString(2, responsavel.getNomeCompleto());
                stmtPessoa.setDate(3, Date.valueOf(responsavel.getDataNascimento()));
                stmtPessoa.setString(4, responsavel.getContato());
                stmtPessoa.setString(5, responsavel.getEmail());
                stmtPessoa.executeUpdate();
            }

            // 2. Grava Responsável (com as colunas exatas do seu \d)
            try (PreparedStatement stmtResp = conn.prepareStatement(sqlResponsavel)) {
                stmtResp.setLong(1, responsavel.getCpf());
                stmtResp.setString(2, responsavel.getParentesco());
                stmtResp.setBoolean(3, responsavel.isContatoPrincipal());
                stmtResp.executeUpdate();
            }

            // 3. Grava o Vínculo
            try (PreparedStatement stmtVinculo = conn.prepareStatement(sqlResponsabiliza)) {
                stmtVinculo.setLong(1, cpfAdolescente);
                stmtVinculo.setLong(2, responsavel.getCpf());
                stmtVinculo.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar responsável: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
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

    public List<Responsavel> listarResponsaveis(long cpfAdolescente) {
        List<Responsavel> lista = new ArrayList<>();

        String sql = "SELECT p.cpf, p.nome_completo, p.data_nascimento, p.contato, p.email, " +
                "r.parentesco, r.contato_principal " +
                "FROM Pessoa p " +
                "INNER JOIN Responsavel r ON p.cpf = r.cpf_responsavel " +
                "INNER JOIN Responsabiliza v ON r.cpf_responsavel = v.cpf_responsavel " +
                "WHERE v.cpf_adolescente = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfAdolescente);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Responsavel resp = new Responsavel();

                    // Dados da tabela Pessoa
                    resp.setCpf(rs.getLong("cpf"));
                    resp.setNomeCompleto(rs.getString("nome_completo"));
                    resp.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());
                    resp.setContato(rs.getString("contato"));
                    resp.setEmail(rs.getString("email"));

                    // Dados da tabela Responsavel
                    resp.setParentesco(rs.getString("parentesco"));
                    resp.setContatoPrincipal(rs.getBoolean("contato_principal"));

                    lista.add(resp);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar responsáveis: " + e.getMessage());
        }

        return lista;
    }
}