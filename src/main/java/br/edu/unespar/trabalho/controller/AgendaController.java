package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.dao.*;
import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import br.edu.unespar.trabalho.util.Sessao;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AgendaController {
    @FXML private GridPane gridCalendario;
    @FXML private Label lblMesAno, lblDataSelecionada, lblDiaSemanaSelecionado, lblIrParaHojeTexto, lblMensagem;
    @FXML private Label lblNomeUsuario, lblCargoUsuario;
    @FXML private ComboBox<Integer> cmbAno;
    @FXML private CheckBox chkCancelados;
    @FXML private VBox listaEventosPainel;
    private final EventoAgendaDAO dao=new EventoAgendaDAO();
    private List<EventoAgenda> eventos=List.of();
    private LocalDate dataAtualVisualizacao=LocalDate.now().withDayOfMonth(1);
    private LocalDate dataSelecionada=LocalDate.now();
    private boolean carregandoCombo;
    private static final DateTimeFormatter HORA=DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale PORTUGUES=Locale.forLanguageTag("pt-BR");

    @FXML public void initialize() {
        popularComboBoxAnos();
        chkCancelados.selectedProperty().addListener((o,a,b)-> { atualizarCalendario(); atualizarPainelLateral(); });
        lblIrParaHojeTexto.setText("Ir para hoje — "+LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        var usuario=Sessao.getUsuarioLogado();
        lblNomeUsuario.setText(usuario==null?"Usuário":usuario.getLogin());
        lblCargoUsuario.setText(usuario==null?"":usuario.getCargoFuncao());
        atualizarAgenda();
    }
    private void popularComboBoxAnos() {
        carregandoCombo=true;
        List<Integer> anos=new ArrayList<>();
        int ano=dataAtualVisualizacao.getYear();
        for(int a=ano-10;a<=ano+10;a++) anos.add(a);
        cmbAno.setItems(FXCollections.observableArrayList(anos)); cmbAno.setValue(ano);
        carregandoCombo=false;
    }
    @FXML public void atualizarAgenda() {
        lblMensagem.setText("");
        try { eventos=dao.listar(dataAtualVisualizacao,dataAtualVisualizacao.withDayOfMonth(dataAtualVisualizacao.lengthOfMonth())); }
        catch(RuntimeException e) { eventos=List.of(); erro(e); }
        atualizarCalendario(); atualizarPainelLateral();
    }
    private List<EventoAgenda> eventosDoDia(LocalDate data) {
        return eventos.stream().filter(e->e.getData().equals(data))
                .filter(e->chkCancelados.isSelected() || e.getStatus()!=EventoAgenda.Status.CANCELADO).toList();
    }
    private void atualizarCalendario() {
        gridCalendario.getChildren().removeIf(n->GridPane.getRowIndex(n)!=null && GridPane.getRowIndex(n)>0);
        for(var cabecalho:gridCalendario.getChildren()) if(cabecalho instanceof Label label) label.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        String mes=dataAtualVisualizacao.getMonth().getDisplayName(TextStyle.FULL,PORTUGUES);
        lblMesAno.setText(mes.substring(0,1).toUpperCase()+mes.substring(1));
        carregandoCombo=true;
        if(!cmbAno.getItems().contains(dataAtualVisualizacao.getYear())) popularComboBoxAnos();
        cmbAno.setValue(dataAtualVisualizacao.getYear()); carregandoCombo=false;
        int deslocamento=dataAtualVisualizacao.getDayOfWeek().getValue()%7;
        int semanas=(deslocamento+dataAtualVisualizacao.lengthOfMonth()+6)/7;
        gridCalendario.getRowConstraints().clear();
        gridCalendario.getRowConstraints().add(new RowConstraints(25));
        for(int i=0;i<semanas;i++) {
            RowConstraints linha=new RowConstraints(); linha.setVgrow(Priority.ALWAYS); linha.setMinHeight(70);
            gridCalendario.getRowConstraints().add(linha);
        }
        for(int dia=1;dia<=dataAtualVisualizacao.lengthOfMonth();dia++) {
            LocalDate data=dataAtualVisualizacao.withDayOfMonth(dia);
            VBox celula=new VBox(3); celula.setMinWidth(0); celula.setUserData(data);
            celula.setStyle("-fx-cursor: hand;");
            celula.getStyleClass().add(data.equals(dataSelecionada)?"day-cell-today":"day-cell");
            Label numero=new Label(Integer.toString(dia));
            numero.getStyleClass().add(data.equals(LocalDate.now())?"day-number-today":"day-number");
            celula.getChildren().add(numero);
            List<EventoAgenda> doDia=eventosDoDia(data);
            for(EventoAgenda e:doDia.stream().limit(2).toList()) {
                Label tag=new Label(e.getHoraInicio().format(HORA)+" "+e.getTitulo());
                tag.setMinWidth(0); tag.setMaxWidth(Double.MAX_VALUE);
                tag.getStyleClass().add(estiloTag(e.getTipo()));
                tag.setTooltip(new Tooltip(e.getHoraInicio().format(HORA)+"–"+e.getHoraFim().format(HORA)+" · "+e.getTitulo()+" · "+e.getStatus()));
                if(e.getStatus()==EventoAgenda.Status.CANCELADO) tag.setOpacity(.5);
                celula.getChildren().add(tag);
            }
            if(doDia.size()>2) { Label mais=new Label("+ "+(doDia.size()-2)+" compromissos"); mais.getStyleClass().add("event-desc-sub"); celula.getChildren().add(mais); }
            celula.setOnMouseClicked(e-> { dataSelecionada=data; atualizarCalendario(); atualizarPainelLateral(); });
            int posicao=deslocamento+dia-1;
            gridCalendario.add(celula,posicao%7,posicao/7+1);
        }
    }
    private void atualizarPainelLateral() {
        lblDataSelecionada.setText(dataSelecionada.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy",PORTUGUES)));
        lblDiaSemanaSelecionado.setText(dataSelecionada.getDayOfWeek().getDisplayName(TextStyle.FULL,PORTUGUES));
        listaEventosPainel.getChildren().clear();
        for(EventoAgenda e:eventosDoDia(dataSelecionada)) {
            VBox card=new VBox(6); card.setUserData(e);
            card.getStyleClass().add(e.getTipo()==EventoAgenda.Tipo.VISITA?"event-card-green":"event-card-purple");
            Label horario=new Label(e.getHoraInicio().format(HORA)+"–"+e.getHoraFim().format(HORA)+" · "+e.getStatus());
            horario.getStyleClass().add("event-time-purple"); horario.setWrapText(true);
            Label titulo=new Label(e.getTitulo()); titulo.getStyleClass().add("event-title-bold"); titulo.setWrapText(true);
            card.getChildren().addAll(horario,titulo);
            adicionarDescricao(card,e.getTipo().toString());
            if(e.getNomeAdolescente()!=null) adicionarDescricao(card,"Adolescente: "+e.getNomeAdolescente());
            if(e.getNomeTecnico()!=null) adicionarDescricao(card,"Técnico: "+e.getNomeTecnico());
            if(!e.getLocal().isBlank()) adicionarDescricao(card,"Local: "+e.getLocal());
            if(!e.getObservacoes().isBlank()) adicionarDescricao(card,e.getObservacoes());
            Button editar=new Button("Editar"); editar.setOnAction(a->abrirFormulario(e));
            Button excluir=new Button("Excluir"); excluir.setOnAction(a->excluir(e));
            card.getChildren().add(new HBox(6,editar,excluir));
            if(e.getStatus()==EventoAgenda.Status.AGENDADO) {
                Button concluir=new Button("Concluir"), cancelar=new Button("Cancelar");
                concluir.setDisable(e.getData().atTime(e.getHoraFim()).isAfter(LocalDateTime.now()));
                concluir.setOnAction(a->alterarStatus(e,EventoAgenda.Status.CONCLUIDO));
                cancelar.setOnAction(a->alterarStatus(e,EventoAgenda.Status.CANCELADO));
                card.getChildren().add(new HBox(6,concluir,cancelar));
            }
            listaEventosPainel.getChildren().add(card);
        }
        if(listaEventosPainel.getChildren().isEmpty()) adicionarDescricao(listaEventosPainel,"Nenhum compromisso para este dia.");
    }
    private void adicionarDescricao(VBox caixa,String texto) {
        Label label=new Label(texto); label.setWrapText(true); label.getStyleClass().add("event-desc-sub"); caixa.getChildren().add(label);
    }
    @FXML public void mesAnterior(ActionEvent e) { exibirData(dataAtualVisualizacao.minusMonths(1)); }
    @FXML public void proximoMes(ActionEvent e) { exibirData(dataAtualVisualizacao.plusMonths(1)); }
    @FXML public void mudarAnoCombo(ActionEvent e) {
        if(!carregandoCombo && cmbAno.getValue()!=null) exibirData(dataAtualVisualizacao.withYear(cmbAno.getValue()));
    }
    @FXML public void irParaHoje(javafx.scene.input.MouseEvent e) { exibirData(LocalDate.now()); }
    public void exibirData(LocalDate data) { dataSelecionada=data; dataAtualVisualizacao=data.withDayOfMonth(1); atualizarAgenda(); }
    @FXML public void abrirModalNovoEvento(ActionEvent e) { abrirFormulario(null); }

    private void abrirFormulario(EventoAgenda existente) {
        try {
            Dialog<ButtonType> dialogo=new Dialog<>(); dialogo.setTitle(existente==null?"Novo compromisso":"Editar compromisso");
            if(gridCalendario.getScene()!=null && gridCalendario.getScene().getWindow()!=null) dialogo.initOwner(gridCalendario.getScene().getWindow());
            dialogo.getDialogPane().getStylesheets().add(getClass().getResource("/View/agenda-style.css").toExternalForm());
            dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
            ((Button)dialogo.getDialogPane().lookupButton(ButtonType.OK)).setText("Salvar");
            TextField titulo=new TextField(existente==null?"":existente.getTitulo()); titulo.setId("txtEventoTitulo");
            ComboBox<EventoAgenda.Tipo> tipo=new ComboBox<>(FXCollections.observableArrayList(EventoAgenda.Tipo.values()));
            tipo.setValue(existente==null?EventoAgenda.Tipo.ATENDIMENTO:existente.getTipo()); tipo.setId("cmbEventoTipo");
            DatePicker data=new DatePicker(existente==null?dataSelecionada:existente.getData()); data.setId("dpEventoData");
            TextField inicio=new TextField(existente==null?"09:00":existente.getHoraInicio().format(HORA)); inicio.setId("txtEventoInicio");
            TextField fim=new TextField(existente==null?"10:00":existente.getHoraFim().format(HORA)); fim.setId("txtEventoFim");
            TextField local=new TextField(existente==null?"":existente.getLocal()); local.setId("txtEventoLocal");
            TextArea observacoes=new TextArea(existente==null?"":existente.getObservacoes()); observacoes.setPrefRowCount(3); observacoes.setWrapText(true);
            ComboBox<EventoAgenda.Status> status=new ComboBox<>(FXCollections.observableArrayList(EventoAgenda.Status.values()));
            status.setValue(existente==null?EventoAgenda.Status.AGENDADO:existente.getStatus()); status.setId("cmbEventoStatus");
            ComboBox<Adolescente> adolescente=new ComboBox<>(FXCollections.observableArrayList(new AdolescenteDAO().listarCadastros())); adolescente.setId("cmbEventoAdolescente");
            adolescente.setConverter(new StringConverter<>() {
                public String toString(Adolescente a) { return a==null?"Sem adolescente":a.getNomeCompleto()+" · "+a.getCpfFormatado(); }
                public Adolescente fromString(String s) { return null; }
            });
            adolescente.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Adolescente a,boolean vazio) {
                    super.updateItem(a,vazio); setText(adolescente.getConverter().toString(a));
                }
            });
            ComboBox<EquipeTecnica> tecnico=new ComboBox<>(FXCollections.observableArrayList(new EquipeTecnicaDAO().listar())); tecnico.setId("cmbEventoTecnico");
            tecnico.setConverter(new StringConverter<>() {
                public String toString(EquipeTecnica t) { return t==null?"Sem técnico":t.getNomeCompleto(); }
                public EquipeTecnica fromString(String s) { return null; }
            });
            tecnico.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(EquipeTecnica t,boolean vazio) {
                    super.updateItem(t,vazio); setText(tecnico.getConverter().toString(t));
                }
            });
            if(existente!=null) {
                adolescente.setValue(adolescente.getItems().stream().filter(a->existente.getCpfAdolescente()!=null && a.getCpf()==existente.getCpfAdolescente()).findFirst().orElse(null));
                tecnico.setValue(tecnico.getItems().stream().filter(t->existente.getCpfEquipe()!=null && t.getCpf()==existente.getCpfEquipe()).findFirst().orElse(null));
            }
            Button limparAdolescente=new Button("Limpar"), limparTecnico=new Button("Limpar");
            limparAdolescente.setOnAction(e->adolescente.setValue(null)); limparTecnico.setOnAction(e->tecnico.setValue(null));
            GridPane grade=new GridPane(); grade.setHgap(12); grade.setVgap(10); grade.setPrefWidth(520);
            grade.addRow(0,new Label("Título *"),titulo); grade.addRow(1,new Label("Tipo *"),tipo);
            grade.addRow(2,new Label("Data *"),data); grade.addRow(3,new Label("Horários *"),new HBox(8,inicio,new Label("até"),fim));
            inicio.setPrefWidth(90); fim.setPrefWidth(90);
            grade.addRow(4,new Label("Local"),local); grade.addRow(5,new Label("Adolescente (opcional)"),new HBox(8,adolescente,limparAdolescente));
            grade.addRow(6,new Label("Técnico (opcional)"),new HBox(8,tecnico,limparTecnico));
            grade.addRow(7,new Label("Situação *"),status); grade.addRow(8,new Label("Observações"),observacoes);
            for(ComboBox<?> combo:List.of(tipo,status,adolescente,tecnico)) { combo.setMaxWidth(Double.MAX_VALUE); combo.setMinWidth(0); combo.setPrefWidth(260); }
            HBox.setHgrow(adolescente,Priority.ALWAYS); HBox.setHgrow(tecnico,Priority.ALWAYS);
            ColumnConstraints legenda=new ColumnConstraints(), campos=new ColumnConstraints(); campos.setHgrow(Priority.ALWAYS);
            grade.getColumnConstraints().addAll(legenda,campos);
            Label aviso=new Label("Horários no formato HH:mm. Um compromisso não lança frequência automaticamente."); aviso.setWrapText(true);
            Label erro=new Label(); erro.setId("lblEventoErro"); erro.setWrapText(true); erro.setStyle("-fx-text-fill: #c0392b;");
            ScrollPane scroll=new ScrollPane(grade); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(420);
            dialogo.getDialogPane().setContent(new VBox(10,scroll,aviso,erro)); dialogo.setResizable(true);
            EventoAgenda salvo=new EventoAgenda();
            dialogo.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION,e-> {
                try {
                    data.commitValue();
                    if(!inicio.getText().matches("[0-9]{2}:[0-9]{2}") || !fim.getText().matches("[0-9]{2}:[0-9]{2}"))
                        throw new IllegalArgumentException("Informe os horários no formato HH:mm, por exemplo 09:30.");
                    salvo.setIdEvento(existente==null?0:existente.getIdEvento()); salvo.setTitulo(titulo.getText()); salvo.setTipo(tipo.getValue());
                    salvo.setData(data.getValue()); salvo.setHoraInicio(LocalTime.parse(inicio.getText())); salvo.setHoraFim(LocalTime.parse(fim.getText()));
                    salvo.setLocal(local.getText()); salvo.setObservacoes(observacoes.getText()); salvo.setStatus(status.getValue());
                    salvo.setCpfAdolescente(adolescente.getValue()==null?null:adolescente.getValue().getCpf());
                    salvo.setCpfEquipe(tecnico.getValue()==null?null:tecnico.getValue().getCpf());
                    if(existente==null) dao.inserir(salvo); else dao.atualizar(salvo);
                } catch(java.time.format.DateTimeParseException ex) { erro.setText("Confira a data e os horários informados."); e.consume(); }
                catch(RuntimeException ex) { erro.setText(ex.getMessage()); e.consume(); }
            });
            if(dialogo.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK) { exibirData(salvo.getData()); lblMensagem.setText("Compromisso salvo."); }
        } catch(RuntimeException e) { erro(e); }
    }
    private void alterarStatus(EventoAgenda evento,EventoAgenda.Status status) {
        if(status==EventoAgenda.Status.CANCELADO && !confirmar("Cancelar este compromisso? Ele ficará no histórico.")) return;
        EventoAgenda.Status anterior=evento.getStatus(); evento.setStatus(status);
        try { dao.atualizar(evento); atualizarAgenda(); }
        catch(RuntimeException e) { evento.setStatus(anterior); erro(e); }
    }
    private void excluir(EventoAgenda e) {
        if(!confirmar("Excluir definitivamente este compromisso? Para preservar o histórico, use Cancelar.")) return;
        try { if(!dao.excluir(e.getIdEvento())) throw new IllegalArgumentException("O compromisso não existe mais."); atualizarAgenda(); }
        catch(RuntimeException ex) { erro(ex); }
    }
    private boolean confirmar(String mensagem) { return new Alert(Alert.AlertType.CONFIRMATION,mensagem,ButtonType.YES,ButtonType.NO).showAndWait().orElse(ButtonType.NO)==ButtonType.YES; }
    private void erro(RuntimeException e) { lblMensagem.setText(e.getMessage()); if(!(e instanceof IllegalArgumentException)) e.printStackTrace(); }
    private String estiloTag(EventoAgenda.Tipo tipo) {
        return switch(tipo) {
            case VISITA -> "event-tag-green"; case REUNIAO,RELATORIO -> "event-tag-purple";
            case PIA -> "event-tag-orange"; case AUDIENCIA -> "event-tag-red"; default -> "event-tag-blue";
        };
    }
    @FXML public void irParaPainel(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/Dashboard.fxml","Painel de Controle"); }
    @FXML public void irParaAdolescentes(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/AdolescentesView.fxml","Adolescentes"); }
    @FXML public void irParaAgenda(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/AgendaView.fxml","Agenda"); }
    @FXML public void irParaRelatorios(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/RelatoriosView.fxml","Relatórios"); }
    @FXML public void irParaEquipeTecnica(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/EquipeTecnicaView.fxml","Equipe Técnica"); }
    @FXML public void fazerLogout(ActionEvent e) { Sessao.limparSessao(); NavegacaoUtil.mudarTela(e,"/View/Login.fxml","Login"); }
}
