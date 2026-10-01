package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.SituacaoSocial;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SituacaoSocialDAO {

    public boolean inserir(SituacaoSocial ss) {
        String sql = "INSERT INTO SituacaoSocial (id_situacao_social, renda_familiar, beneficio_social, cras_referencia, cpf_adolescente) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ss.getIdSituacaoSocial());
            stmt.setDouble(2, ss.getRendaFamiliar());
            stmt.setString(3, ss.getBeneficioSocial());
            stmt.setString(4, ss.getCrasReferencia());
            stmt.setLong(5, ss.getCpfAdolescente());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar os dados de situação social: " + e.getMessage());
            return false;
        }
    }
}