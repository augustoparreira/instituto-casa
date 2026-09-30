package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.model.EventoAgendaDTO;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AgendaController {

    @FXML private GridPane gridCalendario;
    @FXML private Label lblMesAno;
    @FXML private ComboBox<Integer> cmbAno;
    @FXML private Label lblDataSelecionada;
    @FXML private Label lblDiaSemanaSelecionado;
    @FXML private Label lblIrParaHojeTexto;
    @FXML private VBox listaEventosPainel;

    private List<EventoAgendaDTO> eventosMock;
    private LocalDate dataAtualVisualizacao = LocalDate.now();
    private LocalDate dataSelecionada = LocalDate.now();
    private boolean carregandoCombo = false;

    @FXML
    public void initialize() {
        carregarEventosMock();
        popularComboBoxAnos();
        atualizarCalendario();
        atualizarPainelLateral(dataSelecionada);

        if (lblIrParaHojeTexto != null) {
            lblIrParaHojeTexto.setText("Ir para hoje — " + String.format("%02d", LocalDate.now().getDayOfMonth()) + "/" + String.format("%02d", LocalDate.now().getMonthValue()) + "/" + LocalDate.now().getYear());
        }
    }

    private void carregarEventosMock() {
        eventosMock = new ArrayList<>();
        eventosMock.add(new EventoAgendaDTO(3, "09h00", "Atendimento", "atendimento"));
        eventosMock.add(new EventoAgendaDTO(5, "14h00", "Visita dom.", "visita"));
        eventosMock.add(new EventoAgendaDTO(10, "08h30", "Registro freq.", "visita"));
        eventosMock.add(new EventoAgendaDTO(25, "09h00", "Reunião equipe", "reuniao"));
        eventosMock.add(new EventoAgendaDTO(LocalDate.now().getDayOfMonth(), "10h00", "Compromisso de Hoje", "pia"));
    }

    private void popularComboBoxAnos() {
        carregandoCombo = true;
        List<Integer> anos = new ArrayList<>();
        for (int a = 2024; a <= 2030; a++) {
            anos.add(a);
        }
        cmbAno.setItems(FXCollections.observableArrayList(anos));
        cmbAno.setValue(dataAtualVisualizacao.getYear());
        carregandoCombo = false;
    }

    private void atualizarCalendario() {
        gridCalendario.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);

        String nomeMes = dataAtualVisualizacao.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        lblMesAno.setText(nomeMes.substring(0, 1).toUpperCase() + nomeMes.substring(1));

        if (cmbAno != null && !carregandoCombo) {
            carregandoCombo = true;
            cmbAno.setValue(dataAtualVisualizacao.getYear());
            carregandoCombo = false;
        }

        int primeiroDiaSemana = dataAtualVisualizacao.withDayOfMonth(1).getDayOfWeek().getValue() % 7;
        int totalDiasMes = dataAtualVisualizacao.lengthOfMonth();

        int diaContador = 1;
        for (int linha = 1; linha <= 5; linha++) {
            for (int coluna = 0; coluna < 7; coluna++) {

                if (linha == 1 && coluna < primeiroDiaSemana) {
                    continue;
                }

                if (diaContador <= totalDiasMes) {
                    final int d = diaContador;
                    LocalDate dataCelula = LocalDate.of(dataAtualVisualizacao.getYear(), dataAtualVisualizacao.getMonth(), d);

                    VBox celula = new VBox(4);
                    celula.setStyle("-fx-cursor: hand;");

                    if (dataCelula.equals(dataSelecionada)) {
                        celula.getStyleClass().add("day-cell-today");
                    } else {
                        celula.getStyleClass().add("day-cell");
                    }

                    Label lblDia = new Label(String.valueOf(d));
                    if (dataCelula.equals(LocalDate.now())) {
                        lblDia.getStyleClass().add("day-number-today");
                    } else {
                        lblDia.getStyleClass().add("day-number");
                    }
                    celula.getChildren().add(lblDia);

                    for (EventoAgendaDTO ev : eventosMock) {
                        if (ev.getDia() == d) {
                            Label lblEv = new Label(ev.getHorario() + " " + ev.getTitulo());
                            lblEv.getStyleClass().add(getEstiloTag(ev.getTipo()));
                            celula.getChildren().add(lblEv);
                        }
                    }

                    celula.setOnMouseClicked(event -> {
                        dataSelecionada = dataCelula;
                        atualizarCalendario();
                        atualizarPainelLateral(dataSelecionada);
                    });

                    gridCalendario.add(celula, coluna, linha);
                    diaContador++;
                }
            }
        }
    }

    private void atualizarPainelLateral(LocalDate data) {
        String mesStr = data.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        mesStr = mesStr.substring(0, 1).toUpperCase() + mesStr.substring(1);

        lblDataSelecionada.setText(data.getDayOfMonth() + " de " + mesStr + " de " + data.getYear());

        String diaSemana = data.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        lblDiaSemanaSelecionado.setText(diaSemana.substring(0, 1).toUpperCase() + diaSemana.substring(1));

        listaEventosPainel.getChildren().clear();

        boolean temEvento = false;
        for (EventoAgendaDTO ev : eventosMock) {
            if (ev.getDia() == data.getDayOfMonth()) {
                temEvento = true;
                VBox card = new VBox(3);
                card.getStyleClass().add(getEstiloCard(ev.getTipo()));

                Label lblHorario = new Label(ev.getHorario());
                lblHorario.getStyleClass().add(getEstiloHora(ev.getTipo()));

                Label lblTit = new Label(ev.getTitulo());
                lblTit.getStyleClass().add("event-title-bold");

                card.getChildren().addAll(lblHorario, lblTit);
                listaEventosPainel.getChildren().add(card);
            }
        }

        if (!temEvento) {
            Label vazio = new Label("Nenhum compromisso agendado.");
            vazio.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
            listaEventosPainel.getChildren().add(vazio);
        }
    }

    @FXML
    public void mesAnterior(ActionEvent event) {
        dataAtualVisualizacao = dataAtualVisualizacao.minusMonths(1);
        atualizarCalendario();
    }

    @FXML
    public void proximoMes(ActionEvent event) {
        dataAtualVisualizacao = dataAtualVisualizacao.plusMonths(1);
        atualizarCalendario();
    }

    @FXML
    public void mudarAnoCombo(ActionEvent event) {
        if (carregandoCombo) return;
        Integer anoSelecionado = cmbAno.getValue();
        if (anoSelecionado != null) {
            dataAtualVisualizacao = LocalDate.of(anoSelecionado, dataAtualVisualizacao.getMonth(), 1);
            atualizarCalendario();
        }
    }

    @FXML
    public void irParaHoje(javafx.scene.input.MouseEvent event) {
        dataAtualVisualizacao = LocalDate.now();
        dataSelecionada = LocalDate.now();
        atualizarCalendario();
        atualizarPainelLateral(dataSelecionada);
    }

    @FXML
    public void abrirModalNovoEvento(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Novo Evento");
        alert.setHeaderText(null);
        alert.setContentText("Formulário de cadastro de novo compromisso na agenda.");
        alert.showAndWait();
    }

    private String getEstiloTag(String tipo) {
        return switch (tipo) {
            case "visita" -> "event-tag-green";
            case "atendimento" -> "event-tag-blue";
            case "reuniao" -> "event-tag-purple";
            case "pia" -> "event-tag-orange";
            case "audiencia" -> "event-tag-red";
            default -> "event-tag-blue";
        };
    }

    private String getEstiloCard(String tipo) {
        return switch (tipo) {
            case "visita" -> "event-card-green";
            case "reuniao" -> "event-card-purple";
            default -> "event-card-purple";
        };
    }

    private String getEstiloHora(String tipo) {
        return switch (tipo) {
            case "visita" -> "event-time-green";
            case "reuniao" -> "event-time-purple";
            default -> "event-time-purple";
        };
    }

    @FXML public void irParaPainel(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Dashboard.fxml", "Painel de Controle"); }
    @FXML public void irParaAdolescentes(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/AdolescentesView.fxml", "Adolescentes"); }
    @FXML public void fazerLogout(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Login.fxml", "Login"); }
}