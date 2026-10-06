package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Atividade;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import br.edu.unespar.trabalho.util.IdUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AtividadeDAO {

    public boolean inserir(Atividade a) {
        if(a.getNomeAtividade()==null || a.getNomeAtividade().isBlank() || a.getNomeAtividade().length()>25 || a.getCargaHoraria()<1 || a.getCargaHoraria()>24)
            throw new IllegalArgumentException("Confira o nome da atividade e a carga horária (1 a 24).");
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try(PreparedStatement s=c.prepareStatement("INSERT INTO Atividade(id_atividade,nome_atividade,tipo,carga_horaria) VALUES(?,?,?,?)")) {
                a.setIdAtividade(IdUtil.proximoId(c,"Atividade")); s.setInt(1,a.getIdAtividade()); s.setString(2,a.getNomeAtividade());
                s.setString(3,a.getTipo()); s.setInt(4,a.getCargaHoraria()); s.executeUpdate(); c.commit(); return true;
            } catch(SQLException|RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível cadastrar a atividade.",e); }
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