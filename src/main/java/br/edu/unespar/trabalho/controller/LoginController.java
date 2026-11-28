package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import br.edu.unespar.trabalho.util.Sessao;
import br.edu.unespar.trabalho.util.Senhas;
import br.edu.unespar.trabalho.util.ValidacaoFormulario;
import br.edu.unespar.trabalho.util.CampoSenha;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Valida as credenciais de acesso, inicia a sessão e abre o painel principal.
 */
public class LoginController {

    @FXML private TextField txtLogin;
    @FXML private VBox areaSenha;
    @FXML private Button btnEntrar;
    private final CampoSenha campoSenha = new CampoSenha("senha", Senhas.MAXIMO_AUTENTICACAO);

    @FXML
    public void initialize() {
        ValidacaoFormulario.limitarTexto(txtLogin,100,"Login");
        campoSenha.estilizarLogin();
        areaSenha.getChildren().add(campoSenha);
    }

    @FXML
    public void fazerLogin(ActionEvent event) {
        String usuario = txtLogin.getText();
        String senha = campoSenha.getTexto();
        campoSenha.ocultar();

        if (usuario == null || usuario.trim().isEmpty() || senha.trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos Inválidos", "Por favor, preencha o login e a senha.");
            return;
        }

        try {

            EquipeTecnicaDTO usuarioLogado = Sessao.entrar(usuario, senha);

            if (usuarioLogado != null) {

                String destino = usuarioLogado.getPerfil() == br.edu.unespar.trabalho.model.NivelAcesso.EDUCADOR
                        ? "/View/FrequenciaView.fxml" : "/View/Dashboard.fxml";
                NavegacaoUtil.exigirTela(destino);
                FXMLLoader loader = new FXMLLoader(getClass().getResource(destino));
                Parent root = loader.load();

                if (loader.getController() instanceof DashboardController dashboardController)
                    dashboardController.inicializarDados(usuarioLogado.getNomeCompleto(), usuarioLogado.getCargoFuncao());
                Stage stage = (Stage) btnEntrar.getScene().getWindow();
                NavegacaoUtil.trocarRaiz(stage, root, destino.endsWith("Dashboard.fxml")
                        ? "Instituto C.A.S.A. - Painel de Controle" : "Instituto C.A.S.A. - Frequência");
                br.edu.unespar.trabalho.util.SegurancaInterface.marcarAutenticado(true);
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Acesso Negado", "Usuário ou senha incorretos.");
            }

        } catch (Exception e) {
            Sessao.limparSessao();
            mostrarAlerta(Alert.AlertType.ERROR, "Falha ao entrar", "Não foi possível iniciar o acesso. Confira a conexão, a migração de segurança e a configuração da conta.");
        } finally {
            campoSenha.limpar();
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
