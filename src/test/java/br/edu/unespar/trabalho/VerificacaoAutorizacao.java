package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.service.*;
import br.edu.unespar.trabalho.util.Sessao;

/** Contrato de permissões sem acesso ao banco. Executar com -ea. */
public class VerificacaoAutorizacao {
    public static void main(String[] args) {
        Sessao.limparSessao();
        for (Operacao op : Operacao.values()) assert !AutorizacaoService.pode(op);
        for (NivelAcesso perfil : NivelAcesso.values()) {
            Sessao.setUsuarioLogado(new EquipeTecnicaDTO(1L, "teste", "Profissional", "Cargo", perfil));
            assert AutorizacaoService.pode(Operacao.FREQUENCIA_REGISTRAR);
            assert AutorizacaoService.pode(Operacao.USUARIO_GERENCIAR) == (perfil == NivelAcesso.ADMINISTRADOR);
            for (Operacao op : Operacao.values()) {
                if (perfil == NivelAcesso.ADMINISTRADOR) assert AutorizacaoService.pode(op);
                assert AutorizacaoService.pode(op, new ContextoAcesso(2L, 1, 1L, true))
                        == (perfil == NivelAcesso.ADMINISTRADOR);
            }
            assert !AutorizacaoService.pode(null);
        }
        Sessao.setUsuarioLogado(new EquipeTecnicaDTO(1L, "teste", "Cargo", "invalido"));
        assert !AutorizacaoService.pode(Operacao.FREQUENCIA_REGISTRAR);
        try { AutorizacaoService.exigir(Operacao.USUARIO_GERENCIAR); throw new AssertionError(); }
        catch (AcessoNegadoException esperado) { }
        Sessao.limparSessao();
        assert !AutorizacaoService.pode(Operacao.FREQUENCIA_REGISTRAR);
        System.out.println("OK: contrato de autorização, perfis, contexto sigiloso e sessão ausente.");
    }
}
