package br.edu.unespar.trabalho.util;

import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;
import java.time.Duration;
import java.util.function.Supplier;
import br.edu.unespar.trabalho.dao.EquipeTecnicaDAO;
import br.edu.unespar.trabalho.service.AuthServiceImpl;

public class Sessao {
    private static EquipeTecnicaDTO usuarioLogado;
    private static long ultimaAtividade;
    private static final ThreadLocal<EquipeTecnicaDTO> sessaoDaConsulta = new ThreadLocal<>();
    private static Duration limite = Duration.ofMinutes(Math.max(1, Long.getLong("casa.sessao.minutos", 15L)));

    public static synchronized EquipeTecnicaDTO getUsuarioLogado() {
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
        EquipeTecnicaDTO atual = getUsuarioLogado();
        if (atual==null) return false;
        boolean valida = false;
        try { valida = new EquipeTecnicaDAO().sessaoValida(atual); }
        catch (RuntimeException e) { /* Falha de conexão não mantém autorização antiga. */ }
        synchronized (Sessao.class) {
            if (getUsuarioLogado()!=atual) return false;
            if (!valida) limparSessao();
            return valida;
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
        usuarioLogado = null;
    }
}
