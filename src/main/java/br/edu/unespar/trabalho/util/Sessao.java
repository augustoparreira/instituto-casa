package br.edu.unespar.trabalho.util;

import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;
import java.time.Duration;
import java.util.function.BooleanSupplier;

public class Sessao {
    private static EquipeTecnicaDTO usuarioLogado;
    private static long ultimaAtividade;
    private static BooleanSupplier validarConta = () -> false;
    private static Duration limite = Duration.ofMinutes(Math.max(1, Long.getLong("casa.sessao.minutos", 15L)));

    public static synchronized EquipeTecnicaDTO getUsuarioLogado() {
        if (usuarioLogado != null && System.nanoTime() - ultimaAtividade >= limite.toNanos()) limparSessao();
        return usuarioLogado;
    }

    public static synchronized void setUsuarioLogado(EquipeTecnicaDTO usuario) {
        iniciar(usuario, () -> new br.edu.unespar.trabalho.dao.EquipeTecnicaDAO().sessaoValida(usuario));
    }

    public static synchronized void iniciar(EquipeTecnicaDTO usuario, BooleanSupplier validacao) {
        if (validacao == null) throw new IllegalArgumentException("Validação da conta obrigatória.");
        usuarioLogado = usuario;
        validarConta = validacao;
        ultimaAtividade = System.nanoTime();
    }

    public static synchronized boolean validarContaAtual() {
        if (getUsuarioLogado() == null) return false;
        try { if (validarConta.getAsBoolean()) return true; }
        catch (RuntimeException e) { /* Falha de conexão não mantém autorização antiga. */ }
        limparSessao();
        return false;
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
        validarConta = () -> false;
    }
}
