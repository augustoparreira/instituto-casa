package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.service.*;
import br.edu.unespar.trabalho.util.Sessao;

/** Recusa sem sessão e matriz por perfil autenticado na suíte JDBC. Executar com -ea. */
public class VerificacaoAutorizacao {
    public static void main(String[] args) {
        Sessao.limparSessao();
        for (Operacao op : Operacao.values()) assert !AutorizacaoService.pode(op);
        try { AutorizacaoService.exigir(Operacao.USUARIO_GERENCIAR); throw new AssertionError(); }
        catch (AcessoNegadoException esperado) { }
        System.out.println("OK: todas as operações negadas sem sessão.");
    }

    static void verificarPerfil(NivelAcesso perfil) {
        assert Sessao.getUsuarioLogado().getPerfil() == perfil;
        assert AutorizacaoService.pode(Operacao.FREQUENCIA_REGISTRAR);
        assert AutorizacaoService.pode(Operacao.USUARIO_GERENCIAR) == (perfil == NivelAcesso.ADMINISTRADOR);
        for (Operacao op : Operacao.values()) {
            boolean permitido = switch (perfil) {
                case ADMINISTRADOR -> true;
                case EQUIPE_TECNICA -> op != Operacao.EQUIPE_CONSULTAR && op != Operacao.USUARIO_GERENCIAR;
                case EDUCADOR -> op == Operacao.FREQUENCIA_CONSULTAR || op == Operacao.FREQUENCIA_REGISTRAR
                        || op == Operacao.SENHA_PROPRIA_ALTERAR;
            };
            assert AutorizacaoService.pode(op) == permitido : perfil + " · " + op;
            assert AutorizacaoService.pode(op, new ContextoAcesso(2L, 1, 1L, true))
                    == (permitido && perfil != NivelAcesso.EDUCADOR) : perfil + " · sigilo · " + op;
        }
        assert !AutorizacaoService.pode(null);
    }
}
