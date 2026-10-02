package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Acompanhamento;
import br.edu.unespar.trabalho.model.Adolescente;
import br.edu.unespar.trabalho.model.StatusAdolescente;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AcompanhamentoDAO {

    public boolean vincular(Acompanhamento acompanhamento) {
        String sql = "INSERT INTO Acompanhamento (cpf_adolescente, cpf_equipe, tecnico_referencia) VALUES (?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, acompanhamento.getCpfAdolescente());
            stmt.setLong(2, acompanhamento.getCpfEquipe());
            stmt.setBoolean(3, acompanhamento.isTecnicoReferencia()); // Corrigido para isTecnicoReferencia()

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao vincular acompanhamento técnico: " + e.getMessage());
            return false;
        }
    }

    public List<Adolescente> listarPorTecnico(long cpfEquipe) {
        List<Adolescente> lista = new ArrayList<>();

        String sql = "SELECT p.cpf, p.nome_completo, p.data_nascimento, p.contato, p.email, " +
                "a.naturalidade, a.genero, a.cor_raca, a.status " +
                "FROM Acompanhamento ac " +
                "INNER JOIN Adolescente a ON ac.cpf_adolescente = a.cpf_adolescente " +
                "INNER JOIN Pessoa p ON p.cpf = a.cpf_adolescente " +
                "WHERE ac.cpf_equipe = ? AND a.status <> 'INATIVO' " +
                "ORDER BY p.nome_completo";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfEquipe);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Adolescente jovem = new Adolescente();
                    jovem.setCpf(rs.getLong("cpf"));
                    jovem.setNomeCompleto(rs.getString("nome_completo"));
                    jovem.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());
                    jovem.setContato(rs.getString("contato"));
                    jovem.setEmail(rs.getString("email"));
                    jovem.setNaturalidade(rs.getString("naturalidade"));
                    jovem.setGenero(rs.getString("genero"));
                    jovem.setCorRaca(rs.getString("cor_raca"));
                    jovem.setStatus(StatusAdolescente.fromCodigo(rs.getString("status"))); // Enum corrigido
                    lista.add(jovem);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar jovens do técnico: " + e.getMessage());
        }
        return lista;
    }
}