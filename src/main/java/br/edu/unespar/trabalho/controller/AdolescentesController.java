package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.model.AdolescenteDTO;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class AdolescentesController {

    @FXML private TextField txtPesquisa;
    @FXML private Label lblTotalRegistros;
    @FXML private ListView<AdolescenteDTO> listAdolescentes;

    @FXML private Button btnTipoTodos;
    @FXML private Button btnTipoPsc;
    @FXML private Button btnTipoLa;

    @FXML private Button btnStatusTodos;
    @FXML private Button btnStatusAtivo;
    @FXML private Button btnStatusSuspenso;
    @FXML private Button btnStatusEncerrado;

    private ObservableList<AdolescenteDTO> listaOriginal;
    private FilteredList<AdolescenteDTO> listaFiltrada;

    private String filtroTipoAtual = "Todos";
    private String filtroStatusAtual = "Todos";

    @FXML
    public void initialize() {
        listaOriginal = FXCollections.observableArrayList(
                new AdolescenteDTO("Lucas Henrique Oliveira", "123.456.789-00", "PSC", 0.40, "48/120h", "Dra. Ana Paula Costa", "Ativo", "Balneário Praia Grande", "14/03/2007", "Masculino"),
                new AdolescenteDTO("Mariana dos Santos Silva", "234.567.890-11", "LA", 0.25, "3/12h", "Psic. Bruno Ferreira", "Ativo", "Centro", "22/09/2008", "Feminino"),
                new AdolescenteDTO("Gabriel Alves Pereira", "345.678.901-22", "PSC", 1.00, "90/90h", "Ass. Soc. Carla Mendes", "Ativo", "Jardim Progresso", "05/11/2006", "Masculino"),
                new AdolescenteDTO("Isabela Rocha Cardoso", "456.789.012-33", "LA", 0.41, "5/12h", "Dra. Ana Paula Costa", "Suspenso", "Vila Operária", "18/01/2007", "Feminino"),
                new AdolescenteDTO("Felipe Martins Souza", "567.890.123-44", "PSC", 0.36, "22/60h", "Psic. Bruno Ferreira", "Ativo", "Parque da Fonte", "30/07/2008", "Masculino"),
                new AdolescenteDTO("Rafaela Lima Torres", "678.901.234-55", "PSC", 1.00, "60/60h", "Ass. Soc. Carla Mendes", "Encerrado", "Centro", "12/12/2006", "Feminino")
        );

        listaFiltrada = new FilteredList<>(listaOriginal, p -> true);

        listAdolescentes.setItems(listaFiltrada);
        listAdolescentes.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(AdolescenteDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(15);
                    box.setStyle("-fx-alignment: center-left; -fx-padding: 10px; -fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #e5e5e0; -fx-border-radius: 8px; -fx-cursor: hand;");

                    Circle avatar = new Circle(20);
                    avatar.setStyle("-fx-fill: #d1dcd5;");

                    VBox info = new VBox(2);
                    Label lblNome = new Label(item.getNome());
                    lblNome.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2b2b2b;");
                    Label lblCpf = new Label(item.getCpf() + " • " + item.getBairro());
                    lblCpf.setStyle("-fx-text-fill: #777777; -fx-font-size: 11px;");
                    info.getChildren().addAll(lblNome, lblCpf);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label badgeMedida = new Label(item.getMedida());
                    badgeMedida.setStyle(item.getMedida().equals("PSC") ?
                            "-fx-background-color: #e3edf7; -fx-text-fill: #2a75bb; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 3px 8px; -fx-background-radius: 10px;" :
                            "-fx-background-color: #f3e8f9; -fx-text-fill: #7b4397; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 3px 8px; -fx-background-radius: 10px;");

                    Label lblProgresso = new Label(item.getTextoProgresso());
                    lblProgresso.setStyle("-fx-text-fill: #555555; -fx-font-size: 12px; -fx-font-weight: bold;");

                    Label lblTecnico = new Label(item.getTecnico());
                    lblTecnico.setStyle("-fx-text-fill: #777777; -fx-font-size: 12px;");
                    lblTecnico.setPrefWidth(160);

                    Label badgeStatus = new Label(item.getStatus());
                    String corStatus = switch (item.getStatus()) {
                        case "Ativo" -> "-fx-background-color: #e8f8f0; -fx-text-fill: #27ae60;";
                        case "Suspenso" -> "-fx-background-color: #fef9e7; -fx-text-fill: #d4ac0d;";
                        default -> "-fx-background-color: #f2f2f2; -fx-text-fill: #777777;";
                    };
                    badgeStatus.setStyle(corStatus + " -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 3px 8px; -fx-background-radius: 10px;");

                    box.getChildren().addAll(avatar, info, spacer, badgeMedida, lblProgresso, lblTecnico, badgeStatus);
                    setGraphic(box);
                }
            }
        });
    }

    @FXML
    public void filtrarAdolescentes() {
        String termo = txtPesquisa.getText().toLowerCase();

        listaFiltrada.setPredicate(jovem -> {
            boolean combinaTexto = termo.isEmpty() ||
                    jovem.getNome().toLowerCase().contains(termo) ||
                    jovem.getCpf().contains(termo) ||
                    jovem.getBairro().toLowerCase().contains(termo);

            boolean combinaTipo = filtroTipoAtual.equals("Todos") || jovem.getMedida().equalsIgnoreCase(filtroTipoAtual);
            boolean combinaStatus = filtroStatusAtual.equals("Todos") || jovem.getStatus().equalsIgnoreCase(filtroStatusAtual);

            return combinaTexto && combinaTipo && combinaStatus;
        });

        lblTotalRegistros.setText(listaFiltrada.size() + " registro(s) encontrado(s)");
    }

    @FXML public void filtrarTipoTodos(ActionEvent e) { filtroTipoAtual = "Todos"; atualizarEstiloBotoesTipo(btnTipoTodos); filtrarAdolescentes(); }
    @FXML public void filtrarTipoPsc(ActionEvent e) { filtroTipoAtual = "PSC"; atualizarEstiloBotoesTipo(btnTipoPsc); filtrarAdolescentes(); }
    @FXML public void filtrarTipoLa(ActionEvent e) { filtroTipoAtual = "LA"; atualizarEstiloBotoesTipo(btnTipoLa); filtrarAdolescentes(); }

    private void atualizarEstiloBotoesTipo(Button ativo) {
        btnTipoTodos.getStyleClass().remove("filter-btn-active"); btnTipoTodos.getStyleClass().add("filter-btn");
        btnTipoPsc.getStyleClass().remove("filter-btn-active"); btnTipoPsc.getStyleClass().add("filter-btn");
        btnTipoLa.getStyleClass().remove("filter-btn-active"); btnTipoLa.getStyleClass().add("filter-btn");
        ativo.getStyleClass().remove("filter-btn"); ativo.getStyleClass().add("filter-btn-active");
    }

    @FXML public void filtrarStatusTodos(ActionEvent e) { filtroStatusAtual = "Todos"; atualizarEstiloBotoesStatus(btnStatusTodos); filtrarAdolescentes(); }
    @FXML public void filtrarStatusAtivo(ActionEvent e) { filtroStatusAtual = "Ativo"; atualizarEstiloBotoesStatus(btnStatusAtivo); filtrarAdolescentes(); }
    @FXML public void filtrarStatusSuspenso(ActionEvent e) { filtroStatusAtual = "Suspenso"; atualizarEstiloBotoesStatus(btnStatusSuspenso); filtrarAdolescentes(); }
    @FXML public void filtrarStatusEncerrado(ActionEvent e) { filtroStatusAtual = "Encerrado"; atualizarEstiloBotoesStatus(btnStatusEncerrado); filtrarAdolescentes(); }

    private void atualizarEstiloBotoesStatus(Button ativo) {
        btnStatusTodos.getStyleClass().remove("filter-btn-active"); btnStatusTodos.getStyleClass().add("filter-btn");
        btnStatusAtivo.getStyleClass().remove("filter-btn-active"); btnStatusAtivo.getStyleClass().add("filter-btn");
        btnStatusSuspenso.getStyleClass().remove("filter-btn-active"); btnStatusSuspenso.getStyleClass().add("filter-btn");
        btnStatusEncerrado.getStyleClass().remove("filter-btn-active"); btnStatusEncerrado.getStyleClass().add("filter-btn");
        ativo.getStyleClass().remove("filter-btn"); ativo.getStyleClass().add("filter-btn-active");
    }

    @FXML
    public void clicarAdolescente(MouseEvent event) {
        AdolescenteDTO selecionado = listAdolescentes.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/DetalhesAdolescenteView.fxml"));
                Parent root = loader.load();

                DetalhesAdolescenteController detalheController = loader.getController();
                detalheController.carregarDados(selecionado);

                Stage stage = (Stage) listAdolescentes.getScene().getWindow();
                NavegacaoUtil.trocarRaiz(stage, root, "Instituto C.A.S.A. - Perfil de " + selecionado.getNome());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML public void irParaPainel(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Dashboard.fxml", "Painel de Controle"); }
    @FXML public void irParaAgenda(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/AgendaView.fxml", "Agenda institucional"); }
    @FXML public void fazerLogout(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Login.fxml", "Login"); }
}