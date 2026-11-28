package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.controller.RelatoriosController;
import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;
import br.edu.unespar.trabalho.model.NivelAcesso;
import br.edu.unespar.trabalho.model.Operacao;
import br.edu.unespar.trabalho.model.RelatorioAcompanhamento;
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
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/** Verifica a saída de um rascunho sem depender de um banco de dados. */
public class VerificacaoSaidaRelatorios {
    private enum Conta { ATIVA, SEM_CONEXAO, REVOGADA, DESATIVADA, PERFIL_ALTERADO }
    private enum Saida { JANELA, NAVEGACAO, LOGOUT }
    private static final String URL_TESTE = "jdbc:verificacao:fechamento";
    private static final String RASCUNHO = "Relatório fictício não salvo";
    private static Throwable falhaDialogo;
    private static Conta conta = Conta.ATIVA;
    private static Runnable aposConsulta;
    private static int verificacoes;

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
                verificarRecuperacaoAntesDaSaida();
                for (Conta estado : List.of(Conta.ATIVA, Conta.REVOGADA, Conta.DESATIVADA, Conta.PERFIL_ALTERADO)) {
                    verificarPerdaDeAcessoAntesDaSaida(estado, false);
                    verificarPerdaDeAcessoAntesDaSaida(estado, true);
                }
                verificarFalhaSemRascunho(false);
                verificarFalhaSemRascunho(true);
                verificarPreviaDuranteFalha();
                fim.complete(null);
            }
            catch (Throwable erro) { fim.completeExceptionally(erro); }
        });
        try { fim.get(60, TimeUnit.SECONDS); }
        finally { Platform.exit(); }
        System.out.println("OK: " + verificacoes + " verificações de Relatórios com JavaFX e monitor reais e JDBC simulado.");
    }

    private static void esperarMonitor(boolean expiracaoLocal) throws Exception {
        Object ciclo = new Object();
        if (expiracaoLocal) {
            var espera = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.25));
            espera.setOnFinished(e -> Platform.exitNestedEventLoop(ciclo, null));
            espera.play();
        } else {
            aposConsulta = () -> Platform.exitNestedEventLoop(ciclo, null);
            anteciparRevalidacao();
        }
        Platform.enterNestedEventLoop(ciclo);
    }

    private static boolean formularioBloqueado(Parent raiz) {
        var tela = (BorderPane) raiz;
        return tela.getCenter().isDisabled() && !tela.getCenter().isVisible()
                && tela.getLeft().isDisabled() && !tela.getLeft().isVisible();
    }

    private static void verificarRecuperacaoAntesDaSaida() throws Exception {
        conta = Conta.ATIVA;
        var sessao = iniciarSessao(Duration.ZERO);
        Stage janela = abrirTelaComMonitor();
        Parent raiz = janela.getScene().getRoot();
        var texto = (TextArea) raiz.lookup("#txtRegistroFrequencia");
        texto.setText(RASCUNHO);
        try {
            verificar(dialogosDeSaida().isEmpty() && SegurancaInterface.possuiRascunho(raiz),
                    "A primeira edição protege o rascunho sem abrir confirmação.");
            conta = Conta.SEM_CONEXAO;
            for (int tentativa = 0; tentativa < 2; tentativa++) {
                esperarMonitor(false);
                verificar(janela.getScene().getRoot() == raiz && formularioBloqueado(raiz)
                                && RASCUNHO.equals(texto.getText()) && dialogosDeSaida().isEmpty(),
                        "Falha antes do diálogo ou durante a nova tentativa mantém o texto oculto.");
                verificar(((BorderPane) raiz).getTop().lookupAll(".label").stream().anyMatch(no ->
                                no instanceof Label aviso && aviso.getText().contains("Conexão indisponível")),
                        "O bloqueio deve informar a indisponibilidade.");
                verificarOperacaoProtegidaNegada();
                verificar(!AutorizacaoService.pode(Operacao.AGENDA_CONSULTAR)
                                && !AutorizacaoService.pode(Operacao.RELATORIO_EXPORTAR),
                        "Não se pode navegar para outro módulo protegido nem exportar.");
            }
            var agenda = procurarBotao(raiz, "Agenda", false);
            agenda.fire();
            verificar(agenda.isDisabled() && janela.getScene().getRoot() == raiz && dialogosDeSaida().isEmpty(),
                    "Tentar navegar com o formulário suspenso não troca de tela nem abre confirmação.");
            responderProximoDialogo(ButtonType.NO);
            solicitarFechamento(janela);
            verificar(falhaDialogo == null && janela.isShowing() && RASCUNHO.equals(texto.getText()),
                    "Cancelar o fechamento durante a suspensão mantém o rascunho.");
            conta = Conta.ATIVA;
            esperarMonitor(false);
            verificar(Sessao.getUsuarioLogado() == sessao && !formularioBloqueado(raiz)
                            && RASCUNHO.equals(texto.getText()) && SegurancaInterface.possuiRascunho(raiz),
                    "O monitor revalida a mesma sessão e mantém a proteção do texto que continua não salvo.");
            AutorizacaoService.exigir(Operacao.RELATORIO_ALTERAR);

            conta = Conta.SEM_CONEXAO;
            esperarMonitor(false);
            responderProximoDialogo(ButtonType.YES);
            solicitarSaida(janela, Saida.LOGOUT);
            verificar(falhaDialogo == null && janela.getScene().getRoot().lookup("#btnEntrar") != null
                    && texto.getText().isEmpty(), "O logout explícito durante a suspensão pode descartar o rascunho.");
        } finally {
            encerrarSondagem(janela);
        }
    }

    private static void verificarPerdaDeAcessoAntesDaSaida(Conta estado, boolean duranteSuspensao) throws Exception {
        conta = Conta.ATIVA;
        iniciarSessao(Duration.ZERO);
        Stage janela = abrirTelaComMonitor();
        Parent raiz = janela.getScene().getRoot();
        var texto = (TextArea) raiz.lookup("#txtDescumprimento");
        texto.setText(RASCUNHO);
        try {
            if (duranteSuspensao) {
                conta = Conta.SEM_CONEXAO;
                esperarMonitor(false);
                verificar(formularioBloqueado(raiz) && RASCUNHO.equals(texto.getText()), "A suspensão deve preservar o rascunho.");
            }
            if (estado == Conta.ATIVA) Sessao.configurarInatividade(Duration.ofNanos(1));
            else conta = estado;
            esperarMonitor(estado == Conta.ATIVA);
            Parent login = janela.getScene().getRoot();
            verificar(login.lookup("#btnEntrar") != null && dialogosDeSaida().isEmpty()
                            && texto.getScene() == null && texto.getText().isEmpty(),
                    "A invalidação deve trocar a tela, limpar o texto e dispensar diálogo: " + estado);
            var aviso = (Label) login.lookup("#mensagem-login");
            verificar(aviso.getText().contains("não puderam ser recuperadas") && !aviso.getText().contains(RASCUNHO),
                    "O login informa a perda sem revelar o rascunho.");
            verificarOperacaoProtegidaNegada();
            conta = Conta.ATIVA;
            Sessao.configurarInatividade(Duration.ofMinutes(15));
            iniciarSessao(2L, Duration.ZERO);
            NavegacaoUtil.trocarRaiz(janela, carregarTelaRelatorios(), "Relatórios");
            janela.getScene().getRoot().applyCss();
            var outroTexto = (TextArea) janela.getScene().getRoot().lookup("#txtDescumprimento");
            verificar(outroTexto.getText().isEmpty(), "Outra conta não recebe conteúdo da sessão inválida.");
        } finally {
            encerrarSondagem(janela);
        }
    }

    private static void verificarFalhaSemRascunho(boolean aposSalvar) throws Exception {
        conta = Conta.ATIVA;
        iniciarSessao(Duration.ZERO);
        Stage janela = abrirTelaComMonitor();
        Parent raiz = janela.getScene().getRoot();
        try {
            if (aposSalvar) {
                prepararRascunho(raiz);
                procurarBotao(raiz, "Salvar rascunho", false).fire();
                verificar(((Label) raiz.lookup("#lblMensagem")).getText().equals("Rascunho salvo no banco."),
                        "O formulário salvou o relatório através do DAO e JDBC simulado.");
            }
            verificar(!SegurancaInterface.possuiRascunho(raiz), "A tela sem edições pendentes não registra proteção.");
            conta = Conta.SEM_CONEXAO;
            esperarMonitor(false);
            var aviso = (Label) janela.getScene().getRoot().lookup("#mensagem-login");
            verificar(aviso != null && !aviso.getText().contains("não foram salvas"),
                    "Uma falha sem rascunho encerra a sessão sem aviso de perda.");
        } finally {
            encerrarSondagem(janela);
        }
    }

    private static void verificarPreviaDuranteFalha() throws Exception {
        conta = Conta.ATIVA;
        iniciarSessao(Duration.ZERO);
        Stage janela = abrirTelaComMonitor();
        Parent raiz = janela.getScene().getRoot();
        try {
            prepararRascunho(raiz);
            Platform.runLater(() -> conta = Conta.SEM_CONEXAO);
            procurarBotao(raiz, "Visualizar", false).fire();
            verificar(formularioBloqueado(raiz)
                            && Window.getWindows().stream().filter(Window::isShowing).count() == 1,
                    "A prévia protegida deve fechar quando a sessão é suspensa.");
            verificarOperacaoProtegidaNegada();
        } finally {
            encerrarSondagem(janela);
        }
    }

    private static void prepararRascunho(Parent raiz) throws Exception {
        var rascunho = new RelatorioAcompanhamento();
        rascunho.setCpfAdolescente(1L);
        rascunho.setCompetencia(YearMonth.of(2026, 11));
        rascunho.setCampo(RelatorioAcompanhamento.Campo.NOME, "Adolescente fictício");
        rascunho.setRegistroFrequencia(RASCUNHO);
        var exibir = RelatoriosController.class.getDeclaredMethod("exibir", RelatorioAcompanhamento.class);
        exibir.setAccessible(true);
        exibir.invoke(raiz.getProperties().get("controllerTeste"), rascunho);
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
                    verificar(formularioBloqueado(raiz)
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
            verificar(janela.isShowing() && janela.getScene().getRoot() == raiz && formularioBloqueado(raiz)
                    && RASCUNHO.equals(texto.getText()), "Cancelar deve preservar o rascunho bloqueado: " + saida);
            verificarOperacaoProtegidaNegada();

            conta = Conta.ATIVA;
            Object recuperacao = new Object();
            aposConsulta = () -> Platform.exitNestedEventLoop(recuperacao, null);
            anteciparRevalidacao();
            Platform.enterNestedEventLoop(recuperacao);
            verificar(Sessao.getUsuarioLogado() == usuario && !formularioBloqueado(raiz)
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
        FXMLLoader loader = new FXMLLoader(VerificacaoSaidaRelatorios.class.getResource("/View/RelatoriosView.fxml"));
        Parent raiz = loader.load();
        raiz.getProperties().put("controllerTeste", loader.getController());
        return raiz;
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
                procurarBotao(janela.getScene().getRoot(), rotulo, saida == Saida.LOGOUT).fire();
            }
        }
    }

    private static Button procurarBotao(Parent raiz, String rotulo, boolean logout) {
        if (logout && formularioBloqueado(raiz)) raiz = (Parent) ((BorderPane) raiz).getTop();
        raiz.applyCss();
        return raiz.lookupAll(".button").stream()
                .filter(no -> no instanceof Button botao && rotulo.equals(botao.getText()))
                .map(Button.class::cast).findFirst().orElseThrow();
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
                                    String sql = (String) parametros[0];
                                    boolean consultaSessao = sql.contains("versao_sessao");
                                    if (consultaSessao) sinalizarConsulta();
                                    boolean salvamento = sql.contains("RETURNING id_relatorio");
                                    ResultSet linhas = simular(ResultSet.class,
                                            nome -> nome.equals("getInt") && salvamento ? (Object) 1
                                                    : (consultaSessao && conta == Conta.ATIVA) || salvamento);
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
        verificacoes++;
    }
}
