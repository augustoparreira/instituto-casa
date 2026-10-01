package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.model.MembroEquipeDTO;
import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class EquipeTecnicaController {

    @FXML private TextField txtPesquisa;
    @FXML private Label lblTotalRegistros;
    @FXML private ListView<MembroEquipeDTO> listMembros;

    @FXML private Button btnCargoTodos;
    @FXML private Button btnCargoAssistente;
    @FXML private Button btnCargoPsicologo;
    @FXML private Button btnCargoPedagogo;

    private ObservableList<MembroEquipeDTO> listaOriginal;
    private FilteredList<MembroEquipeDTO> listaFiltrada;

    private String filtroCargoAtual = "Todos";

    @FXML
    public void initialize() {
        // Dados de simulação idênticos ao layout da imagem fornecida
        listaOriginal = FXCollections.observableArrayList(
                new MembroEquipeDTO("Ana Paula Costa", "Assistente Social", "ana.costa", "3 adolescente(s)", "Administrador", "AP"),
                new MembroEquipeDTO("Bruno Ferreira", "Psicólogo", "bruno.ferreira", "2 adolescente(s)", "Equipe Técnica", "BF"),
                new MembroEquipeDTO("Carla Mendes", "Pedagoga", "carla.mendes", "2 adolescente(s)", "Equipe Técnica", "CM"),
                new MembroEquipeDTO("Diego Nascimento", "Educador Social", "diego.nascimento", "0 adolescente(s)", "Educador", "DN"),
                new MembroEquipeDTO("Elisa Takahashi", "Coord. Pedagógica", "elisa.takahashi", "0 adolescente(s)", "Administrador", "ET")
        );

        listaFiltrada = new FilteredList<>(listaOriginal, p -> true);

        listMembros.setItems(listaFiltrada);
        listMembros.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(MembroEquipeDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(15);
                    box.setStyle("-fx-alignment: center-left; -fx-padding: 12px 16px; -fx-background-color: white; " +
                            "-fx-background-radius: 8px; -fx-border-color: #e5e5e0; -fx-border-radius: 8px; -fx-cursor: hand;");

                    // Avatar Circular com Iniciais
                    HBox avatarBox = new HBox();
                    avatarBox.setStyle("-fx-alignment: center;");
                    Circle avatar = new Circle(18);
                    avatar.setStyle("-fx-fill: #d1dcd5;");

                    Label lblIniciais = new Label(item.getIniciais());
                    lblIniciais.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #19523e;");

                    // Informações Principais do Membro
                    VBox info = new VBox(2);
                    Label lblNome = new Label(item.getNome());
                    lblNome.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2b2b2b;");
                    Label lblCargo = new Label(item.getCargo());
                    lblCargo.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
                    Label lblDetalhe = new Label(item.getLogin() + "  " + item.getAdolescentesAtendidos());
                    lblDetalhe.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

                    info.getChildren().addAll(lblNome, lblCargo, lblDetalhe);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    // Badge de Nível de Acesso (Administrador / Equipe Técnica / Educador)
                    Label badgeAcesso = new Label(item.getNivelAcesso());
                    String estiloBadge = switch (item.getNivelAcesso()) {
                        case "Administrador" -> "-fx-background-color: #ececec; -fx-text-fill: #555555;";
                        case "Educador" -> "-fx-background-color: #f3e8f8; -fx-text-fill: #8a30b5;";
                        default -> "-fx-background-color: #e8f3ee; -fx-text-fill: #19523e;";
                    };
                    badgeAcesso.setStyle(estiloBadge + " -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 3px 8px; -fx-background-radius: 10px;");

                    box.getChildren().addAll(avatar, info, spacer, badgeAcesso);
                    setGraphic(box);
                }
            }
        });

        atualizarContador();
    }

    @FXML
    public void filtrarMembros() {
        String termo = txtPesquisa.getText().toLowerCase();

        listaFiltrada.setPredicate(membro -> {
            boolean combinaTexto = termo.isEmpty() ||
                    membro.getNome().toLowerCase().contains(termo) ||
                    membro.getCargo().toLowerCase().contains(termo) ||
                    membro.getLogin().toLowerCase().contains(termo);

            boolean combinaCargo = filtroCargoAtual.equals("Todos") ||
                    membro.getCargo().toLowerCase().contains(filtroCargoAtual.toLowerCase());

            return combinaTexto && combinaCargo;
        });

        atualizarContador();
    }

    private void atualizarContador() {
        if (lblTotalRegistros != null) {
            lblTotalRegistros.setText(listaFiltrada.size() + " membro(s) cadastrado(s)");
        }
    }

    // Handlers dos Filtros Por Cargo
    @FXML public void filtrarCargoTodos(ActionEvent e) { filtroCargoAtual = "Todos"; atualizarEstiloBotoesCargo(btnCargoTodos); filtrarMembros(); }
    @FXML public void filtrarCargoAssistente(ActionEvent e) { filtroCargoAtual = "Assistente"; atualizarEstiloBotoesCargo(btnCargoAssistente); filtrarMembros(); }
    @FXML public void filtrarCargoPsicologo(ActionEvent e) { filtroCargoAtual = "Psicólogo"; atualizarEstiloBotoesCargo(btnCargoPsicologo); filtrarMembros(); }
    @FXML public void filtrarCargoPedagogo(ActionEvent e) { filtroCargoAtual = "Pedagog"; atualizarEstiloBotoesCargo(btnCargoPedagogo); filtrarMembros(); }

    private void atualizarEstiloBotoesCargo(Button ativo) {
        if (btnCargoTodos != null) alternarEstilo(btnCargoTodos, false);
        if (btnCargoAssistente != null) alternarEstilo(btnCargoAssistente, false);
        if (btnCargoPsicologo != null) alternarEstilo(btnCargoPsicologo, false);
        if (btnCargoPedagogo != null) alternarEstilo(btnCargoPedagogo, false);

        if (ativo != null) alternarEstilo(ativo, true);
    }

    private void alternarEstilo(Button btn, boolean isAtivo) {
        btn.getStyleClass().remove("filter-btn-active");
        btn.getStyleClass().remove("filter-btn");
        btn.getStyleClass().add(isAtivo ? "filter-btn-active" : "filter-btn");
    }

    // Métodos Globais de Navegação Lateral
    @FXML public void irParaPainel(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Dashboard.fxml", "Painel de Controle"); }
    @FXML public void irParaAdolescentes(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/AdolescentesView.fxml", "Adolescentes"); }
    @FXML public void irParaAgenda(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/AgendaView.fxml", "Agenda institucional"); }
    @FXML public void irParaRelatorios(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/RelatoriosView.fxml", "Relatórios"); }
    @FXML public void irParaEquipeTecnica(ActionEvent e) { /* Tela Atual */}
    @FXML public void fazerLogout(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Login.fxml", "Login"); }

    @FXML public void irParaDashboard(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Dashboard.fxml", "Painel de Controle"); }
}