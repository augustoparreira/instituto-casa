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
            stmt.setString(4, frequencia.getStatusPresenca());

            if (frequencia.getHorasCumpridas() != null) {
                stmt.setInt(5, frequencia.getHorasCumpridas());
            } else {
                stmt.setNull(5, Types.INTEGER); // Se for falta, manda nulo para o banco
            }

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao registrar frequência: " + e.getMessage());
            return false;
        }
    }
}