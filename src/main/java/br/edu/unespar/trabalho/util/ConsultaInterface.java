package br.edu.unespar.trabalho.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.concurrent.Task;
import javafx.scene.Node;

/** Executa consultas JDBC fora da thread JavaFX e entrega o resultado à tela de origem. */
public final class ConsultaInterface {
    private static final ExecutorService CONSULTAS = Executors.newFixedThreadPool(2,
            Thread.ofPlatform().daemon().name("casa-consulta-",0).factory());

    private ConsultaInterface() { }

    public static <T> Task<T> iniciar(Node origem, Supplier<T> consulta,
            Consumer<T> sucesso, Consumer<Throwable> falha) {
        var cena = origem.getScene();
        var usuario = Sessao.getUsuarioLogado();
        Task<T> tarefa = new Task<>() {
            @Override protected T call() {
                return Sessao.consultarNaSessao(usuario,consulta);
            }
        };
        tarefa.setOnSucceeded(evento -> {
            if (origem.getScene()==cena && Sessao.getUsuarioLogado()==usuario)
                sucesso.accept(tarefa.getValue());
        });
        tarefa.setOnFailed(evento -> {
            if (origem.getScene()==cena && Sessao.getUsuarioLogado()==usuario)
                falha.accept(tarefa.getException());
        });
        CONSULTAS.execute(tarefa);
        return tarefa;
    }
}
