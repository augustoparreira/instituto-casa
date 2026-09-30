package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;
import br.edu.unespar.trabalho.service.AuthService;
import br.edu.unespar.trabalho.service.AuthServiceImpl;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import br.edu.unespar.trabalho.util.NavegacaoUtil;

public class LoginController {

    @FXML
    private TextField txtLogin;

    @FXML
    private PasswordField txtSenha;

    @FXML
    private Button btnEntrar;

    private AuthService authService = new AuthServiceImpl();

    @FXML
    public void fazerLogin(ActionEvent event) {
        String usuario = txtLogin.getText();
        String senha = txtSenha.getText();

        if (usuario == null || usuario.trim().isEmpty() || senha == null || senha.trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos Inválidos", "Por favor, preencha o login e a senha.");
            return;
        }

        try {
            EquipeTecnicaDTO usuarioLogado = authService.autenticar(usuario, senha);

            if (usuarioLogado != null) {
                // Carrega o Dashboard após a autenticação bem-sucedida
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Dashboard.fxml"));
                Parent root = loader.load();

                // Passa os dados do usuário para o controller do Dashboard
                DashboardController dashboardController = loader.getController();
                dashboardController.inicializarDados(usuarioLogado.getLogin(), usuarioLogado.getCargoFuncao());

                // Altera a cena na janela atual
                Stage stage = (Stage) btnEntrar.getScene().getWindow();
                NavegacaoUtil.trocarRaiz(stage, root, "Instituto C.A.S.A. - Painel de Controle");

            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Acesso Negado", "Usuário ou senha incorretos.");
            }

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Ocorreu um erro ao carregar o sistema.");
            e.printStackTrace();
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