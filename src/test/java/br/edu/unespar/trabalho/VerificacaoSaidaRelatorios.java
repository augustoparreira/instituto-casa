package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.controller.RelatoriosController;
import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;
import br.edu.unespar.trabalho.model.NivelAcesso;
import br.edu.unespar.trabalho.model.Operacao;
import br.edu.unespar.trabalho.service.AcessoNegadoException;
import br.edu.unespar.trabalho.service.AutorizacaoService;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import br.edu.unespar.trabalho.util.SegurancaInterface;
import br.edu.unespar.trabalho.util.Sessao;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
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
    private enum Saida { JANELA, NAVEGACAO, LOGOUT }
    private static final String URL_TESTE = "jdbc:verificacao:fechamento";
    private static final String RASCUNHO = "Relatório fictício não salvo";
    private static Throwable falhaDialogo;
    private static Conta conta = Conta.ATIVA;
    private static Runnable aposConsulta;

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
                for (Saida saida : Saida.values()) verificarInterrupcao(saida, ButtonType.NO);
                verificarInterrupcao(Saida.JANELA, ButtonType.YES);
                verificarInterrupcao(Saida.LOGOUT, ButtonType.YES);
                verificarEncerramentoDuranteDialogo(Conta.ATIVA);
                verificarEncerramentoDuranteDialogo(Conta.REVOGADA);
                verificarRespostaAntigaDepoisDaTrocaDeSessao();
                fim.complete(null);
            }
            catch (Throwable erro) { fim.completeExceptionally(erro); }
        });
        try { fim.get(30, TimeUnit.SECONDS); }
        finally { Platform.exit(); }
        System.out.println("OK: fechamento, navegação e logout de Relatórios com o monitor real, recuperação, expiração e revogação.");
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
        return iniciarSessao(1L, inatividade);
    }

    private static EquipeTecnicaDTO iniciarSessao(long cpf, Duration inatividade) throws Exception {
        var contaTeste = new EquipeTecnicaDTO(cpf, "teste", "Teste", "Teste", NivelAcesso.ADMINISTRADOR);
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
            verificar(Sessao.getUsuarioLogado() == null, "A identidade indisponível não pode autorizar operações.");
        }
    }

    private static void verificarInterrupcao(Saida saida, ButtonType resposta) throws Exception {
        conta = Conta.ATIVA;
        var usuario = iniciarSessao(Duration.ZERO);
        Stage janela = abrirTelaComMonitor();
        Parent raiz = janela.getScene().getRoot();
        TextArea texto = (TextArea) raiz.lookup("#txtRegistroFrequencia");
        texto.setText(RASCUNHO);
        try {
            falhaDialogo = null;
            Platform.runLater(() -> agirNoDialogo(painel -> {
                conta = Conta.SEM_CONEXAO;
                aposConsulta = () -> agirNoDialogo(aberto -> {
                    var confirmar = (Button) aberto.lookupButton(ButtonType.YES);
                    if (saida == Saida.JANELA) solicitarFechamento(janela);
                    verificar(dialogosDeSaida().equals(List.of(aberto)), "Deve existir apenas uma confirmação de saída.");
                    verificar(confirmar.isDisabled() == (saida == Saida.NAVEGACAO), "Somente o descarte local pode prosseguir sem conexão.");
                    verificar(aberto.getContentText().contains("Conexão indisponível"), "A confirmação deve explicar a falha.");
                    verificar(raiz.isDisabled() && !((BorderPane) raiz).getCenter().isVisible()
                            && RASCUNHO.equals(texto.getText()), "A indisponibilidade deve ocultar o formulário, bloquear a edição e preservar o texto.");
                    verificar(!AutorizacaoService.pode(Operacao.RELATORIO_CONSULTAR), "Uma autorização anterior não deve valer sem conexão.");
                    verificarOperacaoProtegidaNegada();
                    ((Button) aberto.lookupButton(resposta)).fire();
                });
            }));
            solicitarSaida(janela, saida);
            verificar(falhaDialogo == null, "Falha no diálogo com monitor: " + falhaDialogo);
            if (resposta == ButtonType.YES) {
                verificar(saida == Saida.JANELA ? !janela.isShowing()
                                : janela.getScene().getRoot().lookup("#btnEntrar") != null,
                        "Confirmar o descarte deve concluir o fechamento ou o logout durante a falha.");
                verificarOperacaoProtegidaNegada();
                return;
            }
            verificar(janela.isShowing() && janela.getScene().getRoot() == raiz && raiz.isDisabled()
                    && RASCUNHO.equals(texto.getText()), "Cancelar deve preservar o rascunho bloqueado: " + saida);
            verificarOperacaoProtegidaNegada();

            conta = Conta.ATIVA;
            Object recuperacao = new Object();
            aposConsulta = () -> Platform.exitNestedEventLoop(recuperacao, null);
            anteciparRevalidacao();
            Platform.enterNestedEventLoop(recuperacao);
            verificar(Sessao.getUsuarioLogado() == usuario && !raiz.isDisabled()
                            && ((BorderPane) raiz).getCenter().isVisible() && RASCUNHO.equals(texto.getText()),
                    "A revalidação deve liberar a edição para a mesma sessão: " + saida);
            AutorizacaoService.exigir(Operacao.RELATORIO_ALTERAR);
        } finally {
            encerrarSondagem(janela);
        }
    }

    private static void verificarEncerramentoDuranteDialogo(Conta proximoEstado) throws Exception {
        conta = Conta.ATIVA;
        iniciarSessao(Duration.ZERO);
        Stage janela = abrirTelaComMonitor();
        TextArea texto = (TextArea) janela.getScene().getRoot().lookup("#txtRegistroFrequencia");
        texto.setText(RASCUNHO);
        try {
            falhaDialogo = null;
            Platform.runLater(() -> agirNoDialogo(painel -> {
                conta = proximoEstado;
                if (proximoEstado == Conta.ATIVA) Sessao.configurarInatividade(Duration.ofNanos(1));
            }));
            solicitarFechamento(janela);
            verificar(falhaDialogo == null && janela.isShowing() && dialogosDeSaida().isEmpty(),
                    "A sessão inválida deve dispensar a confirmação e manter o login aberto.");
            Parent login = janela.getScene().getRoot();
            verificar(login.lookup("#btnEntrar") != null && texto.getScene() == null, "O relatório antigo não pode permanecer acessível.");
            var aviso = (Label) login.lookup("#mensagem-login");
            verificar(aviso.getText().contains("A sessão terminou") && aviso.getText().contains("não foram salvas"),
                    "O login precisa informar o encerramento e as alterações não salvas.");
            verificarOperacaoProtegidaNegada();

            conta = Conta.ATIVA;
            Sessao.configurarInatividade(Duration.ofMinutes(15));
            iniciarSessao(2L, Duration.ZERO);
            NavegacaoUtil.trocarRaiz(janela, carregarTelaRelatorios(), "Relatórios");
            Parent relatorios = janela.getScene().getRoot();
            relatorios.applyCss();
            relatorios.layout();
            var novoTexto = (TextArea) relatorios.lookup("#txtRegistroFrequencia");
            verificar(novoTexto.getText().isEmpty() && texto.getScene() == null, "Outra conta não pode receber o rascunho anterior.");
        } finally {
            encerrarSondagem(janela);
        }
    }

    private static void verificarRespostaAntigaDepoisDaTrocaDeSessao() throws Exception {
        conta = Conta.ATIVA;
        iniciarSessao(Duration.ZERO);
        Stage janela = abrirTelaComMonitor();
        ((TextArea) janela.getScene().getRoot().lookup("#txtRegistroFrequencia")).setText(RASCUNHO);
        Parent novaTela = new StackPane();
        try {
            falhaDialogo = null;
            Platform.runLater(() -> agirNoDialogo(painel -> {
                try {
                    Sessao.limparSessao();
                    iniciarSessao(2L, Duration.ZERO);
                    NavegacaoUtil.trocarRaiz(janela, novaTela, "Nova sessão");
                    ((Button) painel.lookupButton(ButtonType.YES)).fire();
                } catch (Exception erro) { throw new AssertionError(erro); }
            }));
            solicitarFechamento(janela);
            verificar(falhaDialogo == null && janela.isShowing() && janela.getScene().getRoot() == novaTela,
                    "Aceitar uma confirmação antiga não pode fechar a nova tela.");
        } finally {
            encerrarSondagem(janela);
        }
    }

    private static Parent carregarTelaRelatorios() throws Exception {
        return FXMLLoader.load(VerificacaoSaidaRelatorios.class.getResource("/View/RelatoriosView.fxml"));
    }

    private static Stage abrirTelaComMonitor() throws Exception {
        Stage janela = new Stage();
        NavegacaoUtil.trocarRaiz(janela, carregarTelaRelatorios(), "Relatórios");
        janela.show();
        anteciparRevalidacao();
        SegurancaInterface.marcarAutenticado(true);
        SegurancaInterface.iniciarMonitor(janela);
        return janela;
    }

    private static void anteciparRevalidacao() throws Exception {
        Field segundos = SegurancaInterface.class.getDeclaredField("segundos");
        segundos.setAccessible(true);
        segundos.setInt(null, 4);
    }

    private static void solicitarSaida(Stage janela, Saida saida) {
        switch (saida) {
            case JANELA -> solicitarFechamento(janela);
            case NAVEGACAO, LOGOUT -> {
                String rotulo = saida == Saida.NAVEGACAO ? "Agenda" : "Sair do sistema";
                janela.getScene().getRoot().lookupAll(".button").stream()
                        .filter(no -> no instanceof Button botao && rotulo.equals(botao.getText()))
                        .map(Button.class::cast).findFirst().orElseThrow().fire();
            }
        }
    }

    private static List<DialogPane> dialogosDeSaida() {
        return Window.getWindows().stream().filter(Window::isShowing).map(Window::getScene)
                .map(Scene::getRoot).filter(DialogPane.class::isInstance).map(DialogPane.class::cast)
                .filter(painel -> painel.getContentText().contains("alterações não salvas")).toList();
    }

    private static void agirNoDialogo(Consumer<DialogPane> acao) {
        try {
            acao.accept(dialogosDeSaida().getFirst());
        } catch (Throwable erro) {
            falhaDialogo = erro;
            for (DialogPane painel : dialogosDeSaida()) painel.getScene().getWindow().hide();
        }
    }

    private static void encerrarSondagem(Stage janela) {
        aposConsulta = null;
        SegurancaInterface.marcarAutenticado(false);
        Sessao.limparSessao();
        Sessao.configurarInatividade(Duration.ofMinutes(15));
        janela.close();
    }

    private static void sinalizarConsulta() {
        if (aposConsulta == null) return;
        Platform.runLater(aposConsulta);
        aposConsulta = null;
    }

    private static void configurarJdbcSimulado() throws Exception {
        System.setProperty("casa.db.url", URL_TESTE);
        System.setProperty("casa.db.user", "teste");
        System.setProperty("casa.db.password", "");
        DriverManager.registerDriver((Driver) Proxy.newProxyInstance(
                Driver.class.getClassLoader(), new Class<?>[]{Driver.class}, (proxy, metodo, argumentos) -> {
                    if (metodo.getName().equals("toString")) return URL_TESTE;
                    if (metodo.getName().equals("connect") && URL_TESTE.equals(argumentos[0])) {
                        if (conta == Conta.SEM_CONEXAO) {
                            sinalizarConsulta();
                            throw new SQLTransientConnectionException("Falha JDBC simulada");
                        }
                        return Connection.class.cast(Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                new Class<?>[]{Connection.class}, (c, operacao, parametros) -> {
                                    if (!operacao.getName().equals("prepareStatement")) return null;
                                    boolean consultaSessao = ((String) parametros[0]).contains("versao_sessao");
                                    if (consultaSessao) sinalizarConsulta();
                                    ResultSet linhas = simular(ResultSet.class, nome -> consultaSessao && conta != Conta.REVOGADA);
                                    return simular(PreparedStatement.class, nome -> linhas);
                                }));
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
