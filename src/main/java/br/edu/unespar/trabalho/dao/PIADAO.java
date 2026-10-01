package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.PIA;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;

public class PIADAO {

    // 1. CREATE (Já tínhamos feito)
    public boolean inserir(PIA pia) {
        String sql = "INSERT INTO PIA (id_pia, data_elaboracao, diagnostico, vulnerabilidades, potencialidades, estrategias, documento_enviado, cpf_adolescente) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, pia.getIdPia());
            stmt.setDate(2, Date.valueOf(pia.getDataElaboracao()));
            stmt.setString(3, pia.getDiagnostico());
            stmt.setString(4, pia.getVulnerabilidades());
            stmt.setString(5, pia.getPotencialidades());
            stmt.setString(6, pia.getEstrategias());
            stmt.setBoolean(7, pia.isDocumentoEnviado());
            stmt.setLong(8, pia.getCpfAdolescente());

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir PIA: " + e.getMessage());
            return false;
        }
    }

    // 2. READ (Buscar o PIA de um adolescente específico para mostrar na tela)
    public PIA buscarPorCpf(long cpfAdolescente) {
        String sql = "SELECT * FROM PIA WHERE cpf_adolescente = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfAdolescente);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PIA pia = new PIA();
                    pia.setIdPia(rs.getInt("id_pia"));
                    pia.setDataElaboracao(rs.getDate("data_elaboracao").toLocalDate());
                    pia.setDiagnostico(rs.getString("diagnostico"));
                    pia.setVulnerabilidades(rs.getString("vulnerabilidades"));
                    pia.setPotencialidades(rs.getString("potencialidades"));
                    pia.setEstrategias(rs.getString("estrategias"));
                    pia.setDocumentoEnviado(rs.getBoolean("documento_enviado"));
                    pia.setCpfAdolescente(rs.getLong("cpf_adolescente"));
                    return pia;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar PIA: " + e.getMessage());
        }
        return null; // Retorna nulo se o adolescente ainda não tiver um PIA cadastrado
    }

    // 3. UPDATE (Editar o texto do PIA ou marcar documento_enviado como true)
    public boolean atualizar(PIA pia) {
        String sql = "UPDATE PIA SET diagnostico = ?, vulnerabilidades = ?, potencialidades = ?, estrategias = ?, documento_enviado = ? WHERE id_pia = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pia.getDiagnostico());
            stmt.setString(2, pia.getVulnerabilidades());
            stmt.setString(3, pia.getPotencialidades());
            stmt.setString(4, pia.getEstrategias());
            stmt.setBoolean(5, pia.isDocumentoEnviado());
            stmt.setInt(6, pia.getIdPia());

            int linhasAfetadas = stmt.executeUpdate();
            return linhasAfetadas > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar PIA: " + e.getMessage());
            return false;
        }
    }

    // 4. DELETE (Conforme exigido pelo método excluirPIA() no diagrama de classes)
    public boolean excluir(int idPia) {
        String sql = "DELETE FROM PIA WHERE id_pia = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idPia);

            int linhasAfetadas = stmt.executeUpdate();
            return linhasAfetadas > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir PIA: " + e.getMessage());
            return false;
        }
    }
}