package br.edu.unespar.trabalho.util;

import br.edu.unespar.trabalho.model.Operacao;
import br.edu.unespar.trabalho.service.AutorizacaoService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/** Apresentação de permissões e monitoramento da inatividade em janelas JavaFX. */
public final class SegurancaInterface {
    private static final Map<Scene,Boolean> OBSERVADAS = new WeakHashMap<>();
    private static boolean autenticado;
    private static Timeline monitor;
    private static int segundos;
    private static ProtecaoRascunho protecaoRascunho;

    private record ProtecaoRascunho(Parent raiz, Consumer<Boolean> atualizarBloqueio) { }

    private SegurancaInterface() { }

    public static void iniciarMonitor(Stage principal) {
        if (monitor != null) monitor.stop();
        monitor = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            for (Window janela : List.copyOf(Window.getWindows())) observar(janela.getScene());
            if (!autenticado) return;
            boolean haviaRascunho = protecaoRascunho != null && principal.getScene().getRoot() == protecaoRascunho.raiz();
            if (!Sessao.existeSessao() || (haviaRascunho && !Sessao.rascunhoDaSessaoAtual())) {
                NavegacaoUtil.encerrarSessao(principal, haviaRascunho);
            } else if (++segundos % 5 == 0) {
                switch (Sessao.revalidarPeloMonitor()) {
                    case INVALIDA -> NavegacaoUtil.encerrarSessao(principal, haviaRascunho);
                    case INDISPONIVEL -> { if (haviaRascunho) protecaoRascunho.atualizarBloqueio().accept(true); }
                    case VALIDA -> { if (haviaRascunho) protecaoRascunho.atualizarBloqueio().accept(false); }
                }
            }
        }));
        monitor.setCycleCount(Timeline.INDEFINITE); monitor.play();
    }

    public static void marcarAutenticado(boolean valor) {
        autenticado = valor;
        if (!valor) protecaoRascunho = null;
    }

    public static boolean protegerRascunho(Parent raiz, Consumer<Boolean> atualizarBloqueio) {
        if (protecaoRascunho != null) return possuiRascunho(raiz);
        if (!Sessao.protegerRascunho()) return false;
        protecaoRascunho = new ProtecaoRascunho(raiz, atualizarBloqueio);
        return true;
    }

    public static boolean possuiRascunho(Parent raiz) {
        return protecaoRascunho != null && protecaoRascunho.raiz() == raiz && Sessao.rascunhoDaSessaoAtual();
    }

    public static void liberarRascunho(Parent raiz) {
        if (possuiRascunho(raiz) && Sessao.liberarRascunho()) {
            protecaoRascunho = null;
        }
    }

    public static void desvincularRascunho(Parent raiz) {
        if (protecaoRascunho != null && protecaoRascunho.raiz() == raiz) {
            Sessao.desvincularRascunho();
            protecaoRascunho = null;
        }
    }

    public static void observar(Scene scene) {
        if (scene == null || OBSERVADAS.putIfAbsent(scene,true) != null) return;
        scene.addEventFilter(KeyEvent.KEY_PRESSED,e -> Sessao.registrarAtividade());
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED,e -> Sessao.registrarAtividade());
        scene.addEventFilter(ScrollEvent.SCROLL,e -> Sessao.registrarAtividade());
    }

    public static void aplicar(Parent raiz) {
        var usuario = Sessao.getUsuarioLogado();
        aplicarNo(raiz,usuario);
    }

    private static void aplicarNo(Node no, br.edu.unespar.trabalho.model.EquipeTecnicaDTO usuario) {
        if(no instanceof Label label && usuario!=null) {
            if(label.getStyleClass().contains("user-name")) label.setText(usuario.getNomeCompleto());
            if(label.getStyleClass().contains("user-role")) label.setText(usuario.getCargoFuncao());
        }
        if(no instanceof Button botao) {
            if (usuario != null && botao.getStyleClass().contains("logout-button")
                    && botao.getParent() instanceof VBox menu
                    && menu.getChildren().stream().noneMatch(filho -> "trocarMinhaSenha".equals(filho.getId()))) {
                Button senha = new Button("Alterar minha senha");
                senha.setId("trocarMinhaSenha");
                senha.getStyleClass().add("logout-button");
                senha.setMaxWidth(Double.MAX_VALUE);
                senha.setOnAction(evento -> trocarMinhaSenha((Stage) botao.getScene().getWindow()));
                menu.getChildren().add(menu.getChildren().indexOf(botao), senha);
            }
            Operacao operacao=switch(botao.getText()) {
                case "Equipe Técnica" -> Operacao.USUARIO_GERENCIAR;
                case "Painel" -> Operacao.PAINEL_CONSULTAR;
                case "Adolescentes" -> Operacao.ADOLESCENTE_CONSULTAR;
                case "Agenda" -> Operacao.AGENDA_CONSULTAR;
                case "Relatórios" -> Operacao.RELATORIO_CONSULTAR;
                default -> null;
            };
            if(operacao!=null) {
                boolean permitido=AutorizacaoService.pode(operacao);
                botao.setVisible(permitido); botao.setManaged(permitido);
            }
        }
        if(no instanceof Parent pai) for(Node filho:List.copyOf(pai.getChildrenUnmodifiable())) aplicarNo(filho,usuario);
    }

    private static void trocarMinhaSenha(Stage principal) {
        if (!AutorizacaoService.pode(Operacao.SENHA_PROPRIA_ALTERAR)) return;
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.initOwner(principal);
        dialogo.setTitle("Alterar minha senha");
        PasswordField atual = new PasswordField(), nova = new PasswordField(), confirmar = new PasswordField();
        ValidacaoFormulario.limitarSenha(atual,Senhas.MAXIMO_AUTENTICACAO);
        ValidacaoFormulario.limitarSenha(nova,Senhas.MAXIMO_NOVA);
        ValidacaoFormulario.limitarSenha(confirmar,Senhas.MAXIMO_NOVA);
        Label mensagem = new Label();
        dialogo.getDialogPane().setContent(new VBox(8, new Label("Senha atual"), atual,
                new Label("Nova senha (12 a 128 caracteres)"), nova,
                new Label("Confirme a nova senha"), confirmar, mensagem));
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, evento -> {
            try {
                if (!nova.getText().equals(confirmar.getText()))
                    throw new IllegalArgumentException("As senhas não coincidem.");
                new br.edu.unespar.trabalho.dao.EquipeTecnicaDAO().trocarMinhaSenha(atual.getText(), nova.getText());
            } catch (RuntimeException erro) {
                mensagem.setText(erro.getMessage());
                evento.consume();
            }
        });
        try {
            if (dialogo.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                NavegacaoUtil.encerrarSessao(principal);
                new Alert(Alert.AlertType.INFORMATION, "Senha alterada. Entre novamente.").showAndWait();
            }
        } finally {
            atual.clear();
            nova.clear();
            confirmar.clear();
        }
    }
}
