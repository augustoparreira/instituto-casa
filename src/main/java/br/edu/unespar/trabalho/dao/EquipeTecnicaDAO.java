package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.EquipeTecnica;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EquipeTecnicaDAO {

    public EquipeTecnica autenticar(String login, String senha) {
        // Agora buscamos os dados nas duas tabelas para preencher o DTO do frontend
        String sql = "SELECT p.cpf, p.nome_completo, e.login, e.cargo_funcao, e.nivel_acesso " +
                "FROM Pessoa p " +
                "INNER JOIN EquipeTecnica e ON p.cpf = e.cpf_equipe " +
                "WHERE e.login = ? AND e.senha = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            stmt.setString(2, senha);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    EquipeTecnica user = new EquipeTecnica();
                    user.setCpf(rs.getLong("cpf"));
                    user.setNomeCompleto(rs.getString("nome_completo"));
                    user.setLogin(rs.getString("login"));
                    user.setCargoFuncao(rs.getString("cargo_funcao"));
                    user.setNivelAcesso(rs.getString("nivel_acesso"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
        }

        return null;
    }
}