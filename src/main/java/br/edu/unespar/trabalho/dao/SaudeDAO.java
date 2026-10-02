package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Saude;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;

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

    // Buscar dados de saúde do adolescente
    public Saude buscarPorCpf(long cpfAdolescente) {
        String sql = "SELECT * FROM Saude WHERE cpf_adolescente = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, cpfAdolescente);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Saude saude = new Saude();
                    saude.setIdFichaSaude(rs.getInt("id_fichaSaude"));
                    saude.setUbsReferencia(rs.getString("ubs_referencia"));
                    saude.setUsoSpa(rs.getBoolean("uso_spa"));
                    saude.setObservacoes(rs.getString("observacoes"));
                    saude.setSubstanciasUtilizadas(rs.getString("substancias_utilizadas"));
                    saude.setCpfAdolescente(rs.getLong("cpf_adolescente"));
                    return saude;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar saúde: " + e.getMessage());
        }
        return null;
    }

    // Atualizar dados de saúde
    public boolean atualizar(Saude saude) {
        String sql = "UPDATE Saude SET ubs_referencia = ?, uso_spa = ?, observacoes = ?, substancias_utilizadas = ? WHERE cpf_adolescente = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, saude.getUbsReferencia());
            stmt.setBoolean(2, saude.isUsoSpa());
            stmt.setString(3, saude.getObservacoes());
            if (saude.isUsoSpa() && saude.getSubstanciasUtilizadas() != null) {
                stmt.setString(4, saude.getSubstanciasUtilizadas());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }
            stmt.setLong(5, saude.getCpfAdolescente());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar saúde: " + e.getMessage());
            return false;
        }
    }
}