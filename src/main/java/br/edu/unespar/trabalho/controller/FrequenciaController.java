package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.dao.FrequenciaMensalDAO;
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

public class FrequenciaController {
    @FXML private TableView<FrequenciaMensalDTO> tabela;
    @FXML private DatePicker dpCompetencia;
    @FXML private TextField txtPesquisa;
    @FXML private Label lblResumo;
    @FXML private CheckBox chkInativos;
    private List<FrequenciaMensalDTO> linhas=List.of();
    private YearMonth mes=YearMonth.now();

    @FXML public void initialize() {
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
        var resultado=linhas.stream().filter(l->chkInativos.isSelected() || l.getAdolescente().getStatus()!=StatusAdolescente.INATIVO
                        || java.util.stream.IntStream.rangeClosed(1,mes.lengthOfMonth()).anyMatch(d->!l.getDia(d).isEmpty()))
                .filter(l->l.getAdolescente().getNomeCompleto().toLowerCase().contains(termo)
                        || l.getAdolescente().getCpfFormatado().contains(termo)
                        || (termo.matches("[0-9]+") && String.format("%011d",l.getAdolescente().getCpf()).contains(termo))).toList();
        tabela.setItems(FXCollections.observableArrayList(resultado));
        long pendentes=resultado.stream().mapToLong(FrequenciaMensalDTO::getSemVinculo).sum();
        lblResumo.setText(resultado.size()+" adolescentes · "+mes.format(DateTimeFormatter.ofPattern("MM/yyyy"))
                +(pendentes>0?" · "+pendentes+" lançamento(s) com horas sem PSC vinculada: revise pelo dia.":""));
    }

    private void montarColunas() {
        tabela.getColumns().clear();
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
                        String detalhes=linha==null?"":linha.getRegistrosDia(dia).stream().map(f->f+" · "+f.getObservacoes()).collect(java.util.stream.Collectors.joining("\n"));
                        setTooltip(new Tooltip(mes.atDay(dia)+"\n"+detalhes+"\nDuplo clique para registrar ou corrigir."));
                    }
                }
            });
        }
        coluna("MSE",95,FrequenciaMensalDTO::getMse); coluna("Meses LA",95,FrequenciaMensalDTO::getMeses);
        coluna("Horas PSC",90,l->l.getHorasPrevistas()==0?"—":Integer.toString(l.getHorasPrevistas()));
        coluna("Cumpridas",90,l->l.getHorasPrevistas()==0?"—":Integer.toString(l.getHorasCumpridas()));
        coluna("Pendentes",90,l->l.getHorasPrevistas()==0?"—":Integer.toString(l.getHorasPendentes()));
        coluna("IMM",70,l->l.getAdolescente().isImm()?"Sim":"Não");
        coluna("Vale-transporte",110,l->l.getAdolescente().isValeTransporte()?"Sim":"Não");
        coluna("PIA enviado",95,l->l.isPiaEnviado()?"Sim":"Não");
    }

    private TableColumn<FrequenciaMensalDTO,String> coluna(String titulo,int largura,Function<FrequenciaMensalDTO,String> valor) {
        TableColumn<FrequenciaMensalDTO,String> c=new TableColumn<>(titulo); c.setPrefWidth(largura);
        c.setReorderable(false); // a planilha simula um calendário: a ordem das colunas não pode mudar
        c.setCellValueFactory(d->new ReadOnlyStringWrapper(valor.apply(d.getValue()))); tabela.getColumns().add(c); return c;
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