package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Frequencia;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import java.sql.*;

public class FrequenciaDAO {

    public boolean registrar(Frequencia frequencia) {
        String sql = "INSERT INTO Frequencia (cpf_adolescente, id_atividade, data_presenca, status_presenca, horas_cumpridas) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, frequencia.getCpfAdolescente());
            stmt.setInt(2, frequencia.getIdAtividade());
            stmt.setDate(3, Date.valueOf(frequencia.getDataPresenca()));
            stmt.setString(4, frequencia.getStatusPresenca() != null ? frequencia.getStatusPresenca().getCodigo() : null); // Enum corrigido

            if (frequencia.getHorasCumpridas() != null) {
                stmt.setInt(5, frequencia.getHorasCumpridas());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao registrar frequência: " + e.getMessage());
            return false;
        }
    }

    public int consultarFaltas(long cpfAdolescente) {
        // Query corrigida para ler os dois estados de falta definidos no teu Enum StatusPresenca
        String sql = "SELECT COUNT(*) AS total_faltas FROM Frequencia WHERE cpf_adolescente = ? AND status_presenca IN ('FALTA_JUST', 'FALTA_INJ')";
        int faltas = 0;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfAdolescente);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    faltas = rs.getInt("total_faltas");
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro ao consultar faltas: " + e.getMessage());
        }

        return faltas;
    }
}