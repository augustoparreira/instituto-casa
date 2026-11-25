package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.controller.RelatoriosController;
import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;
import br.edu.unespar.trabalho.model.NivelAcesso;
import br.edu.unespar.trabalho.model.Operacao;
import br.edu.unespar.trabalho.service.AcessoNegadoException;
import br.edu.unespar.trabalho.service.AutorizacaoService;
import br.edu.unespar.trabalho.util.Sessao;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLTransientConnectionException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/** Verifica a saída de um rascunho sem depender de um banco de dados. */
public class VerificacaoSaidaRelatorios {
    private enum Conta { ATIVA, SEM_CONEXAO, REVOGADA }
    private static final String URL_TESTE = "jdbc:verificacao:fechamento";
    private static Throwable falhaDialogo;
    private static Conta conta = Conta.ATIVA;

    public static void main(String[] args) throws Exception {
        configurarJdbcSimulado();
        Sessao.configurarInatividade(Duration.ofMinutes(15));
        CompletableFuture<Void> fim = new CompletableFuture<>();
        Platform.startup(() -> Platform.setImplicitExit(false));
        Platform.runLater(() -> {
            try {
                verificarSaidas();
                verificarFechamentoComRascunho(Conta.SEM_CONEXAO);
                verificarFechamentoComRascunho(Conta.ATIVA);
                verificarFechamentoSemConfirmacao(true, false);
                verificarFechamentoSemConfirmacao(false, true);
                verificarFechamentoSemConfirmacao(false, false);
                fim.complete(null);
            }
            catch (Throwable erro) { fim.completeExceptionally(erro); }
        });
        try { fim.get(30, TimeUnit.SECONDS); }
        finally { Platform.exit(); }
        System.out.println("OK: navegação, logout e fechamento de Relatórios, incluindo falha JDBC, expiração e revogação.");
    }

    private static void verificarSaidas() throws Exception {
        Stage janela = new Stage();
        Button origem = new Button("Sair");
        StackPane raiz = new StackPane(origem);
        janela.setScene(new Scene(raiz, 400, 300));
        janela.show();
        try {
            var usuario = iniciarSessao(Duration.ZERO);
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
                verificar(Sessao.getUsuarioLogado() == usuario, "Cancelar deve preservar a sessão.");
            }
            responderProximoDialogo(ButtonType.YES);
            controller.fazerLogout(evento);
            verificar(falhaDialogo == null, "A confirmação de logout falhou: " + falhaDialogo);
            verificar(janela.getScene().getRoot().lookup("#btnEntrar") != null,
                    "Confirmar o descarte deve concluir o logout.");
            verificar(Sessao.getUsuarioLogado() == null, "Confirmar o logout deve encerrar a sessão.");
        } finally {
            janela.close();
            Sessao.limparSessao();
        }
    }

    private static void verificarFechamentoComRascunho(Conta estado) throws Exception {
        conta = estado;
        var usuario = iniciarSessao(Duration.ZERO);
        RelatoriosController controller = new RelatoriosController();
        Stage janela = abrirRelatorios(controller, true);
        try {
            var raiz = janela.getScene().getRoot();
            responderProximoDialogo(ButtonType.NO);
            solicitarFechamento(janela);
            verificar(janela.isShowing(), "Cancelar deve manter a janela aberta: " + estado);
            verificar(falhaDialogo == null && janela.getScene().getRoot() == raiz,
                    "Cancelar deve manter o rascunho na mesma tela: " + estado);
            verificar(Sessao.getUsuarioLogado() == usuario, "O fechamento não deve invalidar a sessão.");

            responderProximoDialogo(ButtonType.YES);
            solicitarFechamento(janela);
            verificar(falhaDialogo == null && !janela.isShowing(),
                    "O fechamento com rascunho deve ocorrer após confirmação: " + estado);

            if (estado == Conta.SEM_CONEXAO) verificarOperacaoProtegidaNegada();
        } finally {
            janela.close();
            Sessao.limparSessao();
        }
    }

    private static void verificarFechamentoSemConfirmacao(boolean expirou, boolean revogada) throws Exception {
        conta = revogada ? Conta.REVOGADA : Conta.ATIVA;
        iniciarSessao(expirou ? Duration.ofDays(1) : Duration.ZERO);
        if (revogada) verificar(!Sessao.validarContaAtual(), "O JDBC deve indicar a revogação.");
        Stage janela = abrirRelatorios(new RelatoriosController(), expirou || revogada);
        try {
            solicitarFechamento(janela);
            verificar(!janela.isShowing(), "A janela deve fechar sem confirmação sem rascunho ou sessão.");
            if (expirou || revogada) verificarOperacaoProtegidaNegada();
        } finally {
            janela.close();
            Sessao.limparSessao();
        }
    }

    private static Stage abrirRelatorios(RelatoriosController controller, boolean comRascunho) throws Exception {
        Field alterado = RelatoriosController.class.getDeclaredField("alterado");
        alterado.setAccessible(true);
        alterado.setBoolean(controller, comRascunho);
        Stage janela = new Stage();
        Scene cena = new Scene(new StackPane(), 400, 300);
        janela.setScene(cena);
        var instalar = RelatoriosController.class.getDeclaredMethod("protegerFechamento", Scene.class, Window.class);
        instalar.setAccessible(true);
        instalar.invoke(controller, cena, janela);
        janela.show();
        return janela;
    }

    private static void solicitarFechamento(Stage janela) {
        janela.fireEvent(new WindowEvent(janela, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private static EquipeTecnicaDTO iniciarSessao(Duration inatividade) throws Exception {
        var contaTeste = new EquipeTecnicaDTO(1L, "teste", "Teste", "Teste", NivelAcesso.ADMINISTRADOR);
        Field usuario = Sessao.class.getDeclaredField("usuarioLogado");
        usuario.setAccessible(true);
        usuario.set(null, contaTeste);
        Field atividade = Sessao.class.getDeclaredField("ultimaAtividade");
        atividade.setAccessible(true);
        atividade.setLong(null, System.nanoTime() - inatividade.toNanos());
        return contaTeste;
    }

    private static void verificarOperacaoProtegidaNegada() {
        try {
            AutorizacaoService.exigir(Operacao.RELATORIO_ALTERAR);
            throw new AssertionError("Alteração protegida deve ser negada para a conta " + conta);
        } catch (AcessoNegadoException esperado) {
            verificar(Sessao.getUsuarioLogado() == null, "A sessão inválida deve ser encerrada.");
        }
    }

    private static void configurarJdbcSimulado() throws Exception {
        System.setProperty("casa.db.url", URL_TESTE);
        System.setProperty("casa.db.user", "teste");
        System.setProperty("casa.db.password", "");
        DriverManager.registerDriver((Driver) Proxy.newProxyInstance(
                Driver.class.getClassLoader(), new Class<?>[]{Driver.class}, (proxy, metodo, argumentos) -> {
                    if (metodo.getName().equals("toString")) return URL_TESTE;
                    if (metodo.getName().equals("connect") && URL_TESTE.equals(argumentos[0])) {
                        if (conta == Conta.SEM_CONEXAO) throw new SQLTransientConnectionException("Falha JDBC simulada");
                        ResultSet linhas = simular(ResultSet.class, nome -> conta != Conta.REVOGADA);
                        PreparedStatement consulta = simular(PreparedStatement.class, nome -> linhas);
                        return simular(Connection.class, nome -> consulta);
                    }
                    return null;
                }));
    }

    private static <T> T simular(Class<T> tipo, Function<String, Object> resultado) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo},
                (proxy, metodo, argumentos) -> metodo.getReturnType() == void.class ? null : resultado.apply(metodo.getName())));
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
