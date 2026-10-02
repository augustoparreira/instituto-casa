package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.EmiteDocumento;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EmiteDocumentoDAO {

    public boolean inserir(EmiteDocumento emite) {
        String sql = "INSERT INTO EmiteDocumento (id_documento, cpf_equipe) VALUES (?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, emite.getIdDocumento());
            stmt.setLong(2, emite.getCpfEquipe());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao registrar emissão de documento: " + e.getMessage());
            return false;
        }
    }
}