package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Adolescente;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdolescenteDAO {

    public boolean inserir(Adolescente adolescente) {
        String sqlPessoa = "INSERT INTO Pessoa (cpf, nome_completo, data_nascimento, contato, email) VALUES (?, ?, ?, ?, ?)";
        String sqlAdolescente = "INSERT INTO Adolescente (cpf_adolescente, naturalidade, genero, cor_raca, status) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;

        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtPessoa = conn.prepareStatement(sqlPessoa)) {
                stmtPessoa.setLong(1, adolescente.getCpf());
                stmtPessoa.setString(2, adolescente.getNomeCompleto());
                stmtPessoa.setDate(3, Date.valueOf(adolescente.getDataNascimento())); // Converte LocalDate para Date do SQL
                stmtPessoa.setString(4, adolescente.getContato());
                stmtPessoa.setString(5, adolescente.getEmail());
                stmtPessoa.executeUpdate();
            }

            try (PreparedStatement stmtAdolescente = conn.prepareStatement(sqlAdolescente)) {
                stmtAdolescente.setLong(1, adolescente.getCpf()); // O CPF é a chave que liga as duas tabelas
                stmtAdolescente.setString(2, adolescente.getNaturalidade());
                stmtAdolescente.setString(3, adolescente.getGenero());
                stmtAdolescente.setString(4, adolescente.getCorRaca());
                stmtAdolescente.setString(5, adolescente.getStatus());
                stmtAdolescente.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar adolescente: " + e.getMessage());
            try {
                if (conn != null) {
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

    public List<Adolescente> listar() {
        List<Adolescente> lista = new ArrayList<>();

        String sql = "SELECT p.cpf, p.nome_completo, p.data_nascimento, p.contato, p.email, " +
                "a.naturalidade, a.genero, a.cor_raca, a.status " +
                "FROM Pessoa p " +
                "INNER JOIN Adolescente a ON p.cpf = a.cpf_adolescente";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Adolescente jovem = new Adolescente();

                jovem.setCpf(rs.getLong("cpf"));
                jovem.setNomeCompleto(rs.getString("nome_completo"));
                jovem.setDataNascimento(rs.getDate("data_nascimento").toLocalDate()); // Converte de volta para LocalDate
                jovem.setContato(rs.getString("contato"));
                jovem.setEmail(rs.getString("email"));

                jovem.setNaturalidade(rs.getString("naturalidade"));
                jovem.setGenero(rs.getString("genero"));
                jovem.setCorRaca(rs.getString("cor_raca"));
                jovem.setStatus(rs.getString("status"));

                lista.add(jovem);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar adolescentes: " + e.getMessage());
        }

        return lista;
    }

    public boolean atualizar(Adolescente adolescente) {
        String sqlPessoa = "UPDATE Pessoa SET nome_completo = ?, data_nascimento = ?, contato = ?, email = ? WHERE cpf = ?";
        String sqlAdolescente = "UPDATE Adolescente SET naturalidade = ?, genero = ?, cor_raca = ?, status = ? WHERE cpf_adolescente = ?";

        Connection conn = null;

        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false); // Inicia a transação

            // Atualiza os dados na tabela pai (Pessoa)
            try (PreparedStatement stmtPessoa = conn.prepareStatement(sqlPessoa)) {
                stmtPessoa.setString(1, adolescente.getNomeCompleto());
                stmtPessoa.setDate(2, Date.valueOf(adolescente.getDataNascimento()));
                stmtPessoa.setString(3, adolescente.getContato());
                stmtPessoa.setString(4, adolescente.getEmail());
                stmtPessoa.setLong(5, adolescente.getCpf());
                stmtPessoa.executeUpdate();
            }

            // Atualiza os dados na tabela filha (Adolescente)
            try (PreparedStatement stmtAdolescente = conn.prepareStatement(sqlAdolescente)) {
                stmtAdolescente.setString(1, adolescente.getNaturalidade());
                stmtAdolescente.setString(2, adolescente.getGenero());
                stmtAdolescente.setString(3, adolescente.getCorRaca());
                stmtAdolescente.setString(4, adolescente.getStatus());
                stmtAdolescente.setLong(5, adolescente.getCpf());
                stmtAdolescente.executeUpdate();
            }

            conn.commit(); // Confirma as duas alterações
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar adolescente: " + e.getMessage());
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

    public boolean inativar(long cpf) {
        String sql = "UPDATE Adolescente SET status = 'Inativo' WHERE cpf_adolescente = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpf);
            int linhasAfetadas = stmt.executeUpdate();

            return linhasAfetadas > 0;

        } catch (SQLException e) {
            System.err.println("Erro ao inativar adolescente: " + e.getMessage());
            return false;
        }
    }
}