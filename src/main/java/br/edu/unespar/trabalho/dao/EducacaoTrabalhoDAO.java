package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.EducacaoTrabalho;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;

public class EducacaoTrabalhoDAO {

    public boolean inserir(EducacaoTrabalho et) {
        String sql = "INSERT INTO EducacaoTrabalho (id_educacaoTrabalho, estuda, escola, ano_serie, trabalha, local_trabalho, funcao, vinculo_empregaticio, cpf_adolescente) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, et.getIdEducacaoTrabalho());
            stmt.setBoolean(2, et.isEstuda());
            setStringOuNulo(stmt, 3, et.isEstuda() ? et.getEscola() : null);
            setStringOuNulo(stmt, 4, et.isEstuda() ? et.getSerie() : null);
            stmt.setBoolean(5, et.isTrabalha());
            setStringOuNulo(stmt, 6, et.isTrabalha() ? et.getLocalTrabalho() : null);
            setStringOuNulo(stmt, 7, et.isTrabalha() ? et.getFuncao() : null);
            setStringOuNulo(stmt, 8, et.isTrabalha() ? et.getVinculoEmpregaticio() : null);
            stmt.setLong(9, et.getCpfAdolescente());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar dados de educação e trabalho: " + e.getMessage());
            return false;
        }
    }

    public EducacaoTrabalho buscarPorCpf(long cpfAdolescente) {
        String sql = "SELECT * FROM EducacaoTrabalho WHERE cpf_adolescente = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, cpfAdolescente);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    EducacaoTrabalho et = new EducacaoTrabalho();
                    et.setIdEducacaoTrabalho(rs.getInt("id_educacaoTrabalho"));
                    et.setEstuda(rs.getBoolean("estuda"));
                    et.setEscola(rs.getString("escola"));
                    et.setSerie(rs.getString("ano_serie"));
                    et.setTrabalha(rs.getBoolean("trabalha"));
                    et.setLocalTrabalho(rs.getString("local_trabalho"));
                    et.setFuncao(rs.getString("funcao"));
                    et.setVinculoEmpregaticio(rs.getString("vinculo_empregaticio"));
                    et.setCpfAdolescente(rs.getLong("cpf_adolescente"));
                    return et;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar dados de educação/trabalho: " + e.getMessage());
        }
        return null;
    }

    public boolean atualizar(EducacaoTrabalho et) {
        String sql = "UPDATE EducacaoTrabalho SET estuda = ?, escola = ?, ano_serie = ?, trabalha = ?, local_trabalho = ?, funcao = ?, vinculo_empregaticio = ? WHERE cpf_adolescente = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, et.isEstuda());
            setStringOuNulo(stmt, 2, et.isEstuda() ? et.getEscola() : null);
            setStringOuNulo(stmt, 3, et.isEstuda() ? et.getSerie() : null);
            stmt.setBoolean(4, et.isTrabalha());
            setStringOuNulo(stmt, 5, et.isTrabalha() ? et.getLocalTrabalho() : null);
            setStringOuNulo(stmt, 6, et.isTrabalha() ? et.getFuncao() : null);
            setStringOuNulo(stmt, 7, et.isTrabalha() ? et.getVinculoEmpregaticio() : null);
            stmt.setLong(8, et.getCpfAdolescente());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar educação/trabalho: " + e.getMessage());
            return false;
        }
    }

    private void setStringOuNulo(PreparedStatement stmt, int idx, String valor) throws SQLException {
        if (valor != null) stmt.setString(idx, valor);
        else stmt.setNull(idx, Types.VARCHAR);
    }
}