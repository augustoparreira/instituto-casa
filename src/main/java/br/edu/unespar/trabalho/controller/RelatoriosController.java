package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.dao.RelatorioDAO;
import br.edu.unespar.trabalho.model.RelatorioItem;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class RelatoriosController {

    @FXML private Label lblNomeUsuario;
    @FXML private Label lblCargoUsuario;
    @FXML private Label lblNivelAcessoInfo;
    @FXML private Label lblStatusEdicao;

    @FXML private Button btnNovoRelatorio;
    @FXML private Button btnSalvarAtualizacao;
    @FXML private Button btnExportar;
    @FXML private Button btnExcluirRelatorio;

    @FXML private ComboBox<String> cmbTipoFiltro;
    @FXML private ListView<RelatorioItem> listRelatorios;

    @FXML private TextField txtTituloRelatorio;
    @FXML private TextField txtCategoriaRelatorio;
    @FXML private TextArea txtParecerTecnico;

    // Níveis de acesso aceitos: "Administrador", "Equipe Técnica" ou "Educador"
    private String nivelAcessoUsuario = "Administrador";

    private final RelatorioDAO relatorioDAO = RelatorioDAO.getInstance();

    @FXML
    public void initialize() {
        if (cmbTipoFiltro != null) {
            cmbTipoFiltro.setItems(FXCollections.observableArrayList("Todos", "Prestação de Serviços (PSC)", "Liberdade Assistida (LA)", "Geral"));
            cmbTipoFiltro.getSelectionModel().selectFirst();
        }

        // Associa os dados persistentes do DAO à ListView
        listRelatorios.setItems(relatorioDAO.getListaRelatorios());

        // Customização da exibição de cada item na lista
        listRelatorios.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(RelatorioItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(3);
                    box.setStyle("-fx-padding: 8px; -fx-background-color: white; -fx-border-color: #e5e5e0; -fx-border-radius: 6px; -fx-background-radius: 6px;");

                    Label lblTitulo = new Label(item.getTitulo());
                    lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2b2b2b;");

                    Label lblSub = new Label(item.getCategoria() + " • " + item.getData());
                    lblSub.setStyle("-fx-text-fill: #777777; -fx-font-size: 10px;");

                    box.getChildren().addAll(lblTitulo, lblSub);
                    setGraphic(box);
                }
            }
        });

        // Evento de seleção: popula o formulário com os dados exatos do item selecionado
        listRelatorios.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtTituloRelatorio.setText(newVal.getTitulo());
                txtCategoriaRelatorio.setText(newVal.getCategoria());
                txtParecerTecnico.setText(newVal.getParecer());
            } else {
                limparFormulario();
            }
        });

        aplicarPermissoesPorNivel();

        // Seleciona o primeiro elemento da lista, se existir
        if (!listRelatorios.getItems().isEmpty()) {
            listRelatorios.getSelectionModel().selectFirst();
        }
    }

    private void aplicarPermissoesPorNivel() {
        boolean eAdmin = "Administrador".equalsIgnoreCase(nivelAcessoUsuario);
        boolean eEquipeTecnica = "Equipe Técnica".equalsIgnoreCase(nivelAcessoUsuario);
        boolean podeEditar = eAdmin || eEquipeTecnica;

        if (lblNivelAcessoInfo != null) {
            lblNivelAcessoInfo.setText("Nível de Acesso: " + nivelAcessoUsuario +
                    (eAdmin ? " (Acesso Total)" : eEquipeTecnica ? " (Edição do Parecer)" : " (Apenas Visualização)"));
        }

        // Permissões por campo
        txtTituloRelatorio.setEditable(eAdmin);
        txtCategoriaRelatorio.setEditable(eAdmin);
        txtParecerTecnico.setEditable(podeEditar);

        btnSalvarAtualizacao.setDisable(!podeEditar);
        btnNovoRelatorio.setVisible(eAdmin);

        btnExcluirRelatorio.setVisible(eAdmin);
        btnExcluirRelatorio.setManaged(eAdmin);

        if (lblStatusEdicao != null) {
            lblStatusEdicao.setText(podeEditar ? "Modo: Edição Habilitada" : "Modo: Somente Leitura");
        }
    }

    @FXML
    public void salvarAtualizacaoRelatorio(ActionEvent event) {
        RelatorioItem selecionado = listRelatorios.getSelectionModel().getSelectedItem();

        if (selecionado == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Selecione um relatório na lista para salvar as alterações.", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        boolean eAdmin = "Administrador".equalsIgnoreCase(nivelAcessoUsuario);
        boolean eEquipeTecnica = "Equipe Técnica".equalsIgnoreCase(nivelAcessoUsuario);

        if (!eAdmin && !eEquipeTecnica) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Seu nível de acesso não permite alterar relatórios.", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        // Atualiza os dados no objeto existente sem perdê-los ou criar duplicatas
        if (eAdmin) {
            selecionado.setTitulo(txtTituloRelatorio.getText());
            selecionado.setCategoria(txtCategoriaRelatorio.getText());
        }

        selecionado.setParecer(txtParecerTecnico.getText());

        // Força a re-renderização do item atualizado na ListView
        listRelatorios.refresh();

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Alterações salvas com sucesso!", ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    public void gerarNovoRelatorio(ActionEvent event) {
        if (!"Administrador".equalsIgnoreCase(nivelAcessoUsuario)) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Apenas administradores podem criar relatórios.", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        String dataHoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        RelatorioItem novo = new RelatorioItem("Novo Relatório de Acompanhamento", "Geral", "Insira aqui o parecer técnico...", dataHoje);

        relatorioDAO.adicionar(novo);
        listRelatorios.getSelectionModel().select(novo);
    }

    @FXML
    public void excluirRelatorio(ActionEvent event) {
        RelatorioItem selecionado = listRelatorios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Selecione um relatório para excluir.", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja remover o relatório: '" + selecionado.getTitulo() + "'?", ButtonType.YES, ButtonType.NO);
        confirmacao.setHeaderText("Confirmar Exclusão");
        Optional<ButtonType> resultado = confirmacao.showAndWait();

        if (resultado.isPresent() && resultado.get() == ButtonType.YES) {
            relatorioDAO.remover(selecionado);
            limparFormulario();
        }
    }

    private void limparFormulario() {
        txtTituloRelatorio.clear();
        txtCategoriaRelatorio.clear();
        txtParecerTecnico.clear();
    }

    @FXML
    public void exportarPDF(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exportando relatório em formato PDF...", ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Navegação Lateral
    @FXML public void irParaPainel(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Dashboard.fxml", "Painel de Controle"); }
    @FXML public void irParaAdolescentes(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/AdolescentesView.fxml", "Adolescentes"); }
    @FXML public void irParaAgenda(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/AgendaView.fxml", "Agenda institucional"); }
    @FXML public void irParaRelatorios(ActionEvent e) { /* Tela Atual */ }
    @FXML public void irParaEquipeTecnica(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/EquipeTecnicaView.fxml", "Equipe Técnica"); }
    @FXML public void fazerLogout(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Login.fxml", "Login"); }
}