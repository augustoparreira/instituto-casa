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
import br.edu.unespar.trabalho.model.AdolescenteDTO;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.time.LocalDate;

public class CadastroAdolescenteController {

    @FXML private TextField txtNome;
    @FXML private TextField txtCpf;
    @FXML private DatePicker dpNascimento;
    @FXML private TextField txtContato;
    @FXML private TextField txtEmail;

    @FXML private TextField txtNaturalidade;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private ComboBox<String> cmbCorRaca;
    @FXML private TextField txtBairro; // NOVO CAMPO BAIRRO LIGADO AO FXML

    private AdolescenteDAO adolescenteDAO = new AdolescenteDAO();

    @FXML
    public void initialize() {
        cmbGenero.setItems(FXCollections.observableArrayList("Masculino", "Feminino", "Outro", "Não informado"));
        cmbCorRaca.setItems(FXCollections.observableArrayList("Branca", "Preta", "Parda", "Amarela", "Indígena"));

        aplicarMascaraCPF(txtCpf);
        aplicarMascaraTelefone(txtContato);
    }

    private void aplicarMascaraCPF(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            String limpo = newValue.replaceAll("[^0-9]", "");
            if (limpo.length() > 11) limpo = limpo.substring(0, 11);

            StringBuilder formatado = new StringBuilder();
            for (int i = 0; i < limpo.length(); i++) {
                if (i == 3 || i == 6) formatado.append(".");
                if (i == 9) formatado.append("-");
                formatado.append(limpo.charAt(i));
            }

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

            if (dpNascimento.getValue().isAfter(LocalDate.now())) {
                mostrarAlerta(Alert.AlertType.WARNING, "Data Inválida", "A data de nascimento não pode ser no futuro.");
                return;
            }

            String emailDigitado = txtEmail.getText();
            if (emailDigitado != null && !emailDigitado.trim().isEmpty() && !emailDigitado.contains("@")) {
                mostrarAlerta(Alert.AlertType.WARNING, "E-mail Inválido", "O campo de e-mail deve obrigatoriamente conter um '@'.");
                return;
            }

            String cpfNumeros = txtCpf.getText().replaceAll("[^0-9]", "");
            if (cpfNumeros.length() != 11) {
                mostrarAlerta(Alert.AlertType.WARNING, "CPF inválido", "O CPF deve ter 11 dígitos.");
                return;
            }

            Adolescente novo = new Adolescente();
            novo.setCpf(Long.parseLong(cpfNumeros));

            novo.setNomeCompleto(txtNome.getText());
            novo.setDataNascimento(dpNascimento.getValue());
            novo.setContato(txtContato.getText());
            novo.setEmail(txtEmail.getText());

            novo.setNaturalidade(txtNaturalidade.getText() != null && !txtNaturalidade.getText().isEmpty() ? txtNaturalidade.getText() : "Não informada");
            novo.setGenero(cmbGenero.getValue() != null ? cmbGenero.getValue() : "Não informado");
            novo.setCorRaca(cmbCorRaca.getValue() != null ? cmbCorRaca.getValue() : "Não informada");
            novo.setBairro(txtBairro.getText()); // SALVA O BAIRRO NO MODELO
            novo.setStatus(StatusAdolescente.ATIVO);

            boolean sucesso = adolescenteDAO.inserir(novo);

            if (sucesso) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso",
                        "Adolescente cadastrado! Agora cadastre a medida socioeducativa e o técnico de referência.");
                abrirPerfil(event, novo.getCpf());
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível cadastrar na base de dados. Verifique se o CPF já existe.");
            }
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro de Formatação", "O CPF é inválido.");
        } catch (RuntimeException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Banco de dados indisponível",
                    "Não foi possível acessar o banco de dados.\nVerifique se o PostgreSQL está ligado e tente novamente.");
        }
    }

    private void abrirPerfil(ActionEvent event, long cpf) {
        try {
            String cpfDigitos = String.format("%011d", cpf);
            AdolescenteDTO dto = null;
            for (AdolescenteDTO d : adolescenteDAO.listarResumoDTO()) {
                if (d.getCpf().replaceAll("\\D", "").equals(cpfDigitos)) {
                    dto = d;
                    break;
                }
            }
            if (dto == null) {
                voltarParaLista(event);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/DetalhesAdolescenteView.fxml"));
            Parent root = loader.load();
            DetalhesAdolescenteController controller = loader.getController();
            controller.carregarDados(dto);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            NavegacaoUtil.trocarRaiz(stage, root, "Instituto C.A.S.A. - Perfil de " + dto.getNome());
        } catch (Exception e) {
            e.printStackTrace();
            voltarParaLista(event);
        }
    }

    @FXML public void voltarParaLista(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/AdolescentesView.fxml", "Adolescentes"); }

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