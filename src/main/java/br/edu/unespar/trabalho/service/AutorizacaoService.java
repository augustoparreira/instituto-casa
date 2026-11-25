package br.edu.unespar.trabalho.service;

import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.util.Sessao;
import javafx.application.Platform;
import java.util.EnumSet;
import static br.edu.unespar.trabalho.model.Operacao.*;

/** Política provisória central. Permissões indefinidas permanecem negadas. */
public final class AutorizacaoService {
    private AutorizacaoService() { }

    private static final long INTERVALO_INTERFACE_NANOS = 250_000_000L;
    private static EquipeTecnicaDTO usuarioValidadoNaInterface;
    private static long instanteValidacao;

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
        if (operacao == null || usuario == null || usuario.getPerfil() == null || !sessaoValida(usuario)) return false;
        return permitido(usuario, operacao, contexto);
    }

    private static boolean permitido(EquipeTecnicaDTO usuario, Operacao operacao, ContextoAcesso contexto) {
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
        EquipeTecnicaDTO usuario = Sessao.getUsuarioLogado();
        if (operacao == null || usuario == null || usuario.getPerfil() == null
                || !Sessao.validarContaAtual() || Sessao.getUsuarioLogado()!=usuario
                || !permitido(usuario, operacao, contexto))
            throw new AcessoNegadoException();
        if (Platform.isFxApplicationThread()) {
            usuarioValidadoNaInterface = usuario;
            instanteValidacao = System.nanoTime();
        }
    }

    /** Valida a conta uma vez para uma consulta que usa várias projeções. */
    public static EnumSet<Operacao> permissoesDaConsulta(Operacao... obrigatorias) {
        EquipeTecnicaDTO usuario = Sessao.getUsuarioLogado();
        if (usuario==null || usuario.getPerfil()==null || !Sessao.validarContaAtual()
                || Sessao.getUsuarioLogado()!=usuario) throw new AcessoNegadoException();
        EnumSet<Operacao> permissoes = EnumSet.noneOf(Operacao.class);
        for (Operacao operacao : Operacao.values()) {
            if (permitido(usuario,operacao,null)) permissoes.add(operacao);
        }
        for (Operacao operacao : obrigatorias) {
            if (!permissoes.contains(operacao)) throw new AcessoNegadoException();
        }
        return permissoes;
    }

    private static boolean sessaoValida(EquipeTecnicaDTO usuario) {
        boolean interfaceJavaFx = Platform.isFxApplicationThread();
        long agora = System.nanoTime();
        if (interfaceJavaFx && usuario == usuarioValidadoNaInterface
                && agora - instanteValidacao < INTERVALO_INTERFACE_NANOS) return true;
        boolean valida = Sessao.validarContaAtual() && Sessao.getUsuarioLogado()==usuario;
        if (interfaceJavaFx && valida) {
            usuarioValidadoNaInterface = usuario;
            instanteValidacao = agora;
        }
        return valida;
    }
}
