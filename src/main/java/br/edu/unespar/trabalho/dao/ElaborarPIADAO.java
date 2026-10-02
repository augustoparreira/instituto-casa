package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.ElaborarPIA;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ElaborarPIADAO {

    // Tabela associativa pura (cpf_equipe, id_pia). A data de elaboração fica na tabela PIA.
    public boolean registrarAutoria(ElaborarPIA elaboracao) {
        String sql = "INSERT INTO ElaborarPIA (cpf_equipe, id_pia) VALUES (?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, elaboracao.getCpfEquipe());
            stmt.setInt(2, elaboracao.getIdPia());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao registrar autoria do PIA: " + e.getMessage());
            return false;
        }
    }
}