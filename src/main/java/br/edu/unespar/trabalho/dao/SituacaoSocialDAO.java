package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.SituacaoSocial;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class SituacaoSocialDAO {

    public boolean inserir(SituacaoSocial ss) {
        String sql = "INSERT INTO SituacaoSocial (id_situacaoSocial, renda, beneficios_sociais, endereco, bairro, telefone, numero_nis, cras_referencia, cpf_adolescente) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ss.getIdSituacaoSocial());
            stmt.setDouble(2, ss.getRendaFamiliar());
            stmt.setString(3, ss.getBeneficioSocial());
            stmt.setString(4, ss.getEndereco()); // Campo novo adicionado
            stmt.setString(5, ss.getBairro()); // Campo novo adicionado
            stmt.setString(6, ss.getTelefone()); // Campo novo adicionado
            stmt.setLong(7, ss.getNumeroNis()); // Campo novo adicionado

            if (ss.getCrasReferencia() != null) {
                stmt.setInt(8, ss.getCrasReferencia());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }

            stmt.setLong(9, ss.getCpfAdolescente());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar os dados de situação social: " + e.getMessage());
            return false;
        }
    }

    public SituacaoSocial buscarPorCpf(long cpfAdolescente) {
        String sql = "SELECT * FROM SituacaoSocial WHERE cpf_adolescente = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, cpfAdolescente);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    SituacaoSocial ss = new SituacaoSocial();
                    ss.setIdSituacaoSocial(rs.getInt("id_situacaoSocial"));
                    ss.setRendaFamiliar(rs.getDouble("renda"));
                    ss.setBeneficioSocial(rs.getString("beneficios_sociais"));
                    ss.setEndereco(rs.getString("endereco")); // Campo novo adicionado
                    ss.setBairro(rs.getString("bairro")); // Campo novo adicionado
                    ss.setTelefone(rs.getString("telefone")); // Campo novo adicionado
                    ss.setCrasNome(rs.getString("cras_nome"));
                    ss.setNumeroNis(rs.getLong("numero_nis")); // Campo novo adicionado

                    int cras = rs.getInt("cras_referencia");
                    ss.setCrasReferencia(rs.wasNull() ? null : cras);

                    ss.setCpfAdolescente(rs.getLong("cpf_adolescente"));
                    return ss;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível carregar os dados cadastrais.", e);
        }
        return null;
    }

    public boolean atualizar(SituacaoSocial ss) {
        String sql = "UPDATE SituacaoSocial SET renda = ?, beneficios_sociais = ?, endereco = ?, bairro = ?, telefone = ?, numero_nis = ?, cras_referencia = ? WHERE cpf_adolescente = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, ss.getRendaFamiliar());
            stmt.setString(2, ss.getBeneficioSocial());
            stmt.setString(3, ss.getEndereco()); // Campo novo adicionado
            stmt.setString(4, ss.getBairro()); // Campo novo adicionado
            stmt.setString(5, ss.getTelefone()); // Campo novo adicionado
            stmt.setLong(6, ss.getNumeroNis()); // Campo novo adicionado

            if (ss.getCrasReferencia() != null) {
                stmt.setInt(7, ss.getCrasReferencia());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            stmt.setLong(8, ss.getCpfAdolescente());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar situação social: " + e.getMessage());
            return false;
        }
    }
}