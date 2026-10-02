package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.model.Adolescente;
import br.edu.unespar.trabalho.model.StatusAdolescente;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class CadastroAdolescenteController {

    @FXML private TextField txtNome;
    @FXML private TextField txtCpf;
    @FXML private DatePicker dpNascimento;
    @FXML private TextField txtContato;
    @FXML private TextField txtEmail;

    @FXML private TextField txtNaturalidade;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private ComboBox<String> cmbCorRaca;

    private AdolescenteDAO adolescenteDAO = new AdolescenteDAO();

    @FXML
    public void initialize() {
        // Opção "Não informado" ajustada para respeitar o limite de VARCHAR(15) da base de dados
        cmbGenero.setItems(FXCollections.observableArrayList("Masculino", "Feminino", "Outro", "Não informado"));
        cmbCorRaca.setItems(FXCollections.observableArrayList("Branca", "Preta", "Parda", "Amarela", "Indígena"));

        // Aplica as máscaras de formatação em tempo real
        aplicarMascaraCPF(txtCpf);
        aplicarMascaraTelefone(txtContato);
    }

    private void aplicarMascaraCPF(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;

            // Remove tudo o que não for número
            String limpo = newValue.replaceAll("[^0-9]", "");
            if (limpo.length() > 11) limpo = limpo.substring(0, 11);

            // Reconstrói a string com a pontuação
            StringBuilder formatado = new StringBuilder();
            for (int i = 0; i < limpo.length(); i++) {
                if (i == 3 || i == 6) formatado.append(".");
                if (i == 9) formatado.append("-");
                formatado.append(limpo.charAt(i));
            }

            // Atualiza o campo e mantém o cursor no fim
            if (!newValue.equals(formatado.toString())) {
                textField.setText(formatado.toString());
                textField.positionCaret(formatado.length());
            }
        });
    }

    private void aplicarMascaraTelefone(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;

            String limpo = newValue.replaceAll("[^0-9]", "");
            if (limpo.length() > 11) limpo = limpo.substring(0, 11);

            StringBuilder formatado = new StringBuilder();
            for (int i = 0; i < limpo.length(); i++) {
                if (i == 0) formatado.append("(");
                if (i == 2) formatado.append(") ");

                // Lógica dinâmica: ajusta o traço se for telemóvel (11 dígitos) ou telefone fixo (10 dígitos)
                if (limpo.length() == 11 && i == 7) formatado.append("-");
                if (limpo.length() < 11 && i == 6) formatado.append("-");

                formatado.append(limpo.charAt(i));
            }

            if (!newValue.equals(formatado.toString())) {
                textField.setText(formatado.toString());
                textField.positionCaret(formatado.length());
            }
        });
    }

    @FXML
    public void salvarAdolescente(ActionEvent event) {
        try {
            if (txtNome.getText().isEmpty() || txtCpf.getText().isEmpty() || dpNascimento.getValue() == null) {
                mostrarAlerta(Alert.AlertType.WARNING, "Campos Obrigatórios", "Preencha o Nome, CPF e Data de Nascimento.");
                return;
            }

            Adolescente novo = new Adolescente();

            // Como agora usamos máscara, limpamos a pontuação antes de gravar no PostgreSQL (que espera um BIGINT)
            String cpfNumeros = txtCpf.getText().replaceAll("[^0-9]", "");
            novo.setCpf(Long.parseLong(cpfNumeros));

            novo.setNomeCompleto(txtNome.getText());
            novo.setDataNascimento(dpNascimento.getValue());
            novo.setContato(txtContato.getText());
            novo.setEmail(txtEmail.getText());

            novo.setNaturalidade(txtNaturalidade.getText() != null && !txtNaturalidade.getText().isEmpty() ? txtNaturalidade.getText() : "Não informada");
            novo.setGenero(cmbGenero.getValue() != null ? cmbGenero.getValue() : "Não informado");
            novo.setCorRaca(cmbCorRaca.getValue() != null ? cmbCorRaca.getValue() : "Não informada");
            novo.setStatus(StatusAdolescente.ATIVO);

            boolean sucesso = adolescenteDAO.inserir(novo);

            if (sucesso) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Adolescente cadastrado com sucesso!");
                voltarParaLista(event);
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível cadastrar na base de dados. Verifique se o CPF já existe.");
            }
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro de Formatação", "O CPF é inválido.");
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Erro Inesperado", e.getMessage());
        }
    }

    @FXML
    public void voltarParaLista(ActionEvent event) {
        NavegacaoUtil.mudarTela(event, "/View/AdolescentesView.fxml", "Adolescentes");
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    @FXML public void irParaPainel(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Dashboard.fxml", "Painel de Controle"); }
    @FXML public void irParaAgenda(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/AgendaView.fxml", "Agenda institucional"); }
    @FXML public void irParaRelatorios(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/RelatoriosView.fxml", "Relatórios"); }
    @FXML public void irParaEquipeTecnica(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/EquipeTecnicaView.fxml", "Equipe Técnica"); }
    @FXML public void fazerLogout(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Login.fxml", "Login"); }
}