package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.model.AdolescenteDTO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import br.edu.unespar.trabalho.util.NavegacaoUtil;

public class DetalhesAdolescenteController {

    @FXML private Label lblNome;
    @FXML private Label lblCpf;
    @FXML private Label lblStatus;
    @FXML private Label lblTipoMedida;
    @FXML private Label lblResponsavel;
    @FXML private Label lblPorcentagem;
    @FXML private Label lblHoras;

    @FXML private Label lblInfoNome;
    @FXML private Label lblInfoCpf;
    @FXML private Label lblInfoNascimento;
    @FXML private Label lblInfoGenero;
    @FXML private Label lblInfoBairro;
    @FXML private Label lblInfoStatus;

    public void carregarDados(AdolescenteDTO jovem) {
        if (jovem != null) {
            lblNome.setText(jovem.getNome());
            lblCpf.setText(jovem.getCpf());
            lblStatus.setText(jovem.getStatus());
            lblTipoMedida.setText(jovem.getMedida());
            lblResponsavel.setText("Responsável: " + jovem.getTecnico());
            lblPorcentagem.setText((int)(jovem.getProgresso() * 100) + "%");
            lblHoras.setText(jovem.getTextoProgresso() + " horas");

            lblInfoNome.setText(jovem.getNome());
            lblInfoCpf.setText(jovem.getCpf());
            lblInfoNascimento.setText(jovem.getDataNascimento());
            lblInfoGenero.setText(jovem.getGenero());
            lblInfoBairro.setText(jovem.getBairro());
            lblInfoStatus.setText(jovem.getStatus());
        }
    }

    @FXML
    public void voltarParaLista(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/View/AdolescentesView.fxml"));
            Stage stage = (Stage) lblNome.getScene().getWindow();
            NavegacaoUtil.trocarRaiz(stage, root, "Instituto C.A.S.A. - Adolescentes");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void irParaPainel(ActionEvent e) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/View/Dashboard.fxml"));
            Stage stage = (Stage) lblNome.getScene().getWindow();
            NavegacaoUtil.trocarRaiz(stage, root, "Instituto C.A.S.A. - Painel de Controle");
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}