package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.model.AdolescenteDTO;
import br.edu.unespar.trabalho.model.EventoAgendaDTO;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    @FXML private Label lblNomeUsuario;
    @FXML private Label lblCargoUsuario;
    @FXML private Label lblDataAtualizacao;

    // Estatísticas Numéricas
    @FXML private Label lblTotalCadastrados;
    @FXML private Label lblTotalPsc;
    @FXML private Label lblTotalLa;

    // Contêineres Dinâmicos
    @FXML private VBox vboxAdolescentes;
    @FXML private VBox vboxAgenda;

    // Cartão de Alerta (Prazo próximo)
    @FXML private VBox cardPrazoGabriel;
    @FXML private Label lblNomeAlerta;
    @FXML private Label lblDataAlerta;

    private AdolescenteDAO adolescenteDAO = new AdolescenteDAO();

    @FXML
    public void initialize() {
        if (lblDataAtualizacao != null) {
            lblDataAtualizacao.setText("Atualizado em " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy")));
        }

        carregarEstatisticasEAdolescentes();
        carregarAgendaSemanal();
    }

    private void carregarEstatisticasEAdolescentes() {
        List<AdolescenteDTO> jovensCadastrados = adolescenteDAO.listarResumoDTO();

        long totalPsc = jovensCadastrados.stream().filter(j -> "PSC".equalsIgnoreCase(j.getMedida())).count();
        long totalLa = jovensCadastrados.stream().filter(j -> "LA".equalsIgnoreCase(j.getMedida())).count();

        if (lblTotalCadastrados != null) lblTotalCadastrados.setText(String.valueOf(jovensCadastrados.size()));
        if (lblTotalPsc != null) lblTotalPsc.setText(String.valueOf(totalPsc));
        if (lblTotalLa != null) lblTotalLa.setText(String.valueOf(totalLa));

        if (vboxAdolescentes != null) {
            vboxAdolescentes.getChildren().clear();
        }

        for (int i = 0; i < jovensCadastrados.size(); i++) {
            AdolescenteDTO jovem = jovensCadastrados.get(i);

            // Linha do Adolescente
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: white; -fx-padding: 10 15; -fx-border-color: #eeeeee; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
            row.setUserData(jovem);
            row.setOnMouseClicked(this::clicarAdolescentePainel);

            // Avatar com Iniciais
            StackPane avatar = new StackPane();
            Circle circle = new Circle(18, Color.web("#e8f3ee"));
            Label initials = new Label(getIniciais(jovem.getNome()));
            initials.setStyle("-fx-text-fill: #19523e; -fx-font-weight: bold; -fx-font-size: 12px;");
            avatar.getChildren().addAll(circle, initials);

            // Textos e Barra de Progresso
            VBox infoBox = new VBox(4);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Label lblNome = new Label(jovem.getNome());
            lblNome.setStyle("-fx-font-weight: bold; -fx-text-fill: #2b2b2b; -fx-font-size: 13px;");

            Label lblTech = new Label(jovem.getTecnico() != null ? jovem.getTecnico() : "Sem técnico");
            lblTech.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");

            HBox progressBox = new HBox(10);
            progressBox.setAlignment(Pos.CENTER_LEFT);

            // Usando o getProgresso() (double) do DTO
            ProgressBar pb = new ProgressBar(jovem.getProgresso());
            pb.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(pb, Priority.ALWAYS);
            pb.setStyle("-fx-accent: #0f8558; -fx-control-inner-background: #e8f3ee;");

            // Usando o getTextoProgresso() (String) do DTO
            Label lblProgText = new Label(jovem.getTextoProgresso());
            lblProgText.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px; -fx-font-weight: bold;");
            progressBox.getChildren().addAll(pb, lblProgText);

            infoBox.getChildren().addAll(lblNome, lblTech, progressBox);

            // Badge de Medida
            Label lblBadge = new Label(jovem.getMedida());
            if ("LA".equalsIgnoreCase(jovem.getMedida())) {
                lblBadge.setStyle("-fx-background-color: #f3e8f3; -fx-text-fill: #8d118d; -fx-padding: 4 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                lblBadge.setStyle("-fx-background-color: #e8f0f3; -fx-text-fill: #11598d; -fx-padding: 4 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");
            }

            Label chevron = new Label(">");
            chevron.setStyle("-fx-text-fill: #a0a0a0; -fx-font-weight: bold;");

            row.getChildren().addAll(avatar, infoBox, lblBadge, chevron);

            if (vboxAdolescentes != null) {
                vboxAdolescentes.getChildren().add(row);
            }

            // Define o alerta do primeiro jovem
            if (i == 0 && cardPrazoGabriel != null) {
                cardPrazoGabriel.setUserData(jovem);
                cardPrazoGabriel.setVisible(true);
                cardPrazoGabriel.setManaged(true);
                lblNomeAlerta.setText(jovem.getNome());
                lblDataAlerta.setText("Nascimento: " + jovem.getDataNascimento());
            }
        }
    }

    private void carregarAgendaSemanal() {
        if (vboxAgenda == null) return;
        vboxAgenda.getChildren().clear();

        List<EventoAgendaDTO> agenda = new ArrayList<>();
        agenda.add(new EventoAgendaDTO(25, "09h00", "Visita domiciliar", "Lucas Oliveira"));
        agenda.add(new EventoAgendaDTO(26, "10h30", "Reunião PIA", "Mariana Santos"));
        agenda.add(new EventoAgendaDTO(27, "14h00", "Entrega relatório", "Ass. Carla"));
        agenda.add(new EventoAgendaDTO(28, "09h00", "Audiência judicial", "Felipe Souza"));

        for (EventoAgendaDTO evento : agenda) {
            HBox agendaRow = new HBox(12);
            agendaRow.setAlignment(Pos.CENTER_LEFT);
            agendaRow.setStyle("-fx-border-color: transparent transparent #eeeeee transparent; -fx-border-width: 0 0 1 0; -fx-padding: 5 0 8 0;");

            VBox dataBox = new VBox(2);
            dataBox.setAlignment(Pos.CENTER);
            Label lblDia = new Label(evento.getDia() + " Jun");
            lblDia.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 11px;");
            Label lblHora = new Label(evento.getHorario());
            lblHora.setStyle("-fx-text-fill: #19523e; -fx-font-weight: bold; -fx-font-size: 12px;");
            dataBox.getChildren().addAll(lblDia, lblHora);

            VBox infoBox = new VBox(2);
            Label lblTitulo = new Label(evento.getTitulo());
            lblTitulo.setStyle("-fx-text-fill: #2b2b2b; -fx-font-weight: bold; -fx-font-size: 12px;");
            Label lblSub = new Label(evento.getTipo());
            lblSub.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");
            infoBox.getChildren().addAll(lblTitulo, lblSub);

            agendaRow.getChildren().addAll(dataBox, infoBox);
            vboxAgenda.getChildren().add(agendaRow);
        }
    }

    private String getIniciais(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.trim().isEmpty()) return "N/A";
        String[] partes = nomeCompleto.trim().split("\\s+");
        if (partes.length == 1) return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
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

            Stage stage = (Stage) lblTotalCadastrados.getScene().getWindow();
            NavegacaoUtil.trocarRaiz(stage, root, "Instituto C.A.S.A. - Perfil de " + jovemSelecionado.getNome());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void clicarAdolescenteAlerta(MouseEvent event) {
        clicarAdolescentePainel(event);
    }

    @FXML public void irParaRelatorios(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/RelatoriosView.fxml", "Relatórios"); }
    @FXML public void irParaEquipeTecnica(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/EquipeTecnicaView.fxml", "Equipe Técnica"); }
    @FXML public void irParaAdolescentes(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/AdolescentesView.fxml", "Adolescentes"); }
    @FXML public void irParaListaAdolescentes(MouseEvent event) { NavegacaoUtil.mudarTela(event, "/View/AdolescentesView.fxml", "Adolescentes"); }
    @FXML public void irParaAgenda(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/AgendaView.fxml", "Agenda institucional"); }
    @FXML public void irParaAgendaPeloLink(MouseEvent event) { NavegacaoUtil.mudarTela(event, "/View/AgendaView.fxml", "Agenda institucional"); }
    @FXML public void fazerLogout(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/Login.fxml", "Login"); }
}