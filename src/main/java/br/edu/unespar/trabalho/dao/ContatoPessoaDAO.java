package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.util.MascarasFormulario;
import br.edu.unespar.trabalho.util.ValidacaoFormulario;
import java.sql.Connection;
import java.sql.SQLException;

/** Valida o contato de Pessoa na mesma transação que grava o cadastro. */
final class ContatoPessoaDAO {
    private ContatoPessoaDAO() { }

    static String validar(Connection conexao, long cpf, String telefone, boolean edicao, boolean obrigatorio)
            throws SQLException {
        ValidacaoFormulario.texto(telefone,30,"Telefone",obrigatorio);
        if (telefone==null || telefone.isBlank() || MascarasFormulario.telefoneValido(telefone)) return telefone;
        if (edicao) {
            try (var consulta=conexao.prepareStatement("SELECT contato FROM Pessoa WHERE cpf=? FOR UPDATE")) {
                consulta.setLong(1,cpf);
                try (var resultado=consulta.executeQuery()) {
                    if (resultado.next()) {
                        String original = resultado.getString(1);
                        if (MascarasFormulario.telefoneValidoOuExistente(telefone,original)) return original;
                    }
                }
            }
        }
        throw new IllegalArgumentException(MascarasFormulario.TELEFONE_INVALIDO);
    }
}
