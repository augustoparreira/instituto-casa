package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Documento;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

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

    // Caso de uso "Atualizar Status de Documentação"
    public boolean atualizarStatus(int idDocumento, String novoStatus) {
        String sql = "UPDATE Documento SET status_envio = ? WHERE id_documento = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (novoStatus != null) stmt.setString(1, novoStatus);
            else stmt.setNull(1, Types.VARCHAR);
            stmt.setInt(2, idDocumento);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar status do documento: " + e.getMessage());
            return false;
        }
    }

    public List<Documento> listarPorAdolescente(long cpfAdolescente) {
        List<Documento> lista = new ArrayList<>();
        String sql = "SELECT id_documento, tipo, data_geracao, status_envio, cpf_adolescente " +
                "FROM Documento WHERE cpf_adolescente = ? ORDER BY data_geracao DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfAdolescente);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Documento d = new Documento();
                    d.setIdDocumento(rs.getInt("id_documento"));
                    d.setTipo(rs.getString("tipo"));
                    d.setDataGeracao(rs.getDate("data_geracao").toLocalDate());
                    d.setStatusEnvio(rs.getString("status_envio"));
                    d.setCpfAdolescente(rs.getLong("cpf_adolescente"));
                    lista.add(d);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar documentos: " + e.getMessage());
        }
        return lista;
    }
}