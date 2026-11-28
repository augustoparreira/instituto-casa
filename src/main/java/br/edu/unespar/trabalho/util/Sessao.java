package br.edu.unespar.trabalho.util;

import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;
import java.time.Duration;
import java.util.function.Supplier;
import br.edu.unespar.trabalho.dao.EquipeTecnicaDAO;
import br.edu.unespar.trabalho.service.AuthServiceImpl;

public class Sessao {
    enum Verificacao { VALIDA, INVALIDA, INDISPONIVEL }

    private static EquipeTecnicaDTO usuarioLogado;
    private static EquipeTecnicaDTO sessaoComRascunho;
    private static boolean revalidacaoPendente;
    private static long revisaoDaFalha;
    private static long ultimaAtividade;
    private static final ThreadLocal<EquipeTecnicaDTO> sessaoDaConsulta = new ThreadLocal<>();
    private static Duration limite = Duration.ofMinutes(Math.max(1, Long.getLong("casa.sessao.minutos", 15L)));

    public static synchronized EquipeTecnicaDTO getUsuarioLogado() {
        EquipeTecnicaDTO atual = identidadeAtual();
        return revalidacaoPendente ? null : atual;
    }

    // O monitor ainda precisa reconhecer a sessão suspensa e verificar sua expiração.
    private static EquipeTecnicaDTO identidadeAtual() {
        if (usuarioLogado != null && System.nanoTime() - ultimaAtividade >= limite.toNanos()) limparSessao();
        EquipeTecnicaDTO esperada = sessaoDaConsulta.get();
        return esperada==null || esperada==usuarioLogado ? usuarioLogado : null;
    }

    static <T> T consultarNaSessao(EquipeTecnicaDTO esperada, Supplier<T> consulta) {
        if (esperada==null) throw new br.edu.unespar.trabalho.service.AcessoNegadoException();
        sessaoDaConsulta.set(esperada);
        try {
            if (getUsuarioLogado()==null) throw new br.edu.unespar.trabalho.service.AcessoNegadoException();
            return consulta.get();
        } finally {
            sessaoDaConsulta.remove();
        }
    }

    public static synchronized EquipeTecnicaDTO entrar(String login, String senha) throws Exception {
        limparSessao();
        usuarioLogado = new AuthServiceImpl().autenticar(login, senha);
        ultimaAtividade = System.nanoTime();
        return usuarioLogado;
    }

    public static boolean validarContaAtual() {
        return verificarConta(false) == Verificacao.VALIDA;
    }

    static synchronized boolean existeSessao() {
        return identidadeAtual() != null;
    }

    static synchronized boolean protegerRascunho() {
        var atual = getUsuarioLogado();
        if (atual == null) return false;
        sessaoComRascunho = atual;
        return true;
    }

    static synchronized boolean rascunhoDaSessaoAtual() {
        return sessaoComRascunho != null && sessaoComRascunho == identidadeAtual();
    }

    static synchronized boolean rascunhoBloqueado() {
        return rascunhoDaSessaoAtual() && revalidacaoPendente;
    }

    static synchronized boolean liberarRascunho() {
        if (revalidacaoPendente) return false;
        sessaoComRascunho = null;
        return true;
    }

    static synchronized void desvincularRascunho() {
        if (!liberarRascunho()) limparSessao();
    }

    static Verificacao revalidarPeloMonitor() {
        return verificarConta(true);
    }

    private static Verificacao verificarConta(boolean monitor) {
        EquipeTecnicaDTO atual;
        long revisaoInicial;
        synchronized (Sessao.class) {
            atual = identidadeAtual();
            if (atual == null) return Verificacao.INVALIDA;
            if (revalidacaoPendente && !monitor) return Verificacao.INDISPONIVEL;
            revisaoInicial = revisaoDaFalha;
        }

        Verificacao resultado;
        try {
            resultado = new EquipeTecnicaDAO().sessaoValida(atual) ? Verificacao.VALIDA : Verificacao.INVALIDA;
        } catch (RuntimeException erro) {
            resultado = Verificacao.INDISPONIVEL;
        }

        synchronized (Sessao.class) {
            if (identidadeAtual() != atual) return Verificacao.INVALIDA;
            if (resultado == Verificacao.INDISPONIVEL && sessaoComRascunho == atual) {
                revisaoDaFalha++;
                if (!revalidacaoPendente) SegurancaInterface.atualizarRascunhoNaInterface();
                revalidacaoPendente = true;
                return Verificacao.INDISPONIVEL;
            }
            if (resultado != Verificacao.VALIDA) {
                limparSessao();
                return Verificacao.INVALIDA;
            }
            // Uma verificação iniciada antes de outra falha não desfaz a suspensão.
            if (monitor && revisaoInicial == revisaoDaFalha) revalidacaoPendente = false;
            return revalidacaoPendente ? Verificacao.INDISPONIVEL : Verificacao.VALIDA;
        }
    }

    public static synchronized void registrarAtividade() {
        if (getUsuarioLogado() != null) ultimaAtividade = System.nanoTime();
    }

    public static synchronized void configurarInatividade(Duration duracao) {
        if (duracao == null || duracao.isZero() || duracao.isNegative())
            throw new IllegalArgumentException("Prazo de sessão deve ser positivo.");
        limite = duracao;
    }

    public static synchronized void limparSessao() {
        if (sessaoComRascunho != null) SegurancaInterface.atualizarRascunhoNaInterface();
        usuarioLogado = null;
        sessaoComRascunho = null;
        revalidacaoPendente = false;
    }
}
