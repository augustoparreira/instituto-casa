package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Saude;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class SaudeDAO {

    public boolean inserir(Saude saude) {
        String sql = "INSERT INTO Saude (id_fichaSaude, ubs_referencia, uso_spa, observacoes, substancias_utilizadas, cpf_adolescente) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, saude.getIdFichaSaude());
            stmt.setString(2, saude.getUbsReferencia());
            stmt.setBoolean(3, saude.isUsoSpa());
            stmt.setString(4, saude.getObservacoes());

            if (saude.isUsoSpa() && saude.getSubstanciasUtilizadas() != null) {
                stmt.setString(5, saude.getSubstanciasUtilizadas());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }

            stmt.setLong(6, saude.getCpfAdolescente());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar os dados de saúde: " + e.getMessage());
            return false;
        }
    }
}