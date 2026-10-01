package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Acompanhamento;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AcompanhamentoDAO {

    public boolean vincular(Acompanhamento acompanhamento) {
        String sql = "INSERT INTO Acompanhamento (cpf_adolescente, cpf_equipe, tecnico_referencia) VALUES (?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, acompanhamento.getCpfAdolescente());
            stmt.setLong(2, acompanhamento.getCpfEquipe());

            if (acompanhamento.getTecnicoReferencia() != null) {
                stmt.setBoolean(3, acompanhamento.getTecnicoReferencia());
            } else {
                stmt.setNull(3, Types.BOOLEAN);
            }

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao vincular acompanhamento técnico: " + e.getMessage());
            return false;
        }
    }
}