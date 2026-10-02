package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Atividade;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AtividadeDAO {

    public boolean inserir(Atividade a) {
        String sql = "INSERT INTO Atividade (id_atividade, nome_atividade, tipo, carga_horaria) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, a.getIdAtividade());
            stmt.setString(2, a.getNomeAtividade());
            if (a.getTipo() != null) stmt.setString(3, a.getTipo());
            else stmt.setNull(3, Types.VARCHAR);
            stmt.setInt(4, a.getCargaHoraria());

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao incluir atividade: " + e.getMessage());
            return false;
        }
    }

    public List<Atividade> listar() {
        List<Atividade> lista = new ArrayList<>();
        String sql = "SELECT id_atividade, nome_atividade, tipo, carga_horaria FROM Atividade ORDER BY nome_atividade";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.err.println("Erro ao listar atividades: " + e.getMessage());
        }
        return lista;
    }

    public Atividade buscarPorId(int idAtividade) {
        String sql = "SELECT id_atividade, nome_atividade, tipo, carga_horaria FROM Atividade WHERE id_atividade = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAtividade);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao consultar atividade: " + e.getMessage());
        }
        return null;
    }

    public boolean atualizar(Atividade a) {
        String sql = "UPDATE Atividade SET nome_atividade = ?, tipo = ?, carga_horaria = ? WHERE id_atividade = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.getNomeAtividade());
            if (a.getTipo() != null) stmt.setString(2, a.getTipo());
            else stmt.setNull(2, Types.VARCHAR);
            stmt.setInt(3, a.getCargaHoraria());
            stmt.setInt(4, a.getIdAtividade());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar atividade: " + e.getMessage());
            return false;
        }
    }

    // Falha (retorna false) se já existir frequência registrada para a atividade (FK).
    public boolean excluir(int idAtividade) {
        String sql = "DELETE FROM Atividade WHERE id_atividade = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAtividade);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir atividade: " + e.getMessage());
            return false;
        }
    }

    private Atividade mapear(ResultSet rs) throws SQLException {
        Atividade a = new Atividade();
        a.setIdAtividade(rs.getInt("id_atividade"));
        a.setNomeAtividade(rs.getString("nome_atividade"));
        a.setTipo(rs.getString("tipo"));
        a.setCargaHoraria(rs.getInt("carga_horaria"));
        return a;
    }
}