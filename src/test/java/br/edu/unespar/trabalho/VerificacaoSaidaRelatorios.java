package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.controller.RelatoriosController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Verifica a saída de um rascunho sem depender de um banco de dados. */
public class VerificacaoSaidaRelatorios {
    private static Throwable falhaDialogo;
    public static void main(String[] args) throws Exception {
        CompletableFuture<Void> fim = new CompletableFuture<>();
        Platform.startup(() -> Platform.setImplicitExit(false));
        Platform.runLater(() -> {
            try { verificarSaidas(); fim.complete(null); }
            catch (Throwable erro) { fim.completeExceptionally(erro); }
        });
        try { fim.get(30, TimeUnit.SECONDS); }
        finally { Platform.exit(); }
        System.out.println("OK: saída de Relatórios preserva rascunho quando o descarte é recusado.");
    }

    private static void verificarSaidas() throws Exception {
        Stage janela = new Stage();
        Button origem = new Button("Sair");
        StackPane raiz = new StackPane(origem);
        janela.setScene(new Scene(raiz, 400, 300));
        janela.show();
        try {
            RelatoriosController controller = new RelatoriosController();
            Field alterado = RelatoriosController.class.getDeclaredField("alterado");
            alterado.setAccessible(true);
            alterado.setBoolean(controller, true);
            ActionEvent evento = new ActionEvent(origem, origem);
            for (Consumer<ActionEvent> saida : List.<Consumer<ActionEvent>>of(
                    controller::irParaPainel, controller::irParaAdolescentes,
                    controller::irParaAgenda, controller::irParaEquipeTecnica,
                    controller::fazerLogout)) {
                responderProximoDialogo(ButtonType.NO);
                saida.accept(evento);
                verificar(falhaDialogo == null, "A confirmação de saída falhou: " + falhaDialogo);
                verificar(janela.getScene().getRoot() == raiz, "Cancelar deve manter a tela de Relatórios.");
            }
            responderProximoDialogo(ButtonType.YES);
            controller.fazerLogout(evento);
            verificar(falhaDialogo == null, "A confirmação de logout falhou: " + falhaDialogo);
            verificar(janela.getScene().getRoot().lookup("#btnEntrar") != null,
                    "Confirmar o descarte deve concluir o logout.");
        } finally { janela.close(); }
    }

    private static void responderProximoDialogo(ButtonType resposta) {
        falhaDialogo = null;
        Platform.runLater(() -> {
            for (Window janela : List.copyOf(Window.getWindows())) {
                if (janela.getScene() == null) continue;
                if (janela.getScene().getRoot().lookup(".dialog-pane") instanceof DialogPane painel) {
                    try {
                        verificar(painel.getContentText().contains("alterações não salvas"),
                                "O aviso deve explicar o descarte do rascunho.");
                        verificar(painel.lookupButton(resposta) instanceof Button,
                                "A confirmação deve oferecer a resposta esperada.");
                    } catch (Throwable erro) { falhaDialogo = erro; }
                    Button botao = (Button) painel.lookupButton(resposta);
                    if (botao == null) botao = (Button) painel.lookupButton(ButtonType.OK);
                    if (botao != null) botao.fire();
                    return;
                }
            }
            falhaDialogo = new AssertionError("A confirmação de saída não foi apresentada.");
        });
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
