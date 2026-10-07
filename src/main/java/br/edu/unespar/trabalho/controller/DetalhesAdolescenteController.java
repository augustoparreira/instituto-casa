package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.model.FrequenciaMensalDTO;
import br.edu.unespar.trabalho.dao.AtividadeDAO;
import br.edu.unespar.trabalho.dao.ComposicaoFamiliarDAO;
import br.edu.unespar.trabalho.dao.EquipeTecnicaDAO;
import br.edu.unespar.trabalho.dao.FrequenciaDAO;
import br.edu.unespar.trabalho.dao.MedidaSocioeducativaDAO;
import br.edu.unespar.trabalho.dao.PIADAO;
import br.edu.unespar.trabalho.dao.PerfilAdolescenteDAO;
import br.edu.unespar.trabalho.dao.ResponsavelDAO;
import br.edu.unespar.trabalho.dao.SaudeDAO;
import br.edu.unespar.trabalho.dao.SituacaoSocialDAO;
import br.edu.unespar.trabalho.model.AdolescenteDTO;
import br.edu.unespar.trabalho.model.Atividade;
import br.edu.unespar.trabalho.model.ComposicaoFamiliar;
import br.edu.unespar.trabalho.model.EquipeTecnica;
import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;
import br.edu.unespar.trabalho.model.Frequencia;
import br.edu.unespar.trabalho.model.FrequenciaLinhaDTO;
import br.edu.unespar.trabalho.model.MedidaSocioeducativa;
import br.edu.unespar.trabalho.model.PIA;
import br.edu.unespar.trabalho.model.Responsavel;
import br.edu.unespar.trabalho.model.Saude;
import br.edu.unespar.trabalho.model.SituacaoSocial;
import br.edu.unespar.trabalho.model.StatusPresenca;
import br.edu.unespar.trabalho.model.TipoMedida;
import br.edu.unespar.trabalho.util.Formatadores;
import br.edu.unespar.trabalho.util.IdUtil;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import br.edu.unespar.trabalho.util.Sessao;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.time.YearMonth;
import br.edu.unespar.trabalho.dao.EducacaoTrabalhoDAO;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class DetalhesAdolescenteController {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ----- Cabeçalho do perfil -----
    @FXML private Label lblNome;
    @FXML private Label lblCpf;
    @FXML private Label lblStatus;
    @FXML private Label lblTipoMedida;
    @FXML private Label lblResponsavel;
    @FXML private Label lblPorcentagem;
    @FXML private Label lblHoras;

    // ----- Aba: Dados Pessoais -----
    @FXML private Label lblInfoNome;
    @FXML private Label lblInfoCpf;
    @FXML private Label lblInfoNascimento;
    @FXML private Label lblInfoGenero;
    @FXML private Label lblInfoBairro;
    @FXML private Label lblInfoStatus;

    // ----- Abas -----
    @FXML private Button btnAbaDados;
    @FXML private Button btnAbaMedida;
    @FXML private Button btnAbaFrequencia;
    @FXML private Button btnAbaPia;
    @FXML private Button btnAbaFamilia;

    @FXML private VBox paneDados;
    @FXML private VBox paneMedida;
    @FXML private VBox paneFrequencia;
    @FXML private VBox panePia;
    @FXML private VBox paneFamilia;

    // ----- Aba: Medida Socioeducativa -----
    @FXML private VBox boxMedidaCadastrada;
    @FXML private VBox boxMedidaForm;
    @FXML private Label lblMedidaTipo;
    @FXML private Label lblMedidaInicio;
    @FXML private Label lblMedidaDuracao;
    @FXML private Label lblMedidaReincidencia;
    @FXML private Label lblMedidaHistorico;

    @FXML private ComboBox<TipoMedida> cmbTipoMedida;
    @FXML private DatePicker dpInicioMedida;
    @FXML private Label lblDuracaoForm;
    @FXML private TextField txtDuracao;
    @FXML private CheckBox chkReincidencia;
    @FXML private TextArea txtHistorico;

    @FXML private Label lblTecnicoAtual;
    @FXML private ComboBox<EquipeTecnica> cmbTecnico;

    // ----- Aba: Frequência e Horas -----
    @FXML private Label lblFreqPresencas;
    @FXML private Label lblFreqFaltas;
    @FXML private Label lblFreqHoras;
    @FXML private Label lblFreqMeses;
    @FXML private VBox cardFreqHoras;
    @FXML private VBox cardFreqMeses;
    @FXML private Label lblFreqPercentual;
    @FXML private ListView<Frequencia> listaFrequenciaPerfil;
    @FXML private Button btnCorrigirFrequencia;
    @FXML private ComboBox<MedidaSocioeducativa> cmbMedidaFrequenciaGeral;
    @FXML private VBox boxFrequenciaGeral;
    private List<MedidaSocioeducativa> medidasFrequenciaGeral = List.of();
    private List<Frequencia> registrosFrequenciaGeral = List.of();

    // ----- Aba: PIA -----
    @FXML private Label lblPiaStatus;
    @FXML private Label lblPiaData;
    @FXML private Label lblPiaTecnico;
    @FXML private Label lblPiaDiagnostico;
    @FXML private Label lblPiaVulnerabilidades;
    @FXML private Label lblPiaPotencialidades;
    @FXML private Label lblPiaEstrategias;
    @FXML private Label lblPiaEnviado;
    @FXML private Label lblPiaFormTitulo;
    @FXML private VBox boxPiaCadastrado;
    @FXML private VBox boxPiaForm;
    @FXML private Button btnMarcarEnviado;
    @FXML private Button btnCancelarPia;
    @FXML private DatePicker dpPiaData;
    @FXML private TextArea txtPiaDiagnostico;
    @FXML private TextArea txtPiaVulnerabilidades;
    @FXML private TextArea txtPiaPotencialidades;
    @FXML private TextArea txtPiaEstrategias;

    // ----- Aba: Família e Social -----
    @FXML private VBox boxResponsaveis;
    @FXML private VBox boxComposicao;
    @FXML private VBox boxSocialVazio;
    @FXML private GridPane gridSocial;
    @FXML private Button btnEditarSocial;
    @FXML private Label lblSocBairro;
    @FXML private Label lblSocRenda;
    @FXML private Label lblSocBeneficios;
    @FXML private Label lblSocCras;
    @FXML private Label lblSocNis;
    @FXML private Label lblSocUbs;
    @FXML private Label lblSocEndereco;
    @FXML private Label lblSocTelefone;
    private final AdolescenteDAO adolescenteDAO = new AdolescenteDAO();
    private final MedidaSocioeducativaDAO medidaDAO = new MedidaSocioeducativaDAO();
    private final PerfilAdolescenteDAO perfilDAO = new PerfilAdolescenteDAO();
    private final EquipeTecnicaDAO equipeDAO = new EquipeTecnicaDAO();
    private final AtividadeDAO atividadeDAO = new AtividadeDAO();
    private final FrequenciaDAO frequenciaDAO = new FrequenciaDAO();
    private final PIADAO piaDAO = new PIADAO();
    private final ResponsavelDAO responsavelDAO = new ResponsavelDAO();
    private final ComposicaoFamiliarDAO composicaoDAO = new ComposicaoFamiliarDAO();
    private final SituacaoSocialDAO situacaoSocialDAO = new SituacaoSocialDAO();
    private final SaudeDAO saudeDAO = new SaudeDAO();

    /** Limite de texto de cada campo do PIA (exige rodar o ajustes_ddl.sql que amplia as colunas). */
    private static final int LIMITE_TEXTO_PIA = 1000;

    @FXML private Label lblDadosComplementares;
    @FXML private ComboBox<MedidaSocioeducativa> cmbMedidaPerfil;
    @FXML private DatePicker dpFimMedida, dpMesFrequencia;
    private Integer idMedidaEdicao;
    private long cpfAtual;
    private AdolescenteDTO jovemAtual;
    private PIA piaAtual;
    private boolean piaEnviadoAtual;

    @FXML
    public void initialize() {
        dpMesFrequencia.setValue(LocalDate.now().withDayOfMonth(1));
        btnCorrigirFrequencia.disableProperty().bind(listaFrequenciaPerfil.getSelectionModel().selectedItemProperty().isNull());
        listaFrequenciaPerfil.setPlaceholder(textoVazio("Nenhum lançamento neste mês. Dias sem lançamento não são faltas."));
        listaFrequenciaPerfil.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Frequencia frequencia, boolean vazio) {
                super.updateItem(frequencia, vazio);
                setText(vazio || frequencia == null ? null : frequencia.getDataPresenca().format(FORMATO_DATA)
                        + " · " + frequencia + "\n" + Formatadores.textoOuTraco(frequencia.getObservacoes())
                        + (frequencia.getIdMedida() == null && frequencia.getHorasContabilizadas() > 0
                        ? " · Horas sem vínculo com PSC: revise." : ""));
            }
        });
        cmbMedidaPerfil.valueProperty().addListener((o,a,b)->exibirMedidaSelecionada());
        cmbMedidaFrequenciaGeral.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(MedidaSocioeducativa medida,boolean vazio) {
                super.updateItem(medida,vazio);
                setText(medida==null ? "Medidas vigentes (LA e PSC)" : medida.toString());
            }
        });
        cmbMedidaFrequenciaGeral.valueProperty().addListener((o,a,b)->exibirFrequenciaGeral());
        cmbTipoMedida.setItems(FXCollections.observableArrayList(TipoMedida.values()));

        // Aplicando máscaras nos DatePickers de Medidas e PIA
        aplicarMascaraData(dpInicioMedida);
        aplicarMascaraData(dpFimMedida);
        aplicarMascaraData(dpPiaData);

        cmbTecnico.setConverter(new StringConverter<>() {
            @Override
            public String toString(EquipeTecnica t) {
                if (t == null) return "";
                String cargo = t.getCargoFuncao() != null ? " (" + t.getCargoFuncao() + ")" : "";
                return t.getNomeCompleto() + cargo;
            }

            @Override
            public EquipeTecnica fromString(String texto) {
                return null;
            }
        });
    }

    private void aplicarMascaraData(DatePicker datePicker) {
        if (datePicker == null) return;
        TextField editor = datePicker.getEditor();
        editor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            String limpo = newValue.replaceAll("[^0-9]", "");
            if (limpo.length() > 8) limpo = limpo.substring(0, 8);

            StringBuilder formatado = new StringBuilder();
            for (int i = 0; i < limpo.length(); i++) {
                if (i == 2 || i == 4) {
                    formatado.append("/");
                }
                formatado.append(limpo.charAt(i));
            }

            if (!newValue.equals(formatado.toString())) {
                editor.setText(formatado.toString());
                editor.positionCaret(formatado.length());
            }
        });
    }

    public void carregarDados(AdolescenteDTO jovem) {
        if (jovem == null) return;

        jovemAtual = jovem;
        cpfAtual = Long.parseLong(jovem.getCpf().replaceAll("\\D", ""));

        lblNome.setText(jovem.getNome());
        lblCpf.setText(jovem.getCpf());
        lblStatus.setText(jovem.getStatus());
        lblTipoMedida.setText(jovem.getMedida());
        lblResponsavel.setText("Responsável: " + jovem.getTecnico());
        lblPorcentagem.setText((int) (jovem.getProgresso() * 100) + "%");
        lblHoras.setText(jovem.getTextoProgresso());

        lblInfoNome.setText(jovem.getNome());
        lblInfoCpf.setText(jovem.getCpf());
        lblInfoNascimento.setText(jovem.getDataNascimento());
        lblInfoGenero.setText(jovem.getGenero());
        lblInfoBairro.setText(jovem.getBairro());
        lblInfoStatus.setText(jovem.getStatus());

        try {
            var cadastro=adolescenteDAO.buscarPorCpf(cpfAtual);
            piaEnviadoAtual = cadastro.isPiaEnviado();
            var estudo=new EducacaoTrabalhoDAO().buscarPorCpf(cpfAtual);
            var saude=saudeDAO.buscarPorCpf(cpfAtual);
            lblDadosComplementares.setText("Idade: "+cadastro.getIdade()+" anos · aniversário: "+cadastro.getDataNascimento().getMonthValue()
                    +"\nNaturalidade: "+Formatadores.textoOuTraco(cadastro.getNaturalidade())+" · Cor/raça: "+Formatadores.textoOuTraco(cadastro.getCorRaca())
                    +"\nTelefone: "+Formatadores.textoOuTraco(cadastro.getContato())+" · E-mail: "+Formatadores.textoOuTraco(cadastro.getEmail())
                    +"\nEstuda: "+(estudo!=null && estudo.isEstuda()?"Sim · "+Formatadores.textoOuTraco(estudo.getEscola())+" · "+Formatadores.textoOuTraco(estudo.getSerie()):"Não")
                    +"\nTrabalha: "+(estudo!=null && estudo.isTrabalha()?"Sim · "+Formatadores.textoOuTraco(estudo.getLocalTrabalho())+" · "+Formatadores.textoOuTraco(estudo.getFuncao())+" · "+Formatadores.textoOuTraco(estudo.getVinculoEmpregaticio()):"Não")
                    +"\nUBS: "+(saude==null?"—":Formatadores.textoOuTraco(saude.getUbsReferencia()))
                    +" · Uso de SPA: "+(saude!=null && saude.isUsoSpa()?"Sim · "+Formatadores.textoOuTraco(saude.getSubstanciasUtilizadas()):"Não")
                    +"\nIMM: "+(cadastro.isImm()?"Sim":"Não")+" · Vale-transporte: "+(cadastro.isValeTransporte()?"Sim":"Não")
                    +"\nObservações: "+Formatadores.textoOuTraco(cadastro.getObservacoes()));
            carregarAbaMedida();
            carregarAbaFrequencia();
            carregarAbaPia();
            carregarAbaFamilia();
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    private void carregarAbaMedida() {
        var anterior=cmbMedidaPerfil.getValue();
        var medidas=medidaDAO.listarPorAdolescente(cpfAtual);
        cmbMedidaPerfil.setItems(FXCollections.observableArrayList(medidas));
        cmbMedidaPerfil.setValue(medidas.stream().filter(m->anterior!=null && m.getIdMedida()==anterior.getIdMedida()).findFirst().orElse(medidas.isEmpty()?null:medidas.getFirst()));
        alternar(boxMedidaForm,medidas.isEmpty());
        alternar(boxMedidaCadastrada,!medidas.isEmpty());
        exibirMedidaSelecionada();

        cmbTecnico.setItems(FXCollections.observableArrayList(equipeDAO.listar()));
        Long cpfReferencia = perfilDAO.buscarCpfTecnicoReferencia(cpfAtual);
        EquipeTecnica atual = null;
        if (cpfReferencia != null) {
            for (EquipeTecnica t : cmbTecnico.getItems()) {
                if (t.getCpf() == cpfReferencia) {
                    atual = t;
                    break;
                }
            }
        }
        lblTecnicoAtual.setText(atual != null ? atual.getNomeCompleto() : "Sem técnico vinculado");
        cmbTecnico.setValue(atual);
    }

    private void exibirMedidaSelecionada() {
        var m=cmbMedidaPerfil.getValue(); if(m==null) return;
        lblMedidaTipo.setText(m.getTipoMedida().getDescricao());
        lblMedidaInicio.setText(m.getDataInicio().format(FORMATO_DATA)+(m.getDataFim()==null?"":" · encerrada em "+m.getDataFim().format(FORMATO_DATA)));
        lblMedidaDuracao.setText(m.isPSC()?medidaDAO.consultarHorasDaMedida(m.getIdMedida(),LocalDate.now())+" de "+m.getDuracaoHoras()+" horas cumpridas":m.getDuracaoMeses()+" meses");
        lblMedidaReincidencia.setText(m.isReincidencia()?"Sim":"Não"); lblMedidaHistorico.setText(Formatadores.textoOuTraco(m.getHistoricoInfracional()));
    }

    @FXML public void novaMedida() {
        idMedidaEdicao=null; cmbTipoMedida.setValue(null); dpInicioMedida.setValue(null); dpFimMedida.setValue(null);
        txtDuracao.clear(); txtHistorico.clear(); chkReincidencia.setSelected(false); alternar(boxMedidaForm,true);
    }
    @FXML public void editarMedida() {
        var m=cmbMedidaPerfil.getValue(); if(m==null) return;
        idMedidaEdicao=m.getIdMedida(); cmbTipoMedida.setValue(m.getTipoMedida());
        dpInicioMedida.setValue(m.getDataInicio()); dpFimMedida.setValue(m.getDataFim());
        txtDuracao.setText(String.valueOf(m.isPSC()?m.getDuracaoHoras():m.getDuracaoMeses()));
        txtHistorico.setText(m.getHistoricoInfracional()); chkReincidencia.setSelected(m.isReincidencia()); alternar(boxMedidaForm,true);
    }
    @FXML public void cancelarMedida() { idMedidaEdicao=null; alternar(boxMedidaForm,cmbMedidaPerfil.getItems().isEmpty()); }

    @FXML public void editarCadastro() {
        try {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/View/CadastroAdolescenteView.fxml"));
            Parent root=loader.load(); CadastroAdolescenteController controller=loader.getController();
            controller.carregarParaEdicao(cpfAtual);
            NavegacaoUtil.trocarRaiz((Stage)lblNome.getScene().getWindow(),root,"Instituto C.A.S.A. - Editar cadastro");
        } catch(Exception e) { e.printStackTrace(); mostrarAlerta(Alert.AlertType.ERROR,"Erro ao abrir cadastro",e.getMessage()); }
    }
    @FXML public void abrirFrequenciaMensal(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/FrequenciaView.fxml","Frequência mensal"); }

    private void recarregarPerfil() {
        try {
            String cpfDigitos = String.format("%011d", cpfAtual);
            for (AdolescenteDTO d : adolescenteDAO.listarResumoDTO()) {
                if (d.getCpf().replaceAll("\\D", "").equals(cpfDigitos)) {
                    carregarDados(d);
                    return;
                }
            }
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    @FXML
    public void mostrarAba(ActionEvent event) {
        Object origem = event.getSource();
        if (origem == btnAbaDados) ativarAba(btnAbaDados, paneDados);
        else if (origem == btnAbaMedida) ativarAba(btnAbaMedida, paneMedida);
        else if (origem == btnAbaFrequencia) ativarAba(btnAbaFrequencia, paneFrequencia);
        else if (origem == btnAbaPia) ativarAba(btnAbaPia, panePia);
        else if (origem == btnAbaFamilia) ativarAba(btnAbaFamilia, paneFamilia);
    }

    private void ativarAba(Button botaoAtivo, VBox painelAtivo) {
        for (VBox painel : new VBox[]{paneDados, paneMedida, paneFrequencia, panePia, paneFamilia}) {
            alternar(painel, painel == painelAtivo);
        }
        for (Button botao : new Button[]{btnAbaDados, btnAbaMedida, btnAbaFrequencia, btnAbaPia, btnAbaFamilia}) {
            botao.getStyleClass().remove("tab-btn-active");
            if (!botao.getStyleClass().contains("tab-btn")) {
                botao.getStyleClass().add("tab-btn");
            }
        }
        botaoAtivo.getStyleClass().remove("tab-btn");
        botaoAtivo.getStyleClass().add("tab-btn-active");
    }

    private void alternar(Node caixa, boolean mostrar) {
        caixa.setVisible(mostrar);
        caixa.setManaged(mostrar);
    }

    @FXML
    public void tipoMedidaSelecionado() {
        TipoMedida tipo = cmbTipoMedida.getValue();
        if (tipo == TipoMedida.PSC) {
            lblDuracaoForm.setText("DURAÇÃO EM HORAS *");
            txtDuracao.setPromptText("Ex.: 120");
        } else if (tipo == TipoMedida.LA) {
            lblDuracaoForm.setText("DURAÇÃO EM MESES *");
            txtDuracao.setPromptText("Ex.: 12");
        }
    }

    @FXML
    public void salvarMedida() {
        try { dpInicioMedida.commitValue(); dpFimMedida.commitValue(); }
        catch(RuntimeException e) {
            mostrarAlerta(Alert.AlertType.WARNING,"Data inválida","A data informada está incompleta ou inválida. Verifique o dia, mês e ano.");
            return;
        }
        TipoMedida tipo = cmbTipoMedida.getValue();
        if (tipo == null || dpInicioMedida.getValue() == null || txtDuracao.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos obrigatórios",
                    "Informe o tipo da medida, a data de início e a duração.");
            return;
        }

        int duracao;
        try {
            duracao = Integer.parseInt(txtDuracao.getText().trim());
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Duração inválida", "A duração deve ser um número inteiro.");
            return;
        }

        try {
            MedidaSocioeducativa medida = new MedidaSocioeducativa();
            medida.setIdMedida(idMedidaEdicao==null?0:idMedidaEdicao);
            medida.setDataFim(dpFimMedida.getValue());
            medida.setCpfAdolescente(cpfAtual);
            medida.setTipoMedida(tipo);
            medida.setDataInicio(dpInicioMedida.getValue());
            medida.setReincidencia(chkReincidencia.isSelected());
            medida.setHistoricoInfracional(txtHistorico.getText());
            if (tipo == TipoMedida.PSC) {
                medida.setDuracaoHoras(duracao);
            } else {
                medida.setDuracaoMeses(duracao);
            }

            if (idMedidaEdicao==null ? medidaDAO.inserir(medida) : medidaDAO.atualizar(medida)) {
                idMedidaEdicao=null;
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Medida salva com sucesso!");
                recarregarPerfil();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro ao salvar",
                        "Não foi possível salvar a medida. Verifique os dados e tente novamente.");
            }
        } catch (IllegalArgumentException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Dados inválidos", e.getMessage());
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    @FXML
    public void definirTecnico() {
        EquipeTecnica tecnico = cmbTecnico.getValue();
        if (tecnico == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Técnico não selecionado", "Selecione um membro da equipe.");
            return;
        }

        try {
            if (perfilDAO.definirTecnicoReferencia(cpfAtual, tecnico.getCpf())) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Técnico de referência definido!");
                recarregarPerfil();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível definir o técnico de referência.");
            }
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    @FXML public void atualizarFrequenciaMes() {
        try { dpMesFrequencia.commitValue(); carregarAbaFrequencia(); }
        catch(RuntimeException e) { erroBanco(e); }
    }

    private void carregarAbaFrequencia() {
        if(dpMesFrequencia.getValue()==null) throw new IllegalArgumentException("Informe o mês da frequência.");
        YearMonth mes=YearMonth.from(dpMesFrequencia.getValue());
        List<Frequencia> registros=frequenciaDAO.listar(cpfAtual,mes.atDay(1),mes.atEndOfMonth());
        long presencas=registros.stream().filter(Frequencia::isPresente).count();
        long faltas=registros.stream().filter(f->!f.isPresente()).count();
        long injustificadas=registros.stream().filter(f->f.getStatusPresenca()==StatusPresenca.FALTA_INJUSTIFICADA).map(Frequencia::getDataPresenca).distinct().count();
        int horas=registros.stream().filter(f->f.getIdMedida()!=null).mapToInt(Frequencia::getHorasContabilizadas).sum();
        lblFreqPresencas.setText(Long.toString(presencas)); lblFreqFaltas.setText(Long.toString(faltas));
        lblFreqHoras.setText(horas+"h");
        lblFreqPercentual.setText(injustificadas>=FrequenciaMensalDTO.LIMITE_FALTAS_INJUSTIFICADAS?"Irregular":"Regular");
        // O indicador de medida depende do que o adolescente cumpre naquele mês: PSC conta horas, LA conta meses,
        // e quem tem as duas medidas vê os dois cards.
        medidasFrequenciaGeral=medidaDAO.listarPorAdolescente(cpfAtual);
        var resumoMes=new FrequenciaMensalDTO(null,medidasFrequenciaGeral,registros,mes,false);
        boolean temPsc=resumoMes.getMedidasDoMes().stream().anyMatch(MedidaSocioeducativa::isPSC);
        boolean temLa=resumoMes.getMedidasDoMes().stream().anyMatch(MedidaSocioeducativa::isLA);
        lblFreqMeses.setText(resumoMes.getMeses().isBlank()?"-":resumoMes.getMeses());
        alternar(cardFreqHoras,temPsc);
        alternar(cardFreqMeses,temLa);
        listaFrequenciaPerfil.setItems(FXCollections.observableArrayList(registros));
        LocalDate inicioGeral=medidasFrequenciaGeral.stream().map(MedidaSocioeducativa::getDataInicio).min(LocalDate::compareTo).orElse(LocalDate.now());
        registrosFrequenciaGeral=inicioGeral.isAfter(LocalDate.now()) ? List.of() : frequenciaDAO.listar(cpfAtual,inicioGeral,LocalDate.now());
        MedidaSocioeducativa anterior=cmbMedidaFrequenciaGeral.getValue();
        cmbMedidaFrequenciaGeral.setItems(FXCollections.observableArrayList(medidasFrequenciaGeral));
        cmbMedidaFrequenciaGeral.setValue(anterior==null ? null : medidasFrequenciaGeral.stream()
                .filter(m->m.getIdMedida()==anterior.getIdMedida()).findFirst().orElse(null));
        exibirFrequenciaGeral();
    }

    @FXML public void mostrarMedidasVigentesFrequencia() {
        cmbMedidaFrequenciaGeral.setValue(null);
        exibirFrequenciaGeral();
    }

    private void exibirFrequenciaGeral() {
        boxFrequenciaGeral.getChildren().clear();
        MedidaSocioeducativa selecionada=cmbMedidaFrequenciaGeral.getValue();
        List<MedidaSocioeducativa> medidas=selecionada==null ? medidasFrequenciaGeral.stream()
                .filter(m->m.vigenteEm(LocalDate.now())).toList() : List.of(selecionada);
        if(medidas.isEmpty()) {
            Label vazio=new Label("Nenhuma medida vigente. Selecione uma medida para consultar seu histórico.");
            vazio.getStyleClass().add("page-subtitle"); vazio.setWrapText(true);
            boxFrequenciaGeral.getChildren().add(vazio);
        }
        for(MedidaSocioeducativa medida:medidas) {
            List<Frequencia> vinculados=registrosFrequenciaGeral.stream()
                    .filter(f->f.getIdMedida()!=null && f.getIdMedida()==medida.getIdMedida()).toList();
            Label titulo=new Label(medida.getTipoMedida().getCodigo()+" · Início: "+medida.getDataInicio().format(FORMATO_DATA)
                    +(medida.getDataFim()==null ? "" : " · Encerramento: "+medida.getDataFim().format(FORMATO_DATA)));
            titulo.getStyleClass().add("form-value-normal"); titulo.setWrapText(true);
            HBox indicadores=new HBox(12);
            if(medida.isPSC()) {
                int cumpridas=vinculados.stream().mapToInt(Frequencia::getHorasContabilizadas).sum();
                indicadores.getChildren().addAll(indicadorFrequenciaGeral(medida.getDuracaoHoras()+"h","Horas exigidas","stat-number-dark"),
                        indicadorFrequenciaGeral(cumpridas+"h","Horas cumpridas","stat-number-green"),
                        indicadorFrequenciaGeral(Math.max(0,medida.getDuracaoHoras()-cumpridas)+"h","Horas pendentes","stat-number-orange"));
            } else {
                indicadores.getChildren().add(indicadorFrequenciaGeral(medida.getMesesCorridos()+" / "+medida.getDuracaoMeses(),
                        "Meses decorridos / previstos","stat-number-dark"));
            }
            indicadores.getChildren().addAll(indicadorFrequenciaGeral(Long.toString(vinculados.stream().filter(Frequencia::isPresente).count()),"Presenças registradas","stat-number-green"),
                    indicadorFrequenciaGeral(Long.toString(vinculados.stream().filter(f->!f.isPresente()).count()),"Faltas registradas","stat-number-red"));
            boxFrequenciaGeral.getChildren().add(new VBox(8,titulo,indicadores));
        }
    }

    private VBox indicadorFrequenciaGeral(String valor,String descricao,String estilo) {
        Label numero=new Label(valor); numero.getStyleClass().add(estilo);
        Label legenda=new Label(descricao); legenda.getStyleClass().add("stat-label"); legenda.setWrapText(true);
        VBox card=new VBox(4,numero,legenda); card.getStyleClass().add("stat-box"); card.setAlignment(Pos.CENTER);
        HBox.setHgrow(card,Priority.ALWAYS);
        return card;
    }

    @FXML public void corrigirFrequenciaSelecionada() {
        Frequencia frequencia = listaFrequenciaPerfil.getSelectionModel().getSelectedItem();
        if (frequencia != null) RegistroFrequenciaController.abrir(frequencia, this::recarregarPerfil);
    }

    @FXML public void registrarPresenca() {
        RegistroFrequenciaController.abrir(cpfAtual,LocalDate.now(),this::recarregarPerfil);
    }

    private void carregarAbaPia() {
        piaAtual = piaDAO.buscarPorCpf(cpfAtual);
        alternar(lblPiaStatus, true);
        lblPiaStatus.setText(piaEnviadoAtual ? "Documento enviado" : "Documento pendente");
        lblPiaStatus.getStyleClass().removeAll("badge-enviado", "badge-pendente");
        lblPiaStatus.getStyleClass().add(piaEnviadoAtual ? "badge-enviado" : "badge-pendente");

        if (piaAtual == null) {
            alternar(boxPiaCadastrado, false);
            alternar(boxPiaForm, true);
            prepararFormularioPia(null);
            return;
        }

        boolean enviado = piaAtual.isDocumentoEnviado();
        alternar(boxPiaCadastrado, true);
        alternar(boxPiaForm, false);
        alternar(lblPiaStatus, true);

        lblPiaStatus.setText(enviado ? "Documento enviado" : "Documento pendente");
        lblPiaStatus.getStyleClass().removeAll("badge-enviado", "badge-pendente");
        lblPiaStatus.getStyleClass().add(enviado ? "badge-enviado" : "badge-pendente");

        String autor = perfilDAO.buscarNomeAutorPia(piaAtual.getIdPia());
        lblPiaData.setText(Formatadores.dataExtenso(piaAtual.getDataElaboracao()));
        lblPiaTecnico.setText(Formatadores.textoOuTraco(autor));
        lblPiaDiagnostico.setText(Formatadores.textoOuTraco(piaAtual.getDiagnostico()));
        lblPiaVulnerabilidades.setText(Formatadores.textoOuTraco(piaAtual.getVulnerabilidades()));
        lblPiaPotencialidades.setText(Formatadores.textoOuTraco(piaAtual.getPotencialidades()));
        lblPiaEstrategias.setText(Formatadores.textoOuTraco(piaAtual.getEstrategias()));
        lblPiaEnviado.setText(enviado ? "Sim" : "Não");
        btnMarcarEnviado.setDisable(enviado);
    }

    private void prepararFormularioPia(PIA modelo) {
        boolean edicao = modelo != null;
        lblPiaFormTitulo.setText(edicao ? "Editar PIA" : "Elaborar PIA");
        dpPiaData.setValue(edicao ? modelo.getDataElaboracao() : LocalDate.now());
        dpPiaData.setDisable(edicao);
        txtPiaDiagnostico.setText(edicao ? textoOuVazio(modelo.getDiagnostico()) : "");
        txtPiaVulnerabilidades.setText(edicao ? textoOuVazio(modelo.getVulnerabilidades()) : "");
        txtPiaPotencialidades.setText(edicao ? textoOuVazio(modelo.getPotencialidades()) : "");
        txtPiaEstrategias.setText(edicao ? textoOuVazio(modelo.getEstrategias()) : "");
        alternar(btnCancelarPia, edicao);
    }

    @FXML
    public void editarPia() {
        if (piaAtual == null) return;
        prepararFormularioPia(piaAtual);
        alternar(boxPiaCadastrado, false);
        alternar(boxPiaForm, true);
    }

    @FXML
    public void cancelarEdicaoPia() {
        try {
            carregarAbaPia();
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    @FXML
    public void salvarPia() {
        String diagnostico = txtPiaDiagnostico.getText().trim();
        if (dpPiaData.getValue() == null || diagnostico.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos obrigatórios",
                    "Informe a data de elaboração e o diagnóstico inicial.");
            return;
        }
        if (dpPiaData.getValue().isAfter(LocalDate.now())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Data inválida", "A data de elaboração não pode ser futura.");
            return;
        }
        for (TextArea campo : new TextArea[]{txtPiaDiagnostico, txtPiaVulnerabilidades,
                txtPiaPotencialidades, txtPiaEstrategias}) {
            if (campo.getText().length() > LIMITE_TEXTO_PIA) {
                mostrarAlerta(Alert.AlertType.WARNING, "Texto muito longo",
                        "Cada campo do PIA pode ter no máximo " + LIMITE_TEXTO_PIA + " caracteres.");
                return;
            }
        }

        try {
            boolean editando = piaAtual != null;
            PIA pia = editando ? piaAtual : new PIA();
            pia.setDiagnostico(diagnostico);
            pia.setVulnerabilidades(nuloSeVazio(txtPiaVulnerabilidades.getText()));
            pia.setPotencialidades(nuloSeVazio(txtPiaPotencialidades.getText()));
            pia.setEstrategias(nuloSeVazio(txtPiaEstrategias.getText()));

            boolean gravou;
            if (editando) {
                gravou = piaDAO.atualizar(pia);
            } else {
                EquipeTecnicaDTO usuario = Sessao.getUsuarioLogado();
                if (usuario == null) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Sessão expirada",
                            "Faça login novamente para elaborar o PIA.");
                    return;
                }
                pia.setIdPia(IdUtil.proximoId("PIA"));
                pia.setDataElaboracao(dpPiaData.getValue());
                pia.setDocumentoEnviado(false);
                pia.setCpfAdolescente(cpfAtual);
                gravou = perfilDAO.criarPia(pia, usuario.getCpfEquipe());
            }

            if (gravou) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "PIA salvo com sucesso!");
                carregarAbaPia();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro ao salvar",
                        "Não foi possível salvar o PIA.");
            }
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    @FXML
    public void marcarPiaEnviado() {
        if (piaAtual == null || piaAtual.isDocumentoEnviado()) return;

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Marcar como enviado");
        confirmacao.setHeaderText(null);
        confirmacao.setContentText("Confirmar que o PIA foi enviado ao CREAS?");
        Optional<ButtonType> resposta = confirmacao.showAndWait();
        if (resposta.isEmpty() || resposta.get() != ButtonType.OK) return;

        try {
            piaAtual.setDocumentoEnviado(true);
            if (piaDAO.atualizar(piaAtual)) {
                carregarAbaPia();
            } else {
                piaAtual.setDocumentoEnviado(false);
                mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível atualizar o PIA.");
            }
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    @FXML
    public void exportarTextoPia() {
        if (piaAtual == null) return;

        FileChooser seletor = new FileChooser();
        seletor.setTitle("Salvar relatório do PIA");
        seletor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento de texto", "*.txt"));
        seletor.setInitialFileName("PIA_" + nomeParaArquivo(jovemAtual.getNome()) + "_" + LocalDate.now() + ".txt");
        File destino = seletor.showSaveDialog(lblNome.getScene().getWindow());
        if (destino == null) return;

        try (FileWriter arquivo = new FileWriter(destino, StandardCharsets.UTF_8)) {
            EquipeTecnicaDTO usuario = Sessao.getUsuarioLogado();
            String emitidoPor = usuario != null && usuario.getLogin() != null ? " por " + usuario.getLogin() : "";
            arquivo.write("Plano Individual de Atendimento (PIA)" + System.lineSeparator());
            arquivo.write("Instituto C.A.S.A. - emitido em " + Formatadores.dataCurta(LocalDate.now()) + emitidoPor + System.lineSeparator());
            for (String[] campo : new String[][] {
                    {"Adolescente", jovemAtual.getNome() + "  |  CPF " + jovemAtual.getCpf()},
                    {"Medida socioeducativa", jovemAtual.getMedida() + "  |  Progresso: " + jovemAtual.getTextoProgresso()},
                    {"Técnico de referência", jovemAtual.getTecnico()},
                    {"Data de elaboração", Formatadores.dataExtenso(piaAtual.getDataElaboracao())},
                    {"Técnico(a) responsável pelo PIA", perfilDAO.buscarNomeAutorPia(piaAtual.getIdPia())},
                    {"Diagnóstico inicial", piaAtual.getDiagnostico()},
                    {"Vulnerabilidades identificadas", piaAtual.getVulnerabilidades()},
                    {"Potencialidades", piaAtual.getPotencialidades()},
                    {"Estratégias de intervenção", piaAtual.getEstrategias()},
                    {"Documento enviado ao CREAS", piaAtual.isDocumentoEnviado() ? "Sim" : "Não"}
            }) {
                arquivo.write(System.lineSeparator() + campo[0] + ":" + System.lineSeparator()
                        + Formatadores.textoOuTraco(campo[1]) + System.lineSeparator());
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Erro ao exportar", "Não foi possível salvar o arquivo de texto.");
            return;
        }
        mostrarAlerta(Alert.AlertType.INFORMATION, "Relatório gerado", "Texto salvo em:\n" + destino.getAbsolutePath());
    }

    private void carregarAbaFamilia() {
        boxResponsaveis.getChildren().clear();
        List<Responsavel> responsaveis = responsavelDAO.listarResponsaveis(cpfAtual);
        if (responsaveis.isEmpty()) {
            boxResponsaveis.getChildren().add(textoVazio("Nenhum responsável cadastrado."));
        }
        for (Responsavel r : responsaveis) {
            String contato = r.getContato() != null && !r.getContato().isBlank() ? " · " + r.getContato() : "";
            HBox cartao=cartaoPessoa(r.getNomeCompleto(),Formatadores.textoOuTraco(r.getParentesco())+contato,r.isContatoPrincipal()?"Contato principal":null);
            Button editar=new Button("Editar"); editar.setOnAction(e->formularioResponsavel(r));
            Button remover=new Button("Desvincular"); remover.setOnAction(e->{
                if(confirmarRemocao("Remover o vínculo deste responsável? O cadastro da pessoa será preservado.")) {
                    try { responsavelDAO.desvincular(cpfAtual,r.getCpf()); carregarAbaFamilia(); } catch(RuntimeException ex) { erroBanco(ex); }
                }
            });
            cartao.getChildren().addAll(editar,remover); boxResponsaveis.getChildren().add(cartao);
        }

        boxComposicao.getChildren().clear();
        List<ComposicaoFamiliar> familiares = composicaoDAO.listarPorAdolescente(cpfAtual);
        if (familiares.isEmpty()) {
            boxComposicao.getChildren().add(textoVazio("Nenhum familiar cadastrado."));
        }
        for (ComposicaoFamiliar f : familiares) {
            StringBuilder sub = new StringBuilder(Formatadores.textoOuTraco(f.getParentesco()));
            if (f.getIdade() != null) sub.append(" · ").append(f.getIdade()).append(" anos");
            if (f.getProfissao() != null && !f.getProfissao().isBlank()) sub.append(" · ").append(f.getProfissao());
            if (f.getRenda() != null) sub.append(" · ").append(Formatadores.moeda(f.getRenda()));
            HBox cartao=cartaoPessoa(f.getNome(),sub.toString(),null);
            Button editar=new Button("Editar"); editar.setOnAction(e->formularioFamiliar(f));
            Button remover=new Button("Excluir"); remover.setOnAction(e->{
                if(confirmarRemocao("Excluir este integrante da composição familiar?")) {
                    try { composicaoDAO.excluir(f.getIdComposicaoFamiliar(),cpfAtual); carregarAbaFamilia(); } catch(RuntimeException ex) { erroBanco(ex); }
                }
            });
            cartao.getChildren().addAll(editar,remover); boxComposicao.getChildren().add(cartao);
        }

        SituacaoSocial social = situacaoSocialDAO.buscarPorCpf(cpfAtual);
        Saude saude = saudeDAO.buscarPorCpf(cpfAtual);
        boolean temDados = social != null || saude != null;

        alternar(boxSocialVazio, !temDados);
        alternar(gridSocial, temDados);
        btnEditarSocial.setText(temDados ? "Editar" : "Cadastrar");

        if (temDados) {
            lblSocBairro.setText(social != null ? Formatadores.textoOuTraco(social.getBairro()) : "-");
            lblSocRenda.setText(social != null ? String.format(new java.util.Locale("pt","BR"),"%.2f salários mínimos",social.getRendaFamiliar()) : "-");
            lblSocBeneficios.setText(social != null ? Formatadores.textoOuTraco(social.getBeneficioSocial()) : "-");
            lblSocCras.setText(social == null ? "—" : Formatadores.textoOuTraco(social.getCrasNome()));
            lblSocNis.setText(social != null ? String.valueOf(social.getNumeroNis()) : "-");
            lblSocUbs.setText(saude != null ? Formatadores.textoOuTraco(saude.getUbsReferencia()) : "-");
            lblSocEndereco.setText(social != null ? Formatadores.textoOuTraco(social.getEndereco()) : "-");
            lblSocTelefone.setText(social != null ? Formatadores.textoOuTraco(social.getTelefone()) : "-");
        }
    }

    private boolean confirmarRemocao(String mensagem) {
        Alert a=new Alert(Alert.AlertType.CONFIRMATION,mensagem,ButtonType.YES,ButtonType.NO);
        return a.showAndWait().orElse(ButtonType.NO)==ButtonType.YES;
    }

    private HBox cartaoPessoa(String nome, String detalhe, String selo) {
        Label lblNomePessoa = new Label(nome);
        lblNomePessoa.getStyleClass().add("pessoa-nome");
        Label lblDetalhe = new Label(detalhe);
        lblDetalhe.getStyleClass().add("pessoa-sub");

        HBox cartao = new HBox(10, new VBox(2, lblNomePessoa, lblDetalhe));
        cartao.getStyleClass().add("pessoa-card");
        cartao.setAlignment(Pos.CENTER_LEFT);

        if (selo != null) {
            Region espaco = new Region();
            HBox.setHgrow(espaco, Priority.ALWAYS);
            Label lblSelo = new Label(selo);
            lblSelo.getStyleClass().add("badge-principal");
            cartao.getChildren().addAll(espaco, lblSelo);
        }
        return cartao;
    }

    private Label textoVazio(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("empty-text");
        return l;
    }

    @FXML public void adicionarResponsavel() { formularioResponsavel(null); }
    private void formularioResponsavel(Responsavel existente) {
        Dialog<ButtonType> dialogo = criarDialogo(existente==null?"Adicionar responsável":"Editar responsável");

        TextField txtNome = new TextField();
        TextField txtCpf = new TextField();
        txtCpf.setPromptText("Somente números");
        DatePicker dpNascimento = new DatePicker();
        dpNascimento.setMaxWidth(Double.MAX_VALUE);
        aplicarMascaraData(dpNascimento);

        TextField txtContato = new TextField();
        txtContato.setPromptText("(41) 99999-9999");
        TextField txtEmail = new TextField();
        TextField txtParentesco = new TextField();
        txtParentesco.setPromptText("Ex.: Mãe, Pai, Avó (até 80 caracteres)");
        CheckBox chkPrincipal = new CheckBox("É o contato principal");
        Label erro = rotuloErro();

        GridPane grade = formulario();
        linhaFormulario(grade, 0, "NOME COMPLETO *", txtNome);
        linhaFormulario(grade, 1, "CPF *", txtCpf);
        linhaFormulario(grade, 2, "NASCIMENTO *", dpNascimento);
        linhaFormulario(grade, 3, "PARENTESCO *", txtParentesco);
        linhaFormulario(grade, 4, "CONTATO", txtContato);
        linhaFormulario(grade, 5, "E-MAIL", txtEmail);
        linhaFormulario(grade, 6, "", chkPrincipal);
        Label aviso=new Label("Parentesco e contato principal são definidos para este adolescente. Dados pessoais de um responsável existente são compartilhados.");
        aviso.setWrapText(true); linhaFormulario(grade,7,"",aviso);
        if(existente!=null) {
            txtNome.setText(existente.getNomeCompleto()); txtCpf.setText(String.format("%011d",existente.getCpf())); txtCpf.setDisable(true);
            dpNascimento.setValue(existente.getDataNascimento()); txtContato.setText(existente.getContato()); txtEmail.setText(existente.getEmail());
            txtParentesco.setText(existente.getParentesco()); chkPrincipal.setSelected(existente.isContatoPrincipal());
        } else txtCpf.focusedProperty().addListener((o,a,focused)-> {
            String numero=Formatadores.soDigitos(txtCpf.getText());
            if(!focused && numero.length()==11) {
                try {
                    Responsavel r=responsavelDAO.buscarPessoa(Long.parseLong(numero));
                    boolean reutilizado=r!=null;
                    txtNome.setDisable(reutilizado); dpNascimento.setDisable(reutilizado); txtContato.setDisable(reutilizado); txtEmail.setDisable(reutilizado);
                    if(r!=null) {
                        txtNome.setText(r.getNomeCompleto()); dpNascimento.setValue(r.getDataNascimento()); txtContato.setText(r.getContato()); txtEmail.setText(r.getEmail());
                        aviso.setText("CPF já cadastrado: os dados pessoais serão reutilizados. Para alterá-los, salve o vínculo e use Editar.");
                    }
                } catch(RuntimeException e) { erro.setText(e.getMessage()); }
            }
        });

        boolean salvar = exibirFormulario(dialogo, grade, erro, () -> {
            String nome = txtNome.getText().trim();
            if (nome.isEmpty() || nome.length() > 80) return "Informe o nome (até 80 caracteres).";
            String cpf = Formatadores.soDigitos(txtCpf.getText());
            if (cpf.length() != 11) return "O CPF deve ter 11 dígitos.";
            if (Long.parseLong(cpf) == cpfAtual) return "O responsável não pode ter o mesmo CPF do adolescente.";
            if (dpNascimento.getValue() == null || !dpNascimento.getValue().isBefore(LocalDate.now()))
                return "Informe uma data de nascimento válida.";
            String parentesco = txtParentesco.getText().trim();
            if (parentesco.isEmpty() || parentesco.length() > 80) return "Parentesco: de 1 a 80 caracteres.";
            if (txtContato.getText().trim().length() > 30) return "Contato: no máximo 30 caracteres.";
            if (txtEmail.getText().trim().length() > 90) return "E-mail: no máximo 90 caracteres.";
            return null;
        });
        if (!salvar) return;

        try {
            Responsavel r = new Responsavel();
            r.setCpf(Long.parseLong(Formatadores.soDigitos(txtCpf.getText())));
            r.setNomeCompleto(txtNome.getText().trim());
            r.setDataNascimento(dpNascimento.getValue());
            r.setContato(nuloSeVazio(txtContato.getText()));
            r.setEmail(nuloSeVazio(txtEmail.getText()));
            r.setParentesco(txtParentesco.getText().trim());
            r.setContatoPrincipal(chkPrincipal.isSelected());

            if (existente==null ? responsavelDAO.inserir(r, cpfAtual) : responsavelDAO.atualizar(r,cpfAtual)) {
                carregarAbaFamilia();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro ao salvar",
                        "Não foi possível salvar o responsável.\nVerifique se esse CPF já não está cadastrado.");
            }
        } catch (IllegalArgumentException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Dados inválidos", e.getMessage());
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    @FXML public void adicionarFamiliar() { formularioFamiliar(null); }
    private void formularioFamiliar(ComposicaoFamiliar existente) {
        Dialog<ButtonType> dialogo = criarDialogo(existente==null?"Adicionar familiar":"Editar familiar");

        TextField txtNome = new TextField();
        TextField txtParentesco = new TextField();
        TextField txtIdade = new TextField();
        TextField txtRenda = new TextField();
        txtRenda.setPromptText("Ex.: 1500,00");
        TextField txtEscolaridade = new TextField();
        txtEscolaridade.setPromptText("Ex.: Ensino médio");
        TextField txtProfissao = new TextField();
        Label erro = rotuloErro();

        GridPane grade = formulario();
        linhaFormulario(grade, 0, "NOME *", txtNome);
        linhaFormulario(grade, 1, "PARENTESCO *", txtParentesco);
        linhaFormulario(grade, 2, "IDADE", txtIdade);
        linhaFormulario(grade, 3, "RENDA (R$)", txtRenda);
        linhaFormulario(grade, 4, "ESCOLARIDADE", txtEscolaridade);
        linhaFormulario(grade, 5, "PROFISSÃO", txtProfissao);
        if(existente!=null) {
            txtNome.setText(existente.getNome()); txtParentesco.setText(existente.getParentesco());
            txtIdade.setText(existente.getIdade()==null?"":existente.getIdade().toString());
            txtRenda.setText(existente.getRenda()==null?"":String.format(new java.util.Locale("pt","BR"),"%.2f",existente.getRenda()));
            txtEscolaridade.setText(existente.getEscolaridade()); txtProfissao.setText(existente.getProfissao());
        }

        boolean salvar = exibirFormulario(dialogo, grade, erro, () -> {
            String nome = txtNome.getText().trim();
            if (nome.isEmpty() || nome.length() > 80) return "Informe o nome (até 80 caracteres).";
            String parentesco = txtParentesco.getText().trim();
            if (parentesco.isEmpty() || parentesco.length() > 80) return "Informe o parentesco.";
            if (!txtIdade.getText().isBlank()) {
                try {
                    int idade = Integer.parseInt(txtIdade.getText().trim());
                    if (idade < 0 || idade > 120) return "Idade inválida.";
                } catch (NumberFormatException ex) {
                    return "A idade deve ser um número inteiro.";
                }
            }
            if (!txtRenda.getText().isBlank()) {
                try {
                    Formatadores.lerDecimal(txtRenda.getText());
                } catch (NumberFormatException ex) {
                    return "Renda inválida. Use o formato 1500,00.";
                }
            }
            if (txtEscolaridade.getText().trim().length() > 80) return "Escolaridade: no máximo 80 caracteres.";
            if (txtProfissao.getText().trim().length() > 120) return "Profissão: no máximo 120 caracteres.";
            return null;
        });
        if (!salvar) return;

        try {
            ComposicaoFamiliar f = new ComposicaoFamiliar();
            f.setIdComposicaoFamiliar(existente==null?0:existente.getIdComposicaoFamiliar());
            f.setNome(txtNome.getText().trim());
            f.setParentesco(txtParentesco.getText().trim());
            f.setIdade(txtIdade.getText().isBlank() ? null : Integer.valueOf(txtIdade.getText().trim()));
            f.setRenda(txtRenda.getText().isBlank() ? null : Double.valueOf(Formatadores.lerDecimal(txtRenda.getText())));
            f.setEscolaridade(nuloSeVazio(txtEscolaridade.getText()));
            f.setProfissao(nuloSeVazio(txtProfissao.getText()));
            f.setCpfAdolescente(cpfAtual);

            if (existente==null ? composicaoDAO.inserir(f) : composicaoDAO.atualizar(f)) {
                carregarAbaFamilia();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro ao salvar", "Não foi possível salvar o familiar.");
            }
        } catch (IllegalArgumentException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Dados inválidos", e.getMessage());
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    @FXML public void editarSituacaoSocial() { editarCadastro(); }

    private Dialog<ButtonType> criarDialogo(String titulo) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(null);
        if (lblNome.getScene() != null) {
            dialogo.initOwner(lblNome.getScene().getWindow());
        }
        dialogo.getDialogPane().getStylesheets()
                .add(getClass().getResource("/View/adolescentes-style.css").toExternalForm());
        return dialogo;
    }

    private boolean exibirFormulario(Dialog<ButtonType> dialogo, Node conteudo, Label erro, Supplier<String> validador) {
        VBox raiz = new VBox(12, conteudo, erro);
        raiz.setPrefWidth(500);
        dialogo.getDialogPane().setContent(raiz);

        ButtonType salvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(salvar, ButtonType.CANCEL);

        dialogo.getDialogPane().lookupButton(salvar).addEventFilter(ActionEvent.ACTION, evento -> {
            String mensagem = validador.get();
            if (mensagem != null) {
                erro.setText(mensagem);
                evento.consume();
            }
        });

        Optional<ButtonType> resposta = dialogo.showAndWait();
        return resposta.isPresent() && resposta.get() == salvar;
    }

    private GridPane formulario() {
        GridPane grade = new GridPane();
        grade.setHgap(14);
        grade.setVgap(10);
        ColumnConstraints rotulos = new ColumnConstraints();
        rotulos.setMinWidth(150);
        ColumnConstraints campos = new ColumnConstraints();
        campos.setHgrow(Priority.ALWAYS);
        grade.getColumnConstraints().addAll(rotulos, campos);
        return grade;
    }

    private void linhaFormulario(GridPane grade, int linha, String rotulo, Node campo) {
        Label l = new Label(rotulo);
        l.getStyleClass().add("form-label-mini");
        grade.add(l, 0, linha);
        grade.add(campo, 1, linha);
        GridPane.setHgrow(campo, Priority.ALWAYS);
    }

    private Label rotuloErro() {
        Label erro = new Label();
        erro.getStyleClass().add("error-text");
        erro.setWrapText(true);
        return erro;
    }

    private String textoOuVazio(String texto) {
        return texto == null ? "" : texto;
    }

    private String nuloSeVazio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private String nomeParaArquivo(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.trim().replaceAll("[^A-Za-z0-9]+", "_");
    }

    @FXML public void voltarParaLista(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/AdolescentesView.fxml", "Adolescentes"); }
    @FXML public void irParaPainel(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Dashboard.fxml", "Painel de Controle"); }
    @FXML public void irParaAgenda(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/AgendaView.fxml", "Agenda institucional"); }
    @FXML public void irParaRelatorios(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/RelatoriosView.fxml", "Relatórios"); }
    @FXML public void irParaEquipeTecnica(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/EquipeTecnicaView.fxml", "Equipe Técnica"); }
    @FXML public void fazerLogout(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Login.fxml", "Login"); }

    private void erroBanco(RuntimeException e) {
        e.printStackTrace();
        mostrarAlerta(Alert.AlertType.ERROR, "Banco de dados indisponível",
                "Não foi possível acessar o banco de dados.\nVerifique se o PostgreSQL está ligado e tente novamente.");
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
