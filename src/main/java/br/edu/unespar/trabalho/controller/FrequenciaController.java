package br.edu.unespar.trabalho.controller;

import static br.edu.unespar.trabalho.service.AutorizacaoService.*;
import static br.edu.unespar.trabalho.model.Operacao.*;
import br.edu.unespar.trabalho.dao.FrequenciaMensalDAO;
import br.edu.unespar.trabalho.dao.FrequenciaDAO;
import br.edu.unespar.trabalho.util.ApresentacaoAcesso;
import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Controla a consulta da planilha mensal de frequência e a abertura do editor de lançamentos.
 */
public class FrequenciaController {
    @FXML private TableView<FrequenciaMensalDTO> tabela;
    @FXML private DatePicker dpCompetencia;
    @FXML private TextField txtPesquisa;
    @FXML private Label lblResumo;
    @FXML private Button btnRegistrar, btnRelatorioOperacional;
    @FXML private CheckBox chkInativos;
    private List<FrequenciaMensalDTO> linhas=List.of();
    private YearMonth mes=YearMonth.now();

    @FXML public void initialize() {
        br.edu.unespar.trabalho.util.DatasFormulario.configurar(dpCompetencia);


        exigir(FREQUENCIA_CONSULTAR);
        ApresentacaoAcesso.aplicar(btnRelatorioOperacional, RELATORIO_OPERACIONAL_GERAR);
        btnRelatorioOperacional.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        if (!pode(FREQUENCIA_EDITAR)) btnRegistrar.setText("Registrar frequência");
        dpCompetencia.setValue(mes.atDay(1));
        tabela.setPlaceholder(new Label("Nenhum adolescente encontrado."));
        txtPesquisa.textProperty().addListener((o,a,b)->filtrar());
        chkInativos.selectedProperty().addListener((o,a,b)->filtrar());
        carregarMes();
    }

    @FXML public void atualizar() {
        try {
            dpCompetencia.commitValue();
            if(dpCompetencia.getValue()==null) throw new IllegalArgumentException("Informe o mês de referência.");
            mes=YearMonth.from(dpCompetencia.getValue());
            carregarMes();
        } catch(java.time.DateTimeException e) {
            new Alert(Alert.AlertType.WARNING,"Informe uma data existente no formato DD/MM/AAAA para consultar o mês.").showAndWait();
        } catch(RuntimeException e) { e.printStackTrace(); new Alert(Alert.AlertType.ERROR,"Não foi possível carregar a frequência. "+e.getMessage()).showAndWait(); }
    }

    private void carregarMes() {
        try {
            linhas = new FrequenciaMensalDAO().listar(mes);
            montarColunas();
            filtrar();
        } catch (RuntimeException e) {
            e.printStackTrace();
            linhas = List.of();
            tabela.getItems().clear();
            tabela.getColumns().clear();
            lblResumo.setText("Não foi possível carregar a frequência. " + e.getMessage());
        }
    }

    @FXML public void anterior() {
        mes = mes.minusMonths(1);
        dpCompetencia.setValue(mes.atDay(1));
        carregarMes();
    }

    @FXML public void proximo() {
        mes = mes.plusMonths(1);
        dpCompetencia.setValue(mes.atDay(1));
        carregarMes();
    }

    private void filtrar() {
        String termo=txtPesquisa.getText().trim().toLowerCase();
        boolean cpfCompleto=pode(ADOLESCENTE_CONSULTAR);
        txtPesquisa.setPromptText(cpfCompleto ? "Nome ou CPF" : "Nome ou final do CPF");
        var resultado=linhas.stream().filter(l->chkInativos.isSelected() || l.getAdolescente().getStatus()!=StatusAdolescente.INATIVO
                        || java.util.stream.IntStream.rangeClosed(1,mes.lengthOfMonth()).anyMatch(d->!l.getDia(d).isEmpty()))
                .filter(l->l.getAdolescente().getNomeCompleto().toLowerCase().contains(termo)
                        || (cpfCompleto && l.getAdolescente().getCpfFormatado().contains(termo))
                        || (termo.matches("[0-9]+") && (cpfCompleto
                        ? String.format("%011d",l.getAdolescente().getCpf()).contains(termo)
                        : String.format("%011d",l.getAdolescente().getCpf()).substring(7).endsWith(termo)))).toList();
        tabela.setItems(FXCollections.observableArrayList(resultado));
        long pendentes=resultado.stream().mapToLong(FrequenciaMensalDTO::getSemVinculo).sum();
        lblResumo.setText(resultado.size()+" adolescentes · "+mes.format(DateTimeFormatter.ofPattern("MM/yyyy"))
                +(pendentes>0?" · "+pendentes+" lançamento(s) com horas sem PSC vinculada: revise pelo dia.":""));
    }

    private void montarColunas() {
        tabela.getColumns().clear();
        boolean detalhada=pode(MEDIDA_CONSULTAR) && pode(HORAS_PSC_CONSULTAR);
        boolean podeCorrigir=pode(FREQUENCIA_EDITAR);
        coluna("Adolescente",230,l->l.getAdolescente().getNomeCompleto());
        coluna("Situação mensal",110,FrequenciaMensalDTO::getSituacao);
        for(int d=1;d<=mes.lengthOfMonth();d++) {
            final int dia=d;
            TableColumn<FrequenciaMensalDTO,String> c=coluna(Integer.toString(d),48,l->l.getDia(dia));
            c.setSortable(false);
            c.setCellFactory(col->new TableCell<>() {
                { setOnMouseClicked(e->{
                    if(e.getClickCount()==2 && !isEmpty() && getTableRow().getItem()!=null)
                        RegistroFrequenciaController.abrir(getTableRow().getItem().getAdolescente().getCpf(),mes.atDay(dia),()->carregarMes());
                }); }
                @Override protected void updateItem(String item,boolean vazio) {
                    super.updateItem(item,vazio); setText(vazio?null:item); setTooltip(null); setStyle("");
                    if(!vazio) {
                        setStyle("-fx-alignment:center; -fx-background-color:"+(item.contains("A")?"#f9dfdf":item.contains("J")?"#fff1cf":item.contains("P")?"#e2f1e8":"#fafaf7")+";");
                        var linha=getTableRow()==null?null:getTableRow().getItem();
                        String detalhes=linha==null?"":linha.getRegistrosDia(dia).stream().map(f -> (detalhada ? f.toString() : f.getNomeAtividade()+" · "+f.getStatusPresenca()) + (f.getObservacoes()==null || f.getObservacoes().isBlank() ? "" : " · "+f.getObservacoes())).collect(java.util.stream.Collectors.joining("\n"));
                        setTooltip(new Tooltip(mes.atDay(dia)+"\n"+detalhes+(podeCorrigir?"\nDuplo clique para registrar ou corrigir.":"\nDuplo clique para registrar.")));
                    }
                }
            });
        }
        if (!pode(MEDIDA_CONSULTAR)) return;
        coluna("MSE",95,FrequenciaMensalDTO::getMse); coluna("Meses LA",95,FrequenciaMensalDTO::getMeses);
        if (pode(HORAS_PSC_CONSULTAR)) {
            coluna("Horas PSC",90,l->l.getHorasPrevistas()==0?"—":Integer.toString(l.getHorasPrevistas()));
            coluna("Cumpridas",90,l->l.getHorasPrevistas()==0?"—":Integer.toString(l.getHorasCumpridas()));
            coluna("Pendentes",90,l->l.getHorasPrevistas()==0?"—":Integer.toString(l.getHorasPendentes()));
        }
        if(pode(ADOLESCENTE_CONSULTAR)) coluna("IMM",70,l->l.getAdolescente().isImm()?"Sim":"Não");
        if(pode(ADOLESCENTE_CONSULTAR)) coluna("Vale-transporte",110,l->l.getAdolescente().isValeTransporte()?"Sim":"Não");
        if(pode(PIA_CONSULTAR)) coluna("PIA enviado",95,l->l.isPiaEnviado()?"Sim":"Não");
    }

    private TableColumn<FrequenciaMensalDTO,String> coluna(String titulo,int largura,Function<FrequenciaMensalDTO,String> valor) {
        TableColumn<FrequenciaMensalDTO,String> c=new TableColumn<>(titulo); c.setPrefWidth(largura);
        c.setReorderable(false); // a planilha simula um calendário: a ordem das colunas não pode mudar
        c.setCellValueFactory(d->new ReadOnlyStringWrapper(valor.apply(d.getValue()))); tabela.getColumns().add(c); return c;
    }

    @FXML public void gerarRelatorioOperacional() {
        exigir(RELATORIO_OPERACIONAL_GERAR);
        var linha = tabela.getSelectionModel().getSelectedItem();
        if (linha==null) return;
        var adolescente = linha.getAdolescente();
        var registros = new FrequenciaDAO().listarParaRelatorioOperacional(adolescente.getCpf(),mes);
        StringBuilder relatorio = new StringBuilder("Frequência mensal — uso interno\n")
                .append(adolescente.getNomeCompleto()).append(" · ")
                .append(mes.format(DateTimeFormatter.ofPattern("MM/yyyy"))).append("\n\n");
        if (registros.isEmpty()) relatorio.append("Nenhum lançamento nesta competência.");
        for (Frequencia registro : registros) {
            relatorio.append(registro.getDataPresenca().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .append(" · ").append(registro).append("\n");
        }
        TextArea texto = new TextArea(relatorio.toString());
        texto.setId("txtRelatorioOperacional");
        texto.setEditable(false);
        texto.setWrapText(true);
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Relatório operacional de frequência");
        if (tabela.getScene()!=null) dialogo.initOwner(tabela.getScene().getWindow());
        dialogo.getDialogPane().setContent(texto);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogo.setResizable(true);
        dialogo.showAndWait();
    }

    @FXML public void registrar() {
        var l=tabela.getSelectionModel().getSelectedItem();
        if(l==null) { new Alert(Alert.AlertType.INFORMATION,"Selecione um adolescente na tabela.").showAndWait(); return; }
        LocalDate data=mes.equals(YearMonth.now())?LocalDate.now():mes.atDay(1);
        RegistroFrequenciaController.abrir(l.getAdolescente().getCpf(),data,()->carregarMes());
    }
    @FXML public void voltarParaLista(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/AdolescentesView.fxml","Adolescentes"); }
    @FXML public void irParaPainel(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/Dashboard.fxml","Painel"); }
    @FXML public void irParaAgenda(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/AgendaView.fxml","Agenda"); }
    @FXML public void irParaRelatorios(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/RelatoriosView.fxml","Relatórios"); }
    @FXML public void irParaEquipeTecnica(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/EquipeTecnicaView.fxml","Equipe técnica"); }
    @FXML public void fazerLogout(ActionEvent e) { br.edu.unespar.trabalho.util.Sessao.limparSessao(); NavegacaoUtil.mudarTela(e,"/View/Login.fxml","Login"); }
}