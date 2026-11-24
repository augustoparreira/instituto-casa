package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.service.AutorizacaoService;
import static br.edu.unespar.trabalho.model.Operacao.*;

import br.edu.unespar.trabalho.model.Responsabiliza;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Registra por JDBC o vínculo entre o adolescente e seu responsável.
 */
public class ResponsabilizaDAO {

    public boolean inserir(Responsabiliza vinculo) {
        AutorizacaoService.exigir(FAMILIA_ALTERAR);
        if (vinculo.getCpfResponsavel()==vinculo.getCpfAdolescente())
            throw new IllegalArgumentException("O responsável não pode ser o próprio adolescente.");
        String sql = "INSERT INTO Responsabiliza (cpf_responsavel, cpf_adolescente) VALUES (?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            try {
                try (PreparedStatement adolescente=conn.prepareStatement("SELECT 1 FROM Adolescente WHERE cpf_adolescente=? FOR UPDATE")) {
                    adolescente.setLong(1,vinculo.getCpfAdolescente());
                    try (var resultado=adolescente.executeQuery()) {
                        if (!resultado.next()) throw new IllegalArgumentException("Adolescente não encontrado.");
                    }
                }
                if (!new ResponsavelDAO().bloquearResponsavelReutilizavel(conn,vinculo.getCpfResponsavel()))
                    throw new IllegalArgumentException("CPF indisponível para vínculo de responsável.");
                stmt.setLong(1, vinculo.getCpfResponsavel());
                stmt.setLong(2, vinculo.getCpfAdolescente());
                stmt.executeUpdate();
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("Erro ao gravar vínculo de responsabilidade: " + e.getMessage());
            return false;
        }
    }
}