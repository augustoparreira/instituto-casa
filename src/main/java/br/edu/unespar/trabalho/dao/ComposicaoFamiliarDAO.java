package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.ComposicaoFamiliar;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class ComposicaoFamiliarDAO {

    public boolean inserir(ComposicaoFamiliar cf) {
        String sql = "INSERT INTO ComposicaoFamiliar (id_composicaoFamiliar, nome, parentesco, idade, renda, escolaridade, profissao, cpf_adolescente) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, cf.getIdComposicaoFamiliar());

            if (cf.getNome() != null) stmt.setString(2, cf.getNome());
            else stmt.setNull(2, Types.VARCHAR);

            if (cf.getParentesco() != null) stmt.setString(3, cf.getParentesco());
            else stmt.setNull(3, Types.VARCHAR);

            if (cf.getIdade() != null) stmt.setInt(4, cf.getIdade());
            else stmt.setNull(4, Types.INTEGER);

            if (cf.getRenda() != null) stmt.setDouble(5, cf.getRenda());
            else stmt.setNull(5, Types.DECIMAL);

            if (cf.getEscolaridade() != null) stmt.setString(6, cf.getEscolaridade());
            else stmt.setNull(6, Types.VARCHAR);

            if (cf.getProfissao() != null) stmt.setString(7, cf.getProfissao());
            else stmt.setNull(7, Types.VARCHAR);

            stmt.setLong(8, cf.getCpfAdolescente());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar composição familiar: " + e.getMessage());
            return false;
        }
    }
}