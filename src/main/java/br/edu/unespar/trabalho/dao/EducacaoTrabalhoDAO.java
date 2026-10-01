package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.EducacaoTrabalho;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;

public class EducacaoTrabalhoDAO {

    public boolean inserir(EducacaoTrabalho et) {
        String sql = "INSERT INTO EducacaoTrabalho (id_educacao_trabalho, estuda, escola, serie, trabalha, funcao, vinculo_empregaticio, cpf_adolescente) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, et.getIdEducacaoTrabalho());
            stmt.setBoolean(2, et.isEstuda());

            if (et.isEstuda() && et.getEscola() != null) {
                stmt.setString(3, et.getEscola());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }

            if (et.isEstuda() && et.getSerie() != null) {
                stmt.setString(4, et.getSerie());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }

            stmt.setBoolean(5, et.isTrabalha());

            if (et.isTrabalha() && et.getFuncao() != null) {
                stmt.setString(6, et.getFuncao());
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }

            if (et.isTrabalha() && et.getVinculoEmpregaticio() != null) {
                stmt.setString(7, et.getVinculoEmpregaticio());
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }

            stmt.setLong(8, et.getCpfAdolescente());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar dados de educação e trabalho: " + e.getMessage());
            return false;
        }
    }

    // Buscar dados de educação e trabalho
    public EducacaoTrabalho buscarPorCpf(long cpfAdolescente) {
        String sql = "SELECT * FROM EducacaoTrabalho WHERE cpf_adolescente = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, cpfAdolescente);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    EducacaoTrabalho et = new EducacaoTrabalho();
                    et.setIdEducacaoTrabalho(rs.getInt("id_educacao_trabalho"));
                    et.setEstuda(rs.getBoolean("estuda"));
                    et.setEscola(rs.getString("escola"));
                    et.setSerie(rs.getString("serie"));
                    et.setTrabalha(rs.getBoolean("trabalha"));
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

    // Atualizar dados de educação e trabalho
    public boolean atualizar(EducacaoTrabalho et) {
        String sql = "UPDATE EducacaoTrabalho SET estuda = ?, escola = ?, serie = ?, trabalha = ?, funcao = ?, vinculo_empregaticio = ? WHERE cpf_adolescente = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, et.isEstuda());
            if (et.isEstuda() && et.getEscola() != null) stmt.setString(2, et.getEscola()); else stmt.setNull(2, Types.VARCHAR);
            if (et.isEstuda() && et.getSerie() != null) stmt.setString(3, et.getSerie()); else stmt.setNull(3, Types.VARCHAR);

            stmt.setBoolean(4, et.isTrabalha());
            if (et.isTrabalha() && et.getFuncao() != null) stmt.setString(5, et.getFuncao()); else stmt.setNull(5, Types.VARCHAR);
            if (et.isTrabalha() && et.getVinculoEmpregaticio() != null) stmt.setString(6, et.getVinculoEmpregaticio()); else stmt.setNull(6, Types.VARCHAR);

            stmt.setLong(7, et.getCpfAdolescente());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar educação/trabalho: " + e.getMessage());
            return false;
        }
    }
}