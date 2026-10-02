package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Responsabiliza;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ResponsabilizaDAO {

    public boolean inserir(Responsabiliza vinculo) {
        String sql = "INSERT INTO Responsabiliza (cpf_responsavel, cpf_adolescente) VALUES (?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, vinculo.getCpfResponsavel());
            stmt.setLong(2, vinculo.getCpfAdolescente());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao gravar vínculo de responsabilidade: " + e.getMessage());
            return false;
        }
    }
}