package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
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
import br.edu.unespar.trabalho.util.PdfSimples;
import br.edu.unespar.trabalho.util.Sessao;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
    @FXML private Label lblFreqPercentual;
    @FXML private VBox boxTabelaFrequencia;

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

    private long cpfAtual;
    private AdolescenteDTO jovemAtual;
    private PIA piaAtual;

    @FXML
    public void initialize() {
        cmbTipoMedida.setItems(FXCollections.observableArrayList(TipoMedida.values()));

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
            carregarAbaMedida();
            carregarAbaFrequencia();
            carregarAbaPia();
            carregarAbaFamilia();
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    private void carregarAbaMedida() {
        MedidaSocioeducativa medida = perfilDAO.buscarMedidaAtual(cpfAtual);
        boolean temMedida = medida != null;

        alternar(boxMedidaCadastrada, temMedida);
        alternar(boxMedidaForm, !temMedida);

        if (temMedida) {
            TipoMedida tipo = medida.getTipoMedida();
            lblMedidaTipo.setText(tipo != null ? tipo.getDescricao() : "-");
            lblMedidaInicio.setText(medida.getDataInicio().format(FORMATO_DATA));
            lblMedidaDuracao.setText(medida.isPSC()
                    ? medida.getDuracaoHoras() + " horas"
                    : medida.getDuracaoMeses() + " meses");
            lblMedidaReincidencia.setText(medida.isReincidencia() ? "Sim" : "Não");
            String historico = medida.getHistoricoInfracional();
            lblMedidaHistorico.setText(historico == null || historico.isBlank() ? "-" : historico);
        }

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

    /** Depois de salvar algo, busca o resumo atualizado e redesenha o cabeçalho e as abas. */
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

    // ===================== ABAS =====================

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

    // ===================== MEDIDA SOCIOEDUCATIVA =====================

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
            medida.setIdMedida(IdUtil.proximoId("MedidaSocioeducativa"));
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

            if (medidaDAO.inserir(medida)) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Medida cadastrada com sucesso!");
                recarregarPerfil();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro ao salvar",
                        "Não foi possível salvar a medida. Verifique os dados e tente novamente.");
            }
        } catch (IllegalArgumentException e) {
            // regras de negócio do model (MedidaSocioeducativa.validar)
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

    // ===================== FREQUÊNCIA E HORAS =====================

    private void carregarAbaFrequencia() {
        List<FrequenciaLinhaDTO> linhas = perfilDAO.listarFrequencia(cpfAtual);

        int presencas = 0;
        int faltas = 0;
        int horas = 0;
        for (FrequenciaLinhaDTO l : linhas) {
            if (l.status() == StatusPresenca.PRESENTE) {
                presencas++;
                if (l.horas() != null) horas += l.horas();
            } else {
                faltas++;
            }
        }
        int total = presencas + faltas;

        lblFreqPresencas.setText(String.valueOf(presencas));
        lblFreqFaltas.setText(String.valueOf(faltas));
        lblFreqHoras.setText(horas + "h");
        lblFreqPercentual.setText(total == 0 ? "-" : Math.round(presencas * 100.0 / total) + "%");

        construirTabelaFrequencia(linhas);
    }

    private void construirTabelaFrequencia(List<FrequenciaLinhaDTO> linhas) {
        boxTabelaFrequencia.getChildren().clear();

        if (linhas.isEmpty()) {
            Label vazio = new Label("Nenhuma frequência registrada ainda.");
            vazio.getStyleClass().add("empty-text");
            boxTabelaFrequencia.getChildren().add(vazio);
            return;
        }

        GridPane grade = new GridPane();
        for (double largura : new double[]{18, 30, 14, 38}) {
            ColumnConstraints coluna = new ColumnConstraints();
            coluna.setPercentWidth(largura);
            grade.getColumnConstraints().add(coluna);
        }

        String[] titulos = {"DATA", "STATUS", "HORAS", "ATIVIDADE"};
        for (int c = 0; c < titulos.length; c++) {
            Label cabecalho = new Label(titulos[c]);
            cabecalho.getStyleClass().add("table-head");
            cabecalho.setMaxWidth(Double.MAX_VALUE);
            grade.add(cabecalho, c, 0);
        }

        int linha = 1;
        for (FrequenciaLinhaDTO l : linhas) {
            grade.add(celulaTabela(textoTabela(Formatadores.dataCurta(l.data()), "table-cell-mono")), 0, linha);
            grade.add(celulaTabela(selo(l.status())), 1, linha);

            boolean mostraHoras = l.status() == StatusPresenca.PRESENTE && l.horas() != null;
            grade.add(celulaTabela(textoTabela(mostraHoras ? l.horas() + "h" : "—", "table-cell-text")), 2, linha);
            grade.add(celulaTabela(textoTabela(Formatadores.textoOuTraco(l.atividade()), "table-cell-text")), 3, linha);
            linha++;
        }
        boxTabelaFrequencia.getChildren().add(grade);
    }

    private Label selo(StatusPresenca status) {
        Label selo = new Label(status.getDescricao());
        switch (status) {
            case PRESENTE -> selo.getStyleClass().add("badge-presente");
            case FALTA_JUSTIFICADA -> selo.getStyleClass().add("badge-justificada");
            default -> selo.getStyleClass().add("badge-falta");
        }
        return selo;
    }

    private Label textoTabela(String texto, String estilo) {
        Label l = new Label(texto);
        l.getStyleClass().add(estilo);
        return l;
    }

    private HBox celulaTabela(Node conteudo) {
        HBox caixa = new HBox(conteudo);
        caixa.setAlignment(Pos.CENTER_LEFT);
        caixa.getStyleClass().add("table-cell-box");
        caixa.setMaxWidth(Double.MAX_VALUE);
        return caixa;
    }

    @FXML
    public void registrarPresenca() {
        try {
            Dialog<ButtonType> dialogo = criarDialogo("Registrar presença");

            ComboBox<Atividade> cmbAtividade =
                    new ComboBox<>(FXCollections.observableArrayList(atividadeDAO.listar()));
            cmbAtividade.setPromptText("Selecione a atividade");
            cmbAtividade.setMaxWidth(Double.MAX_VALUE);
            cmbAtividade.setConverter(new StringConverter<>() {
                @Override
                public String toString(Atividade a) {
                    return a == null ? "" : a.getNomeAtividade() + " (" + a.getCargaHoraria() + "h)";
                }

                @Override
                public Atividade fromString(String texto) {
                    return null;
                }
            });

            Button btnNova = new Button("Nova atividade");
            btnNova.getStyleClass().add("btn-outline");

            DatePicker dpData = new DatePicker(LocalDate.now());
            dpData.setMaxWidth(Double.MAX_VALUE);

            ComboBox<StatusPresenca> cmbStatus =
                    new ComboBox<>(FXCollections.observableArrayList(StatusPresenca.values()));
            cmbStatus.setValue(StatusPresenca.PRESENTE);
            cmbStatus.setMaxWidth(Double.MAX_VALUE);

            TextField txtHoras = new TextField();
            txtHoras.setPromptText("Ex.: 4");

            Label erro = rotuloErro();

            cmbAtividade.valueProperty().addListener((obs, antiga, nova) -> {
                if (nova != null && cmbStatus.getValue() == StatusPresenca.PRESENTE) {
                    txtHoras.setText(String.valueOf(nova.getCargaHoraria()));
                }
            });
            cmbStatus.valueProperty().addListener((obs, antigo, novo) -> {
                boolean presente = novo == StatusPresenca.PRESENTE;
                txtHoras.setDisable(!presente);
                if (!presente) {
                    txtHoras.clear();
                } else if (cmbAtividade.getValue() != null) {
                    txtHoras.setText(String.valueOf(cmbAtividade.getValue().getCargaHoraria()));
                }
            });
            btnNova.setOnAction(e -> {
                Atividade criada = dialogoNovaAtividade();
                if (criada != null) {
                    cmbAtividade.getItems().add(criada);
                    cmbAtividade.setValue(criada);
                }
            });

            HBox linhaAtividade = new HBox(8, cmbAtividade, btnNova);
            HBox.setHgrow(cmbAtividade, Priority.ALWAYS);

            GridPane grade = formulario();
            linhaFormulario(grade, 0, "ATIVIDADE *", linhaAtividade);
            linhaFormulario(grade, 1, "DATA *", dpData);
            linhaFormulario(grade, 2, "SITUAÇÃO *", cmbStatus);
            linhaFormulario(grade, 3, "HORAS CUMPRIDAS", txtHoras);

            boolean salvar = exibirFormulario(dialogo, grade, erro, () -> {
                if (cmbAtividade.getValue() == null) return "Selecione a atividade (ou crie uma nova).";
                if (dpData.getValue() == null) return "Informe a data.";
                if (dpData.getValue().isAfter(LocalDate.now())) return "A data não pode ser futura.";
                if (cmbStatus.getValue() == StatusPresenca.PRESENTE) {
                    try {
                        int h = Integer.parseInt(txtHoras.getText().trim());
                        if (h <= 0 || h > 24) return "Informe as horas cumpridas (de 1 a 24).";
                    } catch (NumberFormatException ex) {
                        return "Informe as horas cumpridas (número inteiro).";
                    }
                }
                return null;
            });
            if (!salvar) return;

            Atividade atividade = cmbAtividade.getValue();
            LocalDate data = dpData.getValue();

            if (perfilDAO.existeFrequencia(cpfAtual, atividade.getIdAtividade(), data)) {
                mostrarAlerta(Alert.AlertType.WARNING, "Registro duplicado",
                        "Já existe um registro deste adolescente nesta atividade nesta data.");
                return;
            }

            Frequencia f = new Frequencia();
            f.setCpfAdolescente(cpfAtual);
            f.setIdAtividade(atividade.getIdAtividade());
            f.setDataPresenca(data);
            f.setStatusPresenca(cmbStatus.getValue());
            f.setHorasCumpridas(cmbStatus.getValue() == StatusPresenca.PRESENTE
                    ? Integer.valueOf(txtHoras.getText().trim()) : null);

            if (frequenciaDAO.registrar(f)) {
                recarregarPerfil(); // atualiza a tabela, os totais e o progresso da medida no cabeçalho
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro ao salvar",
                        "Não foi possível registrar a frequência.");
            }
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    /** Cadastra uma atividade (oficina, curso, local de PSC...) e devolve ela, ou null se cancelou/falhou. */
    private Atividade dialogoNovaAtividade() {
        Dialog<ButtonType> dialogo = criarDialogo("Nova atividade");

        TextField txtNome = new TextField();
        txtNome.setPromptText("Até 25 caracteres");
        TextField txtCarga = new TextField();
        txtCarga.setPromptText("Horas por encontro. Ex.: 4");
        Label erro = rotuloErro();

        GridPane grade = formulario();
        linhaFormulario(grade, 0, "NOME *", txtNome);
        linhaFormulario(grade, 1, "CARGA HORÁRIA (h) *", txtCarga);

        boolean salvar = exibirFormulario(dialogo, grade, erro, () -> {
            String nome = txtNome.getText().trim();
            if (nome.isEmpty()) return "Informe o nome da atividade.";
            if (nome.length() > 25) return "O nome pode ter no máximo 25 caracteres.";
            try {
                if (Integer.parseInt(txtCarga.getText().trim()) <= 0) return "A carga horária deve ser maior que zero.";
            } catch (NumberFormatException ex) {
                return "Informe a carga horária (número inteiro).";
            }
            return null;
        });
        if (!salvar) return null;

        Atividade a = new Atividade();
        a.setIdAtividade(IdUtil.proximoId("Atividade"));
        a.setNomeAtividade(txtNome.getText().trim());
        a.setCargaHoraria(Integer.parseInt(txtCarga.getText().trim()));

        if (atividadeDAO.inserir(a)) {
            return a;
        }
        mostrarAlerta(Alert.AlertType.ERROR, "Erro ao salvar", "Não foi possível cadastrar a atividade.");
        return null;
    }

    // ===================== PIA =====================

    private void carregarAbaPia() {
        piaAtual = piaDAO.buscarPorCpf(cpfAtual);

        if (piaAtual == null) {
            alternar(boxPiaCadastrado, false);
            alternar(boxPiaForm, true);
            alternar(lblPiaStatus, false);
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

    /** Preenche o formulário: vazio para um PIA novo, ou com os dados do PIA que está sendo editado. */
    private void prepararFormularioPia(PIA modelo) {
        boolean edicao = modelo != null;
        lblPiaFormTitulo.setText(edicao ? "Editar PIA" : "Elaborar PIA");
        dpPiaData.setValue(edicao ? modelo.getDataElaboracao() : LocalDate.now());
        dpPiaData.setDisable(edicao); // a data de elaboração não muda depois de gravada
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
                        "Não foi possível salvar o PIA.\nSe o erro for de tamanho de texto, execute o script "
                                + "ajustes_ddl.sql no banco.");
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
    public void gerarPdfPia() {
        if (piaAtual == null) return;

        FileChooser seletor = new FileChooser();
        seletor.setTitle("Salvar relatório do PIA");
        seletor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        seletor.setInitialFileName("PIA_" + nomeParaArquivo(jovemAtual.getNome()) + "_" + LocalDate.now() + ".pdf");
        File destino = seletor.showSaveDialog(lblNome.getScene().getWindow());
        if (destino == null) return;

        try {
            EquipeTecnicaDTO usuario = Sessao.getUsuarioLogado();
            String emitidoPor = usuario != null && usuario.getLogin() != null ? " por " + usuario.getLogin() : "";

            PdfSimples pdf = new PdfSimples();
            pdf.titulo("Plano Individual de Atendimento (PIA)");
            pdf.subtitulo("Instituto C.A.S.A. - emitido em " + Formatadores.dataCurta(LocalDate.now()) + emitidoPor);
            pdf.espaco(4);
            pdf.linha();

            pdf.rotulo("Adolescente");
            pdf.paragrafo(jovemAtual.getNome() + "  |  CPF " + jovemAtual.getCpf());
            pdf.rotulo("Medida socioeducativa");
            pdf.paragrafo(jovemAtual.getMedida() + "  |  Progresso: " + jovemAtual.getTextoProgresso());
            pdf.rotulo("Técnico de referência");
            pdf.paragrafo(jovemAtual.getTecnico());

            pdf.espaco(6);
            pdf.linha();

            pdf.rotulo("Data de elaboração");
            pdf.paragrafo(Formatadores.dataExtenso(piaAtual.getDataElaboracao()));
            pdf.rotulo("Técnico(a) responsável pelo PIA");
            pdf.paragrafo(Formatadores.textoOuTraco(perfilDAO.buscarNomeAutorPia(piaAtual.getIdPia())));
            pdf.rotulo("Diagnóstico inicial");
            pdf.paragrafo(Formatadores.textoOuTraco(piaAtual.getDiagnostico()));
            pdf.rotulo("Vulnerabilidades identificadas");
            pdf.paragrafo(Formatadores.textoOuTraco(piaAtual.getVulnerabilidades()));
            pdf.rotulo("Potencialidades");
            pdf.paragrafo(Formatadores.textoOuTraco(piaAtual.getPotencialidades()));
            pdf.rotulo("Estratégias de intervenção");
            pdf.paragrafo(Formatadores.textoOuTraco(piaAtual.getEstrategias()));
            pdf.rotulo("Documento enviado ao CREAS");
            pdf.paragrafo(piaAtual.isDocumentoEnviado() ? "Sim" : "Não");

            pdf.salvar(destino.toPath());
            mostrarAlerta(Alert.AlertType.INFORMATION, "Relatório gerado", "PDF salvo em:\n" + destino.getAbsolutePath());
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Erro ao gerar PDF", "Não foi possível salvar o arquivo PDF.");
        }
    }

    // ===================== FAMÍLIA E SOCIAL =====================

    private void carregarAbaFamilia() {
        // Responsáveis legais
        boxResponsaveis.getChildren().clear();
        List<Responsavel> responsaveis = responsavelDAO.listarResponsaveis(cpfAtual);
        if (responsaveis.isEmpty()) {
            boxResponsaveis.getChildren().add(textoVazio("Nenhum responsável cadastrado."));
        }
        for (Responsavel r : responsaveis) {
            String contato = r.getContato() != null && !r.getContato().isBlank() ? " · " + r.getContato() : "";
            boxResponsaveis.getChildren().add(cartaoPessoa(r.getNomeCompleto(),
                    Formatadores.textoOuTraco(r.getParentesco()) + contato,
                    r.isContatoPrincipal() ? "Contato principal" : null));
        }

        // Demais membros da família
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
            boxComposicao.getChildren().add(cartaoPessoa(f.getNome(), sub.toString(), null));
        }

        // Situação social + UBS (tabela Saude)
        SituacaoSocial social = situacaoSocialDAO.buscarPorCpf(cpfAtual);
        Saude saude = saudeDAO.buscarPorCpf(cpfAtual);
        boolean temDados = social != null || saude != null;

        alternar(boxSocialVazio, !temDados);
        alternar(gridSocial, temDados);
        btnEditarSocial.setText(temDados ? "Editar" : "Cadastrar");

        if (temDados) {
            lblSocBairro.setText(social != null ? Formatadores.textoOuTraco(social.getBairro()) : "-");
            lblSocRenda.setText(social != null ? Formatadores.moeda(social.getRendaFamiliar()) + " / mês" : "-");
            lblSocBeneficios.setText(social != null ? Formatadores.textoOuTraco(social.getBeneficioSocial()) : "-");
            lblSocCras.setText(social != null && social.getCrasReferencia() != null
                    ? "CRAS nº " + social.getCrasReferencia() : "-");
            lblSocNis.setText(social != null ? String.valueOf(social.getNumeroNis()) : "-");
            lblSocUbs.setText(saude != null ? Formatadores.textoOuTraco(saude.getUbsReferencia()) : "-");
            lblSocEndereco.setText(social != null ? Formatadores.textoOuTraco(social.getEndereco()) : "-");
            lblSocTelefone.setText(social != null ? Formatadores.textoOuTraco(social.getTelefone()) : "-");
        }
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

    @FXML
    public void adicionarResponsavel() {
        Dialog<ButtonType> dialogo = criarDialogo("Adicionar responsável");

        TextField txtNome = new TextField();
        TextField txtCpf = new TextField();
        txtCpf.setPromptText("Somente números");
        DatePicker dpNascimento = new DatePicker();
        dpNascimento.setMaxWidth(Double.MAX_VALUE);
        TextField txtContato = new TextField();
        txtContato.setPromptText("(41) 99999-9999");
        TextField txtEmail = new TextField();
        TextField txtParentesco = new TextField();
        txtParentesco.setPromptText("Ex.: Mãe, Pai, Avó (até 10 letras)");
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

        boolean salvar = exibirFormulario(dialogo, grade, erro, () -> {
            String nome = txtNome.getText().trim();
            if (nome.isEmpty() || nome.length() > 80) return "Informe o nome (até 80 caracteres).";
            String cpf = Formatadores.soDigitos(txtCpf.getText());
            if (cpf.length() != 11) return "O CPF deve ter 11 dígitos.";
            if (Long.parseLong(cpf) == cpfAtual) return "O responsável não pode ter o mesmo CPF do adolescente.";
            if (dpNascimento.getValue() == null || !dpNascimento.getValue().isBefore(LocalDate.now()))
                return "Informe uma data de nascimento válida.";
            String parentesco = txtParentesco.getText().trim();
            if (parentesco.isEmpty() || parentesco.length() > 10) return "Parentesco: de 1 a 10 caracteres.";
            if (txtContato.getText().trim().length() > 15) return "Contato: no máximo 15 caracteres.";
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

            if (responsavelDAO.inserir(r, cpfAtual)) {
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

    @FXML
    public void adicionarFamiliar() {
        Dialog<ButtonType> dialogo = criarDialogo("Adicionar familiar");

        TextField txtNome = new TextField();
        TextField txtParentesco = new TextField();
        TextField txtIdade = new TextField();
        TextField txtRenda = new TextField();
        txtRenda.setPromptText("Ex.: 1500,00");
        TextField txtEscolaridade = new TextField();
        txtEscolaridade.setPromptText("Até 10 caracteres. Ex.: Médio");
        TextField txtProfissao = new TextField();
        Label erro = rotuloErro();

        GridPane grade = formulario();
        linhaFormulario(grade, 0, "NOME *", txtNome);
        linhaFormulario(grade, 1, "PARENTESCO *", txtParentesco);
        linhaFormulario(grade, 2, "IDADE", txtIdade);
        linhaFormulario(grade, 3, "RENDA (R$)", txtRenda);
        linhaFormulario(grade, 4, "ESCOLARIDADE", txtEscolaridade);
        linhaFormulario(grade, 5, "PROFISSÃO", txtProfissao);

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
            if (txtEscolaridade.getText().trim().length() > 10) return "Escolaridade: no máximo 10 caracteres.";
            if (txtProfissao.getText().trim().length() > 25) return "Profissão: no máximo 25 caracteres.";
            return null;
        });
        if (!salvar) return;

        try {
            ComposicaoFamiliar f = new ComposicaoFamiliar();
            f.setIdComposicaoFamiliar(IdUtil.proximoId("ComposicaoFamiliar"));
            f.setNome(txtNome.getText().trim());
            f.setParentesco(txtParentesco.getText().trim());
            f.setIdade(txtIdade.getText().isBlank() ? null : Integer.valueOf(txtIdade.getText().trim()));
            f.setRenda(txtRenda.getText().isBlank() ? null : Double.valueOf(Formatadores.lerDecimal(txtRenda.getText())));
            f.setEscolaridade(nuloSeVazio(txtEscolaridade.getText()));
            f.setProfissao(nuloSeVazio(txtProfissao.getText()));
            f.setCpfAdolescente(cpfAtual);

            if (composicaoDAO.inserir(f)) {
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

    @FXML
    public void editarSituacaoSocial() {
        SituacaoSocial existente;
        Saude saudeExistente;
        try {
            existente = situacaoSocialDAO.buscarPorCpf(cpfAtual);
            saudeExistente = saudeDAO.buscarPorCpf(cpfAtual);
        } catch (RuntimeException e) {
            erroBanco(e);
            return;
        }

        Dialog<ButtonType> dialogo = criarDialogo(existente == null ? "Cadastrar situação social" : "Editar situação social");

        TextField txtBairro = new TextField(existente != null ? textoOuVazio(existente.getBairro()) : "");
        TextField txtEndereco = new TextField(existente != null ? textoOuVazio(existente.getEndereco()) : "");
        TextField txtTelefone = new TextField(existente != null ? textoOuVazio(existente.getTelefone()) : "");
        TextField txtRenda = new TextField(existente != null
                ? String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", existente.getRendaFamiliar()) : "");
        TextField txtNis = new TextField(existente != null ? String.valueOf(existente.getNumeroNis()) : "");
        TextField txtCras = new TextField(existente != null && existente.getCrasReferencia() != null
                ? String.valueOf(existente.getCrasReferencia()) : "");
        TextField txtBeneficios = new TextField(existente != null ? textoOuVazio(existente.getBeneficioSocial()) : "");
        TextField txtUbs = new TextField(saudeExistente != null ? textoOuVazio(saudeExistente.getUbsReferencia()) : "");
        txtRenda.setPromptText("Ex.: 1800,00");
        txtNis.setPromptText("Somente números");
        txtCras.setPromptText("Número do CRAS");
        Label erro = rotuloErro();

        GridPane grade = formulario();
        linhaFormulario(grade, 0, "BAIRRO *", txtBairro);
        linhaFormulario(grade, 1, "ENDEREÇO *", txtEndereco);
        linhaFormulario(grade, 2, "TELEFONE *", txtTelefone);
        linhaFormulario(grade, 3, "RENDA FAMILIAR *", txtRenda);
        linhaFormulario(grade, 4, "NIS *", txtNis);
        linhaFormulario(grade, 5, "CRAS DE REFERÊNCIA", txtCras);
        linhaFormulario(grade, 6, "BENEFÍCIOS SOCIAIS", txtBeneficios);
        linhaFormulario(grade, 7, "UBS DE REFERÊNCIA", txtUbs);

        boolean salvar = exibirFormulario(dialogo, grade, erro, () -> {
            String bairro = txtBairro.getText().trim();
            if (bairro.isEmpty() || bairro.length() > 60) return "Informe o bairro (até 60 caracteres).";
            String endereco = txtEndereco.getText().trim();
            if (endereco.isEmpty() || endereco.length() > 60) return "Informe o endereço (até 60 caracteres).";
            String telefone = txtTelefone.getText().trim();
            if (telefone.isEmpty() || telefone.length() > 15) return "Informe o telefone (até 15 caracteres).";
            try {
                Formatadores.lerDecimal(txtRenda.getText());
            } catch (NumberFormatException ex) {
                return "Renda inválida. Use o formato 1800,00.";
            }
            String nis = Formatadores.soDigitos(txtNis.getText());
            if (nis.isEmpty() || nis.length() > 11) return "O NIS deve ter até 11 dígitos.";
            if (!txtCras.getText().isBlank()) {
                try {
                    Integer.parseInt(txtCras.getText().trim());
                } catch (NumberFormatException ex) {
                    return "O CRAS deve ser um número inteiro.";
                }
            }
            if (txtBeneficios.getText().trim().length() > 256) return "Benefícios: no máximo 256 caracteres.";
            if (txtUbs.getText().trim().length() > 25) return "UBS: no máximo 25 caracteres.";
            return null;
        });
        if (!salvar) return;

        try {
            SituacaoSocial ss = existente != null ? existente : new SituacaoSocial();
            ss.setBairro(txtBairro.getText().trim());
            ss.setEndereco(txtEndereco.getText().trim());
            ss.setTelefone(txtTelefone.getText().trim());
            ss.setRendaFamiliar(Formatadores.lerDecimal(txtRenda.getText()));
            ss.setNumeroNis(Long.parseLong(Formatadores.soDigitos(txtNis.getText())));
            ss.setCrasReferencia(txtCras.getText().isBlank() ? null : Integer.valueOf(txtCras.getText().trim()));
            ss.setBeneficioSocial(nuloSeVazio(txtBeneficios.getText()));

            boolean gravou;
            if (existente != null) {
                gravou = situacaoSocialDAO.atualizar(ss);
            } else {
                ss.setIdSituacaoSocial(IdUtil.proximoId("SituacaoSocial"));
                ss.setCpfAdolescente(cpfAtual);
                gravou = situacaoSocialDAO.inserir(ss);
            }

            String ubs = nuloSeVazio(txtUbs.getText());
            if (gravou && saudeExistente != null) {
                saudeExistente.setUbsReferencia(ubs);
                gravou = saudeDAO.atualizar(saudeExistente);
            } else if (gravou && ubs != null) {
                Saude nova = new Saude();
                nova.setIdFichaSaude(IdUtil.proximoId("Saude"));
                nova.setUbsReferencia(ubs);
                nova.setUsoSpa(false);
                nova.setCpfAdolescente(cpfAtual);
                gravou = saudeDAO.inserir(nova);
            }

            if (gravou) {
                recarregarPerfil(); // também atualiza o bairro nos dados pessoais
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro ao salvar",
                        "Não foi possível salvar os dados.\nSe o NIS ou a renda forem rejeitados, execute o script "
                                + "ajustes_ddl.sql no banco.");
            }
        } catch (IllegalArgumentException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Dados inválidos", e.getMessage());
        } catch (RuntimeException e) {
            erroBanco(e);
        }
    }

    // ===================== AUXILIARES DE FORMULÁRIO =====================

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

    /**
     * Mostra o diálogo com botões Salvar/Cancelar. O validador devolve a mensagem de erro (e o diálogo
     * continua aberto) ou null quando está tudo certo. Retorna true se o usuário confirmou.
     */
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
                evento.consume(); // mantém o diálogo aberto
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

    /** "Lucas Henrique Oliveira" -> "Lucas_Henrique_Oliveira" (sem acentos, seguro para nome de arquivo). */
    private String nomeParaArquivo(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.trim().replaceAll("[^A-Za-z0-9]+", "_");
    }

    // ===================== NAVEGAÇÃO E ALERTAS =====================

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