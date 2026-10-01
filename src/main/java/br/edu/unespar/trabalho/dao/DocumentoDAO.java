package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Documento;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class DocumentoDAO {

    public boolean inserir(Documento doc) {
        String sql = "INSERT INTO Documento (id_documento, tipo, data_geracao, status_envio, cpf_adolescente) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, doc.getIdDocumento());

            if (doc.getTipo() != null) stmt.setString(2, doc.getTipo());
            else stmt.setNull(2, Types.VARCHAR);

            stmt.setDate(3, Date.valueOf(doc.getDataGeracao()));

            if (doc.getStatusEnvio() != null) stmt.setString(4, doc.getStatusEnvio());
            else stmt.setNull(4, Types.VARCHAR);

            stmt.setLong(5, doc.getCpfAdolescente());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar documento: " + e.getMessage());
            return false;
        }
    }
}