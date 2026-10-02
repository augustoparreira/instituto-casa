package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.MedidaSocioeducativa;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;

public class MedidaSocioeducativaDAO {

    public boolean inserir(MedidaSocioeducativa medida) {
        String sql = "INSERT INTO MedidaSocioeducativa (id_medida, reincidencia, tipo_medida, data_inicio, historico_infracional, duracao_meses, duracao_horas, cpf_adolescente) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, medida.getIdMedida());
            stmt.setBoolean(2, medida.isReincidencia());
            stmt.setString(3, medida.getTipoMedida() != null ? medida.getTipoMedida().getCodigo() : null); // Enum corrigido
            stmt.setDate(4, Date.valueOf(medida.getDataInicio()));
            stmt.setString(5, medida.getHistoricoInfracional());

            if (medida.getDuracaoMeses() != null) stmt.setInt(6, medida.getDuracaoMeses());
            else stmt.setNull(6, Types.INTEGER);

            if (medida.getDuracaoHoras() != null) stmt.setInt(7, medida.getDuracaoHoras());
            else stmt.setNull(7, Types.INTEGER);

            stmt.setLong(8, medida.getCpfAdolescente());

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir medida: " + e.getMessage());
            return false;
        }
    }

    public int consultarHorasCumpridas(long cpfAdolescente) {
        String sql = "SELECT SUM(horas_cumpridas) AS total_horas FROM Frequencia WHERE cpf_adolescente = ? AND status_presenca = 'PRESENTE'";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfAdolescente);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_horas");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao calcular progresso: " + e.getMessage());
        }
        return 0;
    }
}