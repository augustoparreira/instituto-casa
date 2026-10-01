package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.EducacaoTrabalho;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

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
}