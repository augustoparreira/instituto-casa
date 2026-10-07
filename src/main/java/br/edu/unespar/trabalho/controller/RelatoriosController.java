package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.dao.*;
import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.model.RelatorioAcompanhamento.Campo;
import br.edu.unespar.trabalho.util.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import java.nio.file.Path;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RelatoriosController {
    @FXML private Label lblNomeUsuario, lblCargoUsuario, lblStatusEdicao, lblMensagem;
    @FXML private ComboBox<Adolescente> cmbAdolescente;
    @FXML private DatePicker dpCompetencia, dpEmissao;
    @FXML private TextField txtPesquisa;
    @FXML private ListView<RelatorioAcompanhamento> listRelatorios;
    @FXML private GridPane gridIdentificacao;
    @FXML private TextArea txtRegistroFrequencia, txtDescumprimento;
    private final EnumMap<Campo,TextField> campos=new EnumMap<>(Campo.class);
    private final RelatorioAcompanhamentoDAO dao=new RelatorioAcompanhamentoDAO();
    private List<RelatorioAcompanhamento> salvos=List.of();
    private RelatorioAcompanhamento documento;
    private boolean carregando, alterado;
    private static final DateTimeFormatter DATA=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String INSTITUICAO="CENTRO DE APOIO SOCIAL AO ADOLESCENTE";
    private static final String CNPJ="CNPJ: 04.313.535/0001-73";
    private static final String ENDERECO="Rua Antônio Ostrenski, 100 - Apucarana - Paraná";
    private static final String CONTATO="Fone: (43) 3033 2023";
    private static final String EMAIL="casaapucarana@gmail.com";
    private static final String TITULO="RELATÓRIO DE ACOMPANHAMENTO DE MEDIDA SOCIOEDUCATIVA";
    private static final String FREQUENCIA="1. REGISTRO DE FREQUÊNCIA";
    private static final String DESCUMPRIMENTO="2. DESCUMPRIMENTO DA MEDIDA E CONTATO COM A FAMÍLIA E ACORDO FIRMADO";

    @FXML public void initialize() {
        var usuario=Sessao.getUsuarioLogado();
        lblNomeUsuario.setText(usuario==null?"Usuário":usuario.getLogin());
        lblCargoUsuario.setText(usuario==null?"":usuario.getCargoFuncao());
        dpCompetencia.setValue(LocalDate.now().withDayOfMonth(1)); dpEmissao.setValue(LocalDate.now());
        cmbAdolescente.setConverter(new StringConverter<>() {
            public String toString(Adolescente a) { return a==null?"Selecione o adolescente":a.getNomeCompleto()+" · "+a.getCpfFormatado(); }
            public Adolescente fromString(String s) { return null; }
        });
        cmbAdolescente.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Adolescente a,boolean vazio) {
                super.updateItem(a,vazio); setText(cmbAdolescente.getConverter().toString(a));
            }
        });
        dpCompetencia.setTooltip(new Tooltip("Mês e ano de referência do relatório"));
        int linha=0;
        for(Campo campo:Campo.values()) {
            TextField texto=new TextField(); texto.setId("campo"+campo.name()); texto.getStyleClass().add("search-field");
            texto.textProperty().addListener((o,a,b)-> { if(!carregando) alterado=true; });
            Label rotulo=new Label(campo.getRotulo()); rotulo.setWrapText(true); rotulo.getStyleClass().add("form-label-mini");
            gridIdentificacao.addRow(linha++,rotulo,texto); campos.put(campo,texto);
        }
        txtRegistroFrequencia.textProperty().addListener((o,a,b)-> { if(!carregando) alterado=true; });
        txtDescumprimento.textProperty().addListener((o,a,b)-> { if(!carregando) alterado=true; });
        dpEmissao.valueProperty().addListener((o,a,b)-> { if(!carregando) alterado=true; });
        txtPesquisa.textProperty().addListener((o,a,b)->filtrar());
        listRelatorios.setCellFactory(l->new ListCell<>() {
            @Override protected void updateItem(RelatorioAcompanhamento r,boolean vazio) {
                super.updateItem(r,vazio); setText(null); setGraphic(null);
                if(!vazio && r!=null) {
                    Label nome=new Label(r.getCampo(Campo.NOME)); nome.setWrapText(true); nome.maxWidthProperty().bind(widthProperty().subtract(24));
                    Label referencia=new Label(r.getCompetencia().format(DateTimeFormatter.ofPattern("MM/yyyy"))+" · #"+r.getIdRelatorio());
                    VBox card=new VBox(4,nome,referencia); setGraphic(card);
                }
            }
        });
        listRelatorios.getSelectionModel().selectedItemProperty().addListener((o,anterior,novo)-> {
            if(novo==null || carregando) return;
            if(alterado && !confirmar("Descartar as alterações não salvas e abrir o relatório selecionado?")) {
                carregando=true; listRelatorios.getSelectionModel().select(anterior); carregando=false; return;
            }
            exibir(novo);
        });
        try { cmbAdolescente.setItems(FXCollections.observableArrayList(new AdolescenteDAO().listarCadastros())); atualizarLista(); }
        catch(RuntimeException e) { erro(e); }
    }
    private void atualizarLista() { salvos=dao.listar(); filtrar(); }
    private void filtrar() {
        String termo=txtPesquisa.getText().trim().toLowerCase(Locale.ROOT);
        carregando=true;
        listRelatorios.setItems(FXCollections.observableArrayList(salvos.stream().filter(r->r.toString().toLowerCase(Locale.ROOT).contains(termo)).toList()));
        carregando=false;
    }
    @FXML public void gerarNovoRelatorio() {
        try {
            dpCompetencia.commitValue();
            if(cmbAdolescente.getValue()==null || dpCompetencia.getValue()==null) throw new IllegalArgumentException("Selecione o adolescente e a competência para preparar o relatório.");
            if(alterado && !confirmar("Descartar as alterações não salvas e preparar um novo relatório com os dados atuais?")) return;
            Adolescente a=new AdolescenteDAO().buscarPorCpf(cmbAdolescente.getValue().getCpf());
            if(a==null) throw new IllegalArgumentException("Adolescente não encontrado.");
            YearMonth mes=YearMonth.from(dpCompetencia.getValue());
            RelatorioAcompanhamento r=new RelatorioAcompanhamento(); r.setCpfAdolescente(a.getCpf()); r.setCompetencia(mes);
            r.setCampo(Campo.NOME,a.getNomeCompleto()); r.setCampo(Campo.NASCIMENTO,a.getDataNascimento().format(DATA));
            r.setCampo(Campo.TELEFONE,a.getContato()); r.setCampo(Campo.EMAIL,a.getEmail());
            SituacaoSocial social=new SituacaoSocialDAO().buscarPorCpf(a.getCpf());
            if(social!=null) r.setCampo(Campo.ENDERECO,social.getEndereco()+(social.getBairro()==null||social.getBairro().isBlank()?"":" - "+social.getBairro()));
            List<Responsavel> responsaveis=new ResponsavelDAO().listarResponsaveis(a.getCpf());
            r.setCampo(Campo.RESPONSAVEL,responsaveis.stream().map(Responsavel::getNomeCompleto).collect(Collectors.joining("; ")));
            r.setCampo(Campo.FILIACAO,responsaveis.stream().filter(p->p.getParentesco()!=null && Set.of("mae","pai").contains(java.text.Normalizer.normalize(p.getParentesco().trim().toLowerCase(Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","")))
                    .map(Responsavel::getNomeCompleto).collect(Collectors.joining("; ")));
            r.setCampo(Campo.TELEFONE_RESPONSAVEL,responsaveis.stream().filter(p->p.getContato()!=null && !p.getContato().isBlank()).map(p->p.getNomeCompleto()+": "+p.getContato()).collect(Collectors.joining("; ")));
            List<MedidaSocioeducativa> medidas=new MedidaSocioeducativaDAO().listarPorAdolescente(a.getCpf());
            List<Frequencia> frequencias=new FrequenciaDAO().listar(a.getCpf(),mes.atDay(1),mes.atEndOfMonth());
            var resumo=new FrequenciaMensalDTO(a,medidas,frequencias,mes,a.isPiaEnviado());
            String horas=resumo.getMedidasDoMes().stream().filter(MedidaSocioeducativa::isPSC).map(m-> {
                int cumpridas=frequencias.stream().filter(f->Objects.equals(f.getIdMedida(),m.getIdMedida())).mapToInt(Frequencia::getHorasContabilizadas).sum();
                return "PSC #"+m.getIdMedida()+": "+m.getDuracaoHoras()+"h previstas; "+cumpridas+"h cumpridas no mês";
            }).collect(Collectors.joining("; "));
            r.setCampo(Campo.HORAS,horas.isBlank()?"Não se aplica - sem PSC no mês":horas);
            String registro=frequencias.isEmpty()?"Não há lançamentos de frequência registrados para esta competência.":
                    frequencias.stream().sorted(Comparator.comparing(Frequencia::getDataPresenca).thenComparing(Frequencia::getNomeAtividade))
                    .map(f->f.getDataPresenca().format(DATA)+" - "+f).collect(Collectors.joining("\n"));
            r.setRegistroFrequencia(registro);
            exibir(r); lblMensagem.setText("Revise os dados, informe o número do processo e complete a seção 2 com a avaliação da equipe.");
        } catch(RuntimeException e) { erro(e); }
    }
    private void exibir(RelatorioAcompanhamento r) {
        documento=r; carregando=true;
        if(r.getIdRelatorio()==0) listRelatorios.getSelectionModel().clearSelection();
        cmbAdolescente.setValue(cmbAdolescente.getItems().stream().filter(a->a.getCpf()==r.getCpfAdolescente()).findFirst().orElse(null));
        dpCompetencia.setValue(r.getCompetencia().atDay(1));
        for(Campo campo:Campo.values()) campos.get(campo).setText(r.getCampo(campo));
        txtRegistroFrequencia.setText(r.getRegistroFrequencia()); txtDescumprimento.setText(r.getDescumprimento()); dpEmissao.setValue(r.getDataEmissao());
        lblStatusEdicao.setText((r.getIdRelatorio()==0?"Novo relatório":"Relatório #"+r.getIdRelatorio())+" · "+r.getCampo(Campo.NOME)+" · "+r.getCompetencia());
        carregando=false; alterado=false;
    }
    private RelatorioAcompanhamento lerFormulario() {
        if(documento==null) throw new IllegalArgumentException("Prepare um relatório ou abra um rascunho salvo.");
        dpEmissao.commitValue();
        RelatorioAcompanhamento r=new RelatorioAcompanhamento(); r.setIdRelatorio(documento.getIdRelatorio());
        r.setCpfAdolescente(documento.getCpfAdolescente()); r.setCompetencia(documento.getCompetencia()); r.setDataEmissao(dpEmissao.getValue());
        for(Campo campo:Campo.values()) r.setCampo(campo,campos.get(campo).getText());
        r.setRegistroFrequencia(txtRegistroFrequencia.getText()); r.setDescumprimento(txtDescumprimento.getText()); r.validar(); return r;
    }
    @FXML public void salvarAtualizacaoRelatorio() {
        try { RelatorioAcompanhamento r=lerFormulario(); dao.salvar(r); exibir(r); atualizarLista(); lblMensagem.setText("Rascunho salvo no banco."); }
        catch(RuntimeException e) { erro(e); }
    }
    @FXML public void excluirRelatorio() {
        if(documento==null || documento.getIdRelatorio()==0) { lblMensagem.setText("Abra um relatório salvo para excluir."); return; }
        if(!confirmar("Excluir este relatório salvo?")) return;
        try {
            if(!dao.excluir(documento.getIdRelatorio())) throw new IllegalArgumentException("O relatório não existe mais.");
            documento=null; carregando=true; campos.values().forEach(TextField::clear); txtRegistroFrequencia.clear(); txtDescumprimento.clear();
            carregando=false; alterado=false; atualizarLista(); lblStatusEdicao.setText("Prepare um relatório"); lblMensagem.setText("Relatório excluído.");
        } catch(RuntimeException e) { erro(e); }
    }
    @FXML public void exportarTexto() {
        try {
            RelatorioAcompanhamento r=lerFormulario(); validarExportacao(r);
            FileChooser escolha=new FileChooser(); escolha.setTitle("Salvar relatório de acompanhamento");
            escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento de texto","*.txt"));
            escolha.setInitialFileName("acompanhamento-"+r.getCompetencia()+"-"+r.getCpfAdolescente()+".txt");
            var arquivo=escolha.showSaveDialog(gridIdentificacao.getScene().getWindow());
            if(arquivo!=null) { salvarTexto(r,arquivo.toPath()); lblMensagem.setText("Texto salvo. Confira o documento antes de encaminhar."); }
        } catch(Exception e) { lblMensagem.setText(e.getMessage()); }
    }
    public void exportarPara(Path destino) throws java.io.IOException {
        RelatorioAcompanhamento r=lerFormulario(); validarExportacao(r); salvarTexto(r,destino);
    }
    private void salvarTexto(RelatorioAcompanhamento r,Path destino) throws java.io.IOException {
        try(FileWriter arquivo=new FileWriter(destino.toFile(),StandardCharsets.UTF_8)) {
            for(String linha:new String[]{INSTITUICAO,CNPJ,ENDERECO,CONTATO,EMAIL,"",TITULO,
                    r.getCompetencia().format(DateTimeFormatter.ofPattern("MM/yyyy")),"","Identificação:"})
                arquivo.write(linha+System.lineSeparator());
            for(Campo campo:Campo.values()) arquivo.write(campo.getRotulo()+": "+r.getCampo(campo)+System.lineSeparator());
            arquivo.write(System.lineSeparator()+FREQUENCIA+System.lineSeparator()+r.getRegistroFrequencia()+System.lineSeparator());
            arquivo.write(System.lineSeparator()+DESCUMPRIMENTO+System.lineSeparator()+r.getDescumprimento()+System.lineSeparator());
            arquivo.write(System.lineSeparator()+"Apucarana, "+r.getDataEmissao().format(DATA)+System.lineSeparator());
        }
    }
    private void validarExportacao(RelatorioAcompanhamento r) {
        if(r.getCampo(Campo.PROCESSO).isBlank() || r.getRegistroFrequencia().isBlank() || r.getDescumprimento().isBlank())
            throw new IllegalArgumentException("Para exportar o relatório, informe o número do processo e revise as duas seções do relatório. Se não houve descumprimento, registre isso explicitamente.");
    }
    @FXML public void visualizarRelatorio() {
        try {
            RelatorioAcompanhamento r=lerFormulario(); VBox pagina=new VBox(12); pagina.setStyle("-fx-background-color: white; -fx-padding: 35; -fx-text-fill: black;");
            for(String texto:new String[]{INSTITUICAO,CNPJ,ENDERECO,CONTATO,EMAIL,TITULO,r.getCompetencia().toString(),"Identificação:"}) {
                Label l=new Label(texto); l.setWrapText(true); l.setMaxWidth(Double.MAX_VALUE); l.setStyle("-fx-text-fill: black; -fx-font-weight: bold;"); pagina.getChildren().add(l);
            }
            for(Campo campo:Campo.values()) { Label l=new Label(campo.getRotulo()+": "+r.getCampo(campo)); l.setWrapText(true); l.setStyle("-fx-text-fill: black;"); pagina.getChildren().add(l); }
            for(String[] secao:new String[][]{{FREQUENCIA,r.getRegistroFrequencia()},{DESCUMPRIMENTO,r.getDescumprimento()}}) {
                Label titulo=new Label(secao[0]), corpo=new Label(secao[1]); titulo.setWrapText(true); corpo.setWrapText(true);
                titulo.setStyle("-fx-text-fill: black; -fx-font-weight: bold;"); corpo.setStyle("-fx-text-fill: black; -fx-border-color: black; -fx-padding: 10;"); corpo.setMinHeight(90); corpo.setMaxWidth(Double.MAX_VALUE);
                pagina.getChildren().addAll(titulo,corpo);
            }
            Label data=new Label("Apucarana, "+r.getDataEmissao().format(DATA)); data.setStyle("-fx-text-fill: black;"); pagina.getChildren().add(data);
            ScrollPane scroll=new ScrollPane(pagina); scroll.setFitToWidth(true); scroll.setPrefViewportWidth(620); scroll.setPrefViewportHeight(600);
            Dialog<ButtonType> dialogo=new Dialog<>(); dialogo.setTitle("Prévia do conteúdo do relatório"); dialogo.getDialogPane().setContent(scroll);
            dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); dialogo.setResizable(true); dialogo.showAndWait();
        } catch(RuntimeException e) { erro(e); }
    }
    private boolean confirmar(String texto) { return new Alert(Alert.AlertType.CONFIRMATION,texto,ButtonType.YES,ButtonType.NO).showAndWait().orElse(ButtonType.NO)==ButtonType.YES; }
    private void erro(RuntimeException e) { lblMensagem.setText(e.getMessage()); if(!(e instanceof IllegalArgumentException)) e.printStackTrace(); }
    @FXML public void irParaPainel(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/Dashboard.fxml","Painel de Controle"); }
    @FXML public void irParaAdolescentes(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/AdolescentesView.fxml","Adolescentes"); }
    @FXML public void irParaAgenda(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/AgendaView.fxml","Agenda institucional"); }
    @FXML public void irParaRelatorios(ActionEvent e) { }
    @FXML public void irParaEquipeTecnica(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/EquipeTecnicaView.fxml","Equipe Técnica"); }
    @FXML public void fazerLogout(ActionEvent e) { Sessao.limparSessao(); NavegacaoUtil.mudarTela(e,"/View/Login.fxml","Login"); }
}
