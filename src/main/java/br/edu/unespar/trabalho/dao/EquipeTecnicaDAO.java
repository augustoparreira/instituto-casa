package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.EquipeTecnica;
import br.edu.unespar.trabalho.model.NivelAcesso;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipeTecnicaDAO {

    public EquipeTecnica autenticar(String login, String senha) {
        String sql = "SELECT p.cpf, p.nome_completo, e.login, e.cargo_funcao, e.nivel_acesso " +
                "FROM Pessoa p " +
                "INNER JOIN EquipeTecnica e ON p.cpf = e.cpf_equipe " +
                "WHERE e.login = ? AND e.senha = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            stmt.setString(2, senha);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    EquipeTecnica user = new EquipeTecnica();
                    user.setCpf(rs.getLong("cpf"));
                    user.setNomeCompleto(rs.getString("nome_completo"));
                    user.setLogin(rs.getString("login"));
                    user.setCargoFuncao(rs.getString("cargo_funcao"));
                    user.setNivelAcesso(NivelAcesso.fromCodigo(rs.getString("nivel_acesso"))); // Enum corrigido
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
        }

        return null;
    }

    public boolean inserir(EquipeTecnica membro) {
        String sqlPessoa = "INSERT INTO Pessoa (cpf, nome_completo, data_nascimento, contato, email) VALUES (?, ?, ?, ?, ?)";
        String sqlEquipe = "INSERT INTO EquipeTecnica (cpf_equipe, login, senha, cargo_funcao, nivel_acesso) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtPessoa = conn.prepareStatement(sqlPessoa)) {
                stmtPessoa.setLong(1, membro.getCpf());
                stmtPessoa.setString(2, membro.getNomeCompleto());
                stmtPessoa.setDate(3, Date.valueOf(membro.getDataNascimento()));
                stmtPessoa.setString(4, membro.getContato());
                stmtPessoa.setString(5, membro.getEmail());
                stmtPessoa.executeUpdate();
            }

            try (PreparedStatement stmtEquipe = conn.prepareStatement(sqlEquipe)) {
                stmtEquipe.setLong(1, membro.getCpf());
                stmtEquipe.setString(2, membro.getLogin());
                stmtEquipe.setString(3, membro.getSenha());
                stmtEquipe.setString(4, membro.getCargoFuncao());
                stmtEquipe.setString(5, membro.getNivelAcesso() != null ? membro.getNivelAcesso().getCodigo() : null); // Enum corrigido
                stmtEquipe.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao cadastrar membro da equipe: " + e.getMessage());
            rollbackSilencioso(conn);
            return false;
        } finally {
            fecharSilencioso(conn);
        }
    }

    public List<EquipeTecnica> listar() {
        List<EquipeTecnica> lista = new ArrayList<>();
        String sql = "SELECT p.cpf, p.nome_completo, p.data_nascimento, p.contato, p.email, " +
                "e.login, e.cargo_funcao, e.nivel_acesso " +
                "FROM Pessoa p INNER JOIN EquipeTecnica e ON p.cpf = e.cpf_equipe " +
                "ORDER BY p.nome_completo";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));

        } catch (SQLException e) {
            System.err.println("Erro ao listar equipe técnica: " + e.getMessage());
        }
        return lista;
    }

    public EquipeTecnica buscarPorCpf(long cpf) {
        String sql = "SELECT p.cpf, p.nome_completo, p.data_nascimento, p.contato, p.email, " +
                "e.login, e.cargo_funcao, e.nivel_acesso " +
                "FROM Pessoa p INNER JOIN EquipeTecnica e ON p.cpf = e.cpf_equipe " +
                "WHERE p.cpf = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar membro da equipe: " + e.getMessage());
        }
        return null;
    }

    public boolean atualizar(EquipeTecnica membro) {
        String sqlPessoa = "UPDATE Pessoa SET nome_completo = ?, data_nascimento = ?, contato = ?, email = ? WHERE cpf = ?";
        boolean trocaSenha = membro.getSenha() != null && !membro.getSenha().isBlank();
        String sqlEquipe = trocaSenha
                ? "UPDATE EquipeTecnica SET login = ?, cargo_funcao = ?, nivel_acesso = ?, senha = ? WHERE cpf_equipe = ?"
                : "UPDATE EquipeTecnica SET login = ?, cargo_funcao = ?, nivel_acesso = ? WHERE cpf_equipe = ?";

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtPessoa = conn.prepareStatement(sqlPessoa)) {
                stmtPessoa.setString(1, membro.getNomeCompleto());
                stmtPessoa.setDate(2, Date.valueOf(membro.getDataNascimento()));
                stmtPessoa.setString(3, membro.getContato());
                stmtPessoa.setString(4, membro.getEmail());
                stmtPessoa.setLong(5, membro.getCpf());
                stmtPessoa.executeUpdate();
            }

            try (PreparedStatement stmtEquipe = conn.prepareStatement(sqlEquipe)) {
                stmtEquipe.setString(1, membro.getLogin());
                stmtEquipe.setString(2, membro.getCargoFuncao());
                stmtEquipe.setString(3, membro.getNivelAcesso() != null ? membro.getNivelAcesso().getCodigo() : null); // Enum corrigido

                if (trocaSenha) {
                    stmtEquipe.setString(4, membro.getSenha());
                    stmtEquipe.setLong(5, membro.getCpf());
                } else {
                    stmtEquipe.setLong(4, membro.getCpf());
                }
                stmtEquipe.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar membro da equipe: " + e.getMessage());
            rollbackSilencioso(conn);
            return false;
        } finally {
            fecharSilencioso(conn);
        }
    }

    public boolean excluir(long cpf) {
        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement s1 = conn.prepareStatement("DELETE FROM EquipeTecnica WHERE cpf_equipe = ?")) {
                s1.setLong(1, cpf);
                if (s1.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }
            try (PreparedStatement s2 = conn.prepareStatement("DELETE FROM Pessoa WHERE cpf = ?")) {
                s2.setLong(1, cpf);
                s2.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao excluir membro da equipe: " + e.getMessage());
            rollbackSilencioso(conn);
            return false;
        } finally {
            fecharSilencioso(conn);
        }
    }

    private EquipeTecnica mapear(ResultSet rs) throws SQLException {
        EquipeTecnica e = new EquipeTecnica();
        e.setCpf(rs.getLong("cpf"));
        e.setNomeCompleto(rs.getString("nome_completo"));
        e.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());
        e.setContato(rs.getString("contato"));
        e.setEmail(rs.getString("email"));
        e.setLogin(rs.getString("login"));
        e.setCargoFuncao(rs.getString("cargo_funcao"));
        e.setNivelAcesso(NivelAcesso.fromCodigo(rs.getString("nivel_acesso"))); // Enum corrigido
        return e;
    }

    private void rollbackSilencioso(Connection conn) {
        try { if (conn != null) conn.rollback(); }
        catch (SQLException ex) { System.err.println("Erro ao fazer rollback: " + ex.getMessage()); }
    }

    private void fecharSilencioso(Connection conn) {
        try {
            if (conn != null) { conn.setAutoCommit(true); conn.close(); }
        } catch (SQLException ex) { System.err.println("Erro ao fechar conexão: " + ex.getMessage()); }
    }
}