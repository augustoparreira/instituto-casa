package br.edu.unespar.trabalho.util;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import br.edu.unespar.trabalho.model.Operacao;
import br.edu.unespar.trabalho.service.AutorizacaoService;
import java.util.List;

public class NavegacaoUtil {

    public static void mudarTela(Event event, String fxmlPath, String titulo) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            if ("/View/Login.fxml".equals(fxmlPath)) { encerrarSessao(stage); return; }
            exigirTela(fxmlPath);
            Parent root = FXMLLoader.load(NavegacaoUtil.class.getResource(fxmlPath));
            trocarRaiz(stage, root, "Instituto C.A.S.A. - " + titulo);
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING, "Não foi possível abrir a tela. Confira sua sessão e permissão.").showAndWait();
        }
    }

    public static void exigirTela(String caminho) {
        Operacao op = switch(caminho) {
            case "/View/Dashboard.fxml" -> Operacao.PAINEL_CONSULTAR;
            case "/View/AdolescentesView.fxml" -> Operacao.ADOLESCENTE_CONSULTAR;
            case "/View/CadastroAdolescenteView.fxml", "/View/DetalhesAdolescenteView.fxml" -> Operacao.ADOLESCENTE_CONSULTAR;
            case "/View/AgendaView.fxml" -> Operacao.AGENDA_CONSULTAR;
            case "/View/FrequenciaView.fxml", "/View/RegistroFrequenciaView.fxml" -> Operacao.FREQUENCIA_CONSULTAR;
            case "/View/RelatoriosView.fxml" -> Operacao.RELATORIO_CONSULTAR;
            case "/View/EquipeTecnicaView.fxml" -> Operacao.USUARIO_GERENCIAR;
            default -> throw new br.edu.unespar.trabalho.service.AcessoNegadoException();
        };
        AutorizacaoService.exigir(op);
    }

    public static void encerrarSessao(Stage principal) {
        encerrarSessao(principal, false);
    }

    static void encerrarSessao(Stage principal, boolean haviaRascunho) {
        Sessao.limparSessao(); SegurancaInterface.marcarAutenticado(false);
        for(Window janela:List.copyOf(Window.getWindows())) if(janela!=principal) janela.hide();
        try {
            Parent raiz=FXMLLoader.load(NavegacaoUtil.class.getResource("/View/Login.fxml"));
            if (haviaRascunho && raiz.lookup("#mensagem-login") instanceof Label mensagem) {
                mensagem.setText("A sessão terminou. Havia alterações de Relatórios que não foram salvas.");
            }
            trocarRaiz(principal,raiz,"Instituto C.A.S.A. - Login");
        } catch(java.io.IOException e) { principal.close(); }
    }

    // Troca o conteúdo da janela SEM criar uma nova Scene (isso evita o encolhimento)
    public static void trocarRaiz(Stage stage, Parent root, String tituloCompleto) {
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(new Scene(root));
        } else {
            scene.setRoot(root);
        }
        stage.setTitle(tituloCompleto);
        SegurancaInterface.observar(stage.getScene());
        SegurancaInterface.aplicar(root);
    }
}
