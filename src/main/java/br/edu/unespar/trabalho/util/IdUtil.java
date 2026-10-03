package br.edu.unespar.trabalho.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/** Gera o próximo ID (MAX + 1) das tabelas que não têm auto-incremento. */
public class IdUtil {

    private static final Map<String, String> COLUNA_ID = Map.of(
            "MedidaSocioeducativa", "id_medida",
            "PIA", "id_pia",
            "Saude", "id_fichaSaude",
            "SituacaoSocial", "id_situacaoSocial",
            "EducacaoTrabalho", "id_educacaoTrabalho",
            "ComposicaoFamiliar", "id_composicaoFamiliar",
            "Atividade", "id_atividade",
            "Documento", "id_documento"
    );

    public static int proximoId(String tabela) {
        String coluna = COLUNA_ID.get(tabela);
        if (coluna == null) {
            throw new IllegalArgumentException("Tabela sem ID manual cadastrada: " + tabela);
        }
        String sql = "SELECT COALESCE(MAX(" + coluna + "), 0) + 1 FROM " + tabela;
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao gerar ID para " + tabela, e);
        }
    }
}