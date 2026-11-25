package br.edu.unespar.trabalho.dao;

import static br.edu.unespar.trabalho.service.AutorizacaoService.*;
import static br.edu.unespar.trabalho.model.Operacao.*;
import br.edu.unespar.trabalho.model.Atividade;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import br.edu.unespar.trabalho.util.IdUtil;
import br.edu.unespar.trabalho.util.ValidacaoFormulario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Executa operações de cadastro e consulta de atividades por JDBC.
 */
public class AtividadeDAO {

    public boolean inserir(Atividade a) {
        exigir(ATIVIDADE_GERENCIAR);

        validar(a);
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try(PreparedStatement s=c.prepareStatement("INSERT INTO Atividade(id_atividade,nome_atividade,tipo,carga_horaria) VALUES(?,?,?,?)")) {
                a.setIdAtividade(IdUtil.proximoId(c,"Atividade")); s.setInt(1,a.getIdAtividade()); s.setString(2,a.getNomeAtividade());
                s.setString(3,a.getTipo()); s.setInt(4,a.getCargaHoraria()); s.executeUpdate(); c.commit(); return true;
            } catch(SQLException|RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível cadastrar a atividade.",e); }
    }

    public List<Atividade> listar() {
        exigir(FREQUENCIA_CONSULTAR);

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
        exigir(FREQUENCIA_CONSULTAR);

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
        exigir(ATIVIDADE_GERENCIAR);

        validar(a);
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
        exigir(ATIVIDADE_GERENCIAR);

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

    private void validar(Atividade a) {
        ValidacaoFormulario.texto(a.getNomeAtividade(),25,"Nome da atividade",true);
        ValidacaoFormulario.texto(a.getTipo(),120,"Tipo de atividade",false);
        if(a.getCargaHoraria()<1 || a.getCargaHoraria()>24)
            throw new IllegalArgumentException("Informe uma carga horária de 1 a 24 horas.");
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