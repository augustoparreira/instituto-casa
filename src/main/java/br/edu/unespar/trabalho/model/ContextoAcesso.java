package br.edu.unespar.trabalho.model;

/** Contexto obtido de dados confiáveis, nunca de uma opção livre da interface. */
public record ContextoAcesso(Long cpfAdolescente, Integer idDocumento, Long cpfProfissional,
                             boolean sigiloso) {
    public static ContextoAcesso adolescente(long cpf) {
        return new ContextoAcesso(cpf, null, null, false);
    }
}
