package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.model.AdolescenteDTO;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DashboardController {

    @FXML private Label lblNomeUsuario;
    @FXML private Label lblCargoUsuario;

    @FXML private HBox cardLucas;
    @FXML private HBox cardMariana;
    @FXML private VBox cardPrazoGabriel;

    @FXML
    public void initialize() {
        if (cardLucas != null) {
            cardLucas.setUserData(new AdolescenteDTO(
                    "Lucas Henrique Oliveira", "123.456.789-00", "PSC", 0.40, "48/120h",
                    "Dra. Ana Paula Costa", "Ativo", "Balneário Praia Grande", "14/03/2007", "Masculino"
            ));
        }
        if (cardMariana != null) {
            cardMariana.setUserData(new AdolescenteDTO(
                    "Mariana dos Santos Silva", "234.567.890-11", "LA", 0.25, "3/12h",
                    "Psic. Bruno Ferreira", "Ativo", "Centro", "22/09/2008", "Feminino"
            ));
        }
        if (cardPrazoGabriel != null) {
            cardPrazoGabriel.setUserData(new AdolescenteDTO(
                    "Gabriel Alves Pereira", "345.678.901-22", "PSC", 1.00, "90/90h",
                    "Ass. Soc. Carla Mendes", "Ativo", "Jardim Progresso", "05/11/2006", "Masculino"
            ));
        }
    }

    public void inicializarDados(String nome, String cargo) {
        if (lblNomeUsuario != null) lblNomeUsuario.setText(nome);
        if (lblCargoUsuario != null) lblCargoUsuario.setText(cargo);
    }

    @FXML
    public void irParaAdolescentes(ActionEvent event) {
        NavegacaoUtil.mudarTela(event, "/View/AdolescentesView.fxml", "Adolescentes");
    }

    @FXML
    public void irParaListaAdolescentes(MouseEvent event) {
        NavegacaoUtil.mudarTela(event, "/View/AdolescentesView.fxml", "Adolescentes");
    }

    @FXML
    public void irParaAgenda(ActionEvent event) {
        NavegacaoUtil.mudarTela(event, "/View/AgendaView.fxml", "Agenda institucional");
    }

    @FXML
    public void irParaAgendaPeloLink(MouseEvent event) {
        NavegacaoUtil.mudarTela(event, "/View/AgendaView.fxml", "Agenda institucional");
    }

    @FXML
    public void clicarAdolescentePainel(MouseEvent event) {
        try {
            javafx.scene.Node node = (javafx.scene.Node) event.getSource();
            AdolescenteDTO jovemSelecionado = (AdolescenteDTO) node.getUserData();

            if (jovemSelecionado == null) {
                jovemSelecionado = new AdolescenteDTO(
                        "Lucas Henrique Oliveira", "123.456.789-00", "PSC", 0.40, "48/120h",
                        "Dra. Ana Paula Costa", "Ativo", "Balneário Praia Grande", "14/03/2007", "Masculino"
                );
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/DetalhesAdolescenteView.fxml"));
            Parent root = loader.load();

            DetalhesAdolescenteController detalheController = loader.getController();
            detalheController.carregarDados(jovemSelecionado);

            Stage stage = (Stage) lblNomeUsuario.getScene().getWindow();
            NavegacaoUtil.trocarRaiz(stage, root, "Instituto C.A.S.A. - Perfil de " + jovemSelecionado.getNome());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void fazerLogout(ActionEvent event) {
        NavegacaoUtil.mudarTela(event, "/View/Login.fxml", "Login");
    }
}