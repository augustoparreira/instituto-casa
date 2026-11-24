package br.edu.unespar.trabalho.service;

import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.util.Sessao;
import java.util.EnumSet;
import static br.edu.unespar.trabalho.model.Operacao.*;

/** Política provisória central. Permissões indefinidas permanecem negadas. */
public final class AutorizacaoService {
    private AutorizacaoService() { }

    private static final EnumSet<Operacao> TECNICA = EnumSet.of(
            PAINEL_CONSULTAR, ADOLESCENTE_CONSULTAR, ADOLESCENTE_CADASTRAR, ADOLESCENTE_EDITAR,
            FAMILIA_CONSULTAR, FAMILIA_ALTERAR, EDUCACAO_CONSULTAR, EDUCACAO_ALTERAR,
            MEDIDA_CONSULTAR, MEDIDA_CADASTRAR, MEDIDA_EDITAR,
            FREQUENCIA_CONSULTAR, FREQUENCIA_REGISTRAR, FREQUENCIA_EDITAR, HORAS_PSC_CONSULTAR, HORAS_PSC_REGISTRAR, RELATORIO_OPERACIONAL_GERAR,
            AGENDA_CONSULTAR, AGENDA_CADASTRAR, AGENDA_EDITAR, SENHA_PROPRIA_ALTERAR);
    private static final EnumSet<Operacao> EDUCADOR = EnumSet.of(
            FREQUENCIA_CONSULTAR, FREQUENCIA_REGISTRAR, SENHA_PROPRIA_ALTERAR);

    public static boolean pode(Operacao operacao) {
        return pode(operacao, null);
    }

    public static boolean pode(Operacao operacao, ContextoAcesso contexto) {
        EquipeTecnicaDTO usuario = Sessao.getUsuarioLogado();
        if (operacao == null || usuario == null || usuario.getPerfil() == null || !Sessao.validarContaAtual()) return false;
        if (usuario.getPerfil() == NivelAcesso.ADMINISTRADOR) return true;
        // Vínculo ou autoria não são concessões de sigilo enquanto a ONG não definir a política.
        if (contexto != null && contexto.sigiloso()) return false;
        return switch (usuario.getPerfil()) {
            case EQUIPE_TECNICA -> TECNICA.contains(operacao);
            case EDUCADOR -> EDUCADOR.contains(operacao);
            default -> false;
        };
    }

    public static void exigir(Operacao operacao) { exigir(operacao, null); }

    public static void exigir(Operacao operacao, ContextoAcesso contexto) {
        if (!pode(operacao, contexto)) throw new AcessoNegadoException();
    }
}
