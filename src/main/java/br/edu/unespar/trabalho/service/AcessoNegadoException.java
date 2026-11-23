package br.edu.unespar.trabalho.service;

/** Falha de autorização que a interface deve apresentar sem executar a operação. */
public class AcessoNegadoException extends SecurityException {
    public AcessoNegadoException() {
        super("Sessão inválida ou operação não autorizada para este perfil.");
    }
}
