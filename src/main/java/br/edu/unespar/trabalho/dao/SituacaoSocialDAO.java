package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.SituacaoSocial;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    // Buscar situação social
    public SituacaoSocial buscarPorCpf(long cpfAdolescente) {
        String sql = "SELECT * FROM SituacaoSocial WHERE cpf_adolescente = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, cpfAdolescente);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    SituacaoSocial ss = new SituacaoSocial();
                    ss.setIdSituacaoSocial(rs.getInt("id_situacao_social"));
                    ss.setRendaFamiliar(rs.getDouble("renda_familiar"));
                    ss.setBeneficioSocial(rs.getString("beneficio_social"));
                    ss.setCrasReferencia(rs.getString("cras_referencia"));
                    ss.setCpfAdolescente(rs.getLong("cpf_adolescente"));
                    return ss;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar situação social: " + e.getMessage());
        }
        return null;
    }

    // Atualizar situação social
    public boolean atualizar(SituacaoSocial ss) {
        String sql = "UPDATE SituacaoSocial SET renda_familiar = ?, beneficio_social = ?, cras_referencia = ? WHERE cpf_adolescente = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, ss.getRendaFamiliar());
            stmt.setString(2, ss.getBeneficioSocial());
            stmt.setString(3, ss.getCrasReferencia());
            stmt.setLong(4, ss.getCpfAdolescente());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar situação social: " + e.getMessage());
            return false;
        }
    }
}