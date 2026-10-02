package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.model.AdolescenteDTO;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class DashboardController {

    @FXML private Label lblNomeUsuario;
    @FXML private Label lblCargoUsuario;

    @FXML private Label lblTotalCadastrados;
    @FXML private Label lblTotalPsc;
    @FXML private Label lblTotalLa;

    @FXML private HBox cardLucas;
    @FXML private HBox cardMariana;
    @FXML private VBox cardPrazoGabriel;

    private AdolescenteDAO adolescenteDAO = new AdolescenteDAO();

    @FXML
    public void initialize() {
        carregarEstatisticasReais();
    }

    private void carregarEstatisticasReais() {
        List<AdolescenteDTO> jovensCadastrados = adolescenteDAO.listarResumoDTO();

        long totalPsc = jovensCadastrados.stream().filter(j -> "PSC".equalsIgnoreCase(j.getMedida())).count();
        long totalLa = jovensCadastrados.stream().filter(j -> "LA".equalsIgnoreCase(j.getMedida())).count();

        // Atribui os valores ao ecrã (se tiver incluído os FX:IDs nos labels)
        if (lblTotalCadastrados != null) lblTotalCadastrados.setText(String.valueOf(jovensCadastrados.size()));
        if (lblTotalPsc != null) lblTotalPsc.setText(String.valueOf(totalPsc));
        if (lblTotalLa != null) lblTotalLa.setText(String.valueOf(totalLa));

        // Substitui os cartões estáticos pelos dois primeiros adolescentes da base de dados, se existirem
        if (!jovensCadastrados.isEmpty() && cardLucas != null) {
            cardLucas.setUserData(jovensCadastrados.get(0));
            // Opcional: Procurar e atualizar os Labels internos do HBox (Nome, Técnico, etc.)
        }

        if (jovensCadastrados.size() > 1 && cardMariana != null) {
            cardMariana.setUserData(jovensCadastrados.get(1));
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
    public void clicarAdolescentePainel(MouseEvent event) {
        try {
            javafx.scene.Node node = (javafx.scene.Node) event.getSource();
            AdolescenteDTO jovemSelecionado = (AdolescenteDTO) node.getUserData();

            if (jovemSelecionado == null) return;

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

    @FXML public void irParaRelatorios(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/RelatoriosView.fxml", "Relatórios"); }
    @FXML public void irParaEquipeTecnica(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/EquipeTecnicaView.fxml", "Equipe Técnica"); }
    @FXML public void irParaAdolescentes(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/AdolescentesView.fxml", "Adolescentes"); }
    @FXML public void irParaListaAdolescentes(MouseEvent event) { NavegacaoUtil.mudarTela(event, "/View/AdolescentesView.fxml", "Adolescentes"); }
    @FXML public void irParaAgenda(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/AgendaView.fxml", "Agenda institucional"); }
    @FXML public void irParaAgendaPeloLink(MouseEvent event) { NavegacaoUtil.mudarTela(event, "/View/AgendaView.fxml", "Agenda institucional"); }
    @FXML public void fazerLogout(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/Login.fxml", "Login"); }
}