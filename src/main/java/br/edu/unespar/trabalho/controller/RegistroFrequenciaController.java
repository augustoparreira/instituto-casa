package br.edu.unespar.trabalho.controller;

import static br.edu.unespar.trabalho.service.AutorizacaoService.*;
import static br.edu.unespar.trabalho.model.Operacao.*;
import br.edu.unespar.trabalho.dao.*;
import br.edu.unespar.trabalho.model.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import java.time.LocalDate;

/**
 * Controla o registro, a correção e a exclusão de frequências pelo perfil ou pela planilha mensal.
 */
public class RegistroFrequenciaController {
    @FXML private Label lblAdolescente, lblMensagem;
    @FXML private DatePicker dpData;
    @FXML private ListView<Frequencia> listaRegistros;
    @FXML private ComboBox<Atividade> cmbAtividade;
    @FXML private ComboBox<MedidaSocioeducativa> cmbMedida;
    @FXML private ComboBox<StatusPresenca> cmbStatus;
    @FXML private TextField txtHoras;
    @FXML private TextArea txtObservacoes;
    private long cpf;
    private Frequencia selecionada;
    private Runnable aoSalvar = () -> {};
    private final FrequenciaDAO dao = new FrequenciaDAO();

    @FXML public void initialize() {
        br.edu.unespar.trabalho.util.DatasFormulario.configurar(dpData);

        lblMensagem.sceneProperty().addListener((obs,antes,cena) -> {
            if(cena==null) return;
            javafx.application.Platform.runLater(() -> {
                br.edu.unespar.trabalho.util.ApresentacaoAcesso.identificar(cena.getRoot());
                for(javafx.scene.Node node:cena.getRoot().lookupAll(".button")) {
                    if(!(node instanceof javafx.scene.control.Button button) || button.getOnAction()==null) continue;
                    String texto=button.getText();
                    if("Relatórios".equals(texto)) br.edu.unespar.trabalho.util.ApresentacaoAcesso.aplicar(button,RELATORIO_CONSULTAR);
                    if("Equipe Técnica".equals(texto)) br.edu.unespar.trabalho.util.ApresentacaoAcesso.aplicar(button,USUARIO_GERENCIAR);
                }
            });
        });

        exigir(FREQUENCIA_CONSULTAR);

        cmbStatus.setItems(FXCollections.observableArrayList(StatusPresenca.values()));
        cmbAtividade.setConverter(new StringConverter<>() {
            public String toString(Atividade a) { return a==null?"":a.getNomeAtividade(); }
            public Atividade fromString(String s) { return null; }
        });
        cmbStatus.valueProperty().addListener((o,a,b)-> {
            atualizarHoras(selecionada==null);
        });
        cmbAtividade.valueProperty().addListener((o,a,b)-> {
            atualizarHoras(selecionada==null);
        });
        cmbMedida.valueProperty().addListener((o,a,b)-> {
            atualizarHoras(selecionada==null);
        });
        dpData.valueProperty().addListener((o,a,b)-> { if(cpf!=0 && b!=null) carregarDia(); });
        listaRegistros.getSelectionModel().selectedItemProperty().addListener((o,a,b)-> { if(b!=null) preencher(b); });
    }

    private void atualizarHoras(boolean sugerir) {
        boolean contabiliza=pode(HORAS_PSC_REGISTRAR) && cmbStatus.getValue()==StatusPresenca.PRESENTE
                && cmbMedida.getValue()!=null && cmbMedida.getValue().isPSC();
        txtHoras.setDisable(!contabiliza);
        if(!contabiliza) txtHoras.setText("0");
        else if(sugerir) txtHoras.setText(cmbAtividade.getValue()==null?"0":Integer.toString(cmbAtividade.getValue().getCargaHoraria()));
    }

    public void carregar(long cpf,LocalDate data,Runnable retorno) {
        exigir(FREQUENCIA_CONSULTAR);

        this.cpf=cpf; this.aoSalvar=retorno;
        Adolescente a=new AdolescenteDAO().listarParaFrequencia().stream().filter(p -> p.getCpf()==cpf).findFirst().orElse(null);
        if(a==null) throw new IllegalArgumentException("Adolescente não encontrado.");
        lblAdolescente.setText(a.getNomeCompleto()+" · "+a.getCpfFormatado());
        cmbAtividade.setItems(FXCollections.observableArrayList(new AtividadeDAO().listar()));
        dpData.setValue(data);
    }

    private void carregarDia() {
        try {
            selecionada=null;
            cmbMedida.setItems(FXCollections.observableArrayList(new MedidaSocioeducativaDAO().listarParaFrequencia(cpf)
                    .stream().filter(m->m.vigenteEm(dpData.getValue())).toList()));
            listaRegistros.setItems(FXCollections.observableArrayList(dao.listar(cpf,dpData.getValue(),dpData.getValue())));
            novo();
        } catch(RuntimeException e) { erro(e); }
    }

    @FXML public void novo() {
        selecionada=null; listaRegistros.getSelectionModel().clearSelection(); cmbAtividade.setDisable(false);
        cmbAtividade.setValue(null); cmbMedida.setValue(cmbMedida.getItems().size()==1?cmbMedida.getItems().getFirst():null);
        cmbStatus.setValue(StatusPresenca.PRESENTE); txtHoras.setText("0"); txtObservacoes.clear();
        atualizarHoras(false);
        lblMensagem.setText("Novo lançamento. Para corrigir um existente, selecione-o na lista.");
    }

    private void preencher(Frequencia f) {
        selecionada=f; cmbAtividade.setValue(cmbAtividade.getItems().stream().filter(a->a.getIdAtividade()==f.getIdAtividade()).findFirst().orElse(null));
        cmbAtividade.setDisable(true);
        cmbMedida.setValue(cmbMedida.getItems().stream().filter(m->f.getIdMedida()!=null && m.getIdMedida()==f.getIdMedida()).findFirst().orElse(null));
        cmbStatus.setValue(f.getStatusPresenca()); txtHoras.setText(Integer.toString(f.getHorasContabilizadas()));
        atualizarHoras(false);
        txtObservacoes.setText(f.getObservacoes()); lblMensagem.setText("Editando o lançamento selecionado. Para trocar atividade ou data, exclua-o e registre novamente.");
    }

    @FXML public void semMedida() { cmbMedida.setValue(null); atualizarHoras(false); }

    @FXML public void salvar() {
        try { exigir(selecionada==null ? FREQUENCIA_REGISTRAR : FREQUENCIA_EDITAR); }
        catch(br.edu.unespar.trabalho.service.AcessoNegadoException negado) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING,negado.getMessage()).showAndWait();
            return;
        }

        try {
            LocalDate dataAnterior = dpData.getValue();
            dpData.commitValue();
            if (dpData.getValue() == null) throw new IllegalArgumentException("Informe a data do encontro.");
            if (!dpData.getValue().equals(dataAnterior)) {
                lblMensagem.setText("Data alterada. Confira os registros do dia e preencha o lançamento.");
                return;
            }
            if(cmbAtividade.getValue()==null) throw new IllegalArgumentException("Selecione uma atividade.");
            Frequencia f=new Frequencia(); f.setCpfAdolescente(cpf); f.setIdAtividade(cmbAtividade.getValue().getIdAtividade());
            f.setDataPresenca(dpData.getValue()); f.setStatusPresenca(cmbStatus.getValue());
            f.setHorasCumpridas(f.isPresente() && cmbMedida.getValue()!=null && cmbMedida.getValue().isPSC()?Integer.parseInt(txtHoras.getText().trim()):0);
            f.setIdMedida(cmbMedida.getValue()==null?null:cmbMedida.getValue().getIdMedida()); f.setObservacoes(txtObservacoes.getText());
            if(f.getHorasContabilizadas()>0) exigir(HORAS_PSC_REGISTRAR);
            if(selecionada==null) dao.registrar(f); else dao.atualizar(f);
            carregarDia(); aoSalvar.run(); lblMensagem.setText("Frequência salva. Totais recalculados.");
        } catch(NumberFormatException e) { lblMensagem.setText("Informe horas inteiras entre 0 e 24."); }
        catch(java.time.DateTimeException e) { lblMensagem.setText("Informe uma data existente no formato DD/MM/AAAA."); }
        catch(RuntimeException e) { erro(e); }
    }

    @FXML public void excluir() {
        try { exigir(FREQUENCIA_EXCLUIR); }
        catch(br.edu.unespar.trabalho.service.AcessoNegadoException negado) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING,negado.getMessage()).showAndWait();
            return;
        }

        if(selecionada==null) { lblMensagem.setText("Selecione o lançamento a excluir."); return; }
        Alert aviso=new Alert(Alert.AlertType.CONFIRMATION,"Excluir este lançamento? As horas serão recalculadas.",ButtonType.YES,ButtonType.NO);
        if(aviso.showAndWait().orElse(ButtonType.NO)!=ButtonType.YES) return;
        try {
            boolean excluiu=dao.excluir(selecionada);
            carregarDia(); aoSalvar.run();
            lblMensagem.setText(excluiu ? "Frequência excluída. Totais recalculados."
                    : "O lançamento já foi removido. A lista foi atualizada.");
        }
        catch(RuntimeException e) { erro(e); }
    }

    @FXML public void novaAtividade() {
        try { exigir(ATIVIDADE_GERENCIAR); }
        catch(br.edu.unespar.trabalho.service.AcessoNegadoException negado) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING,negado.getMessage()).showAndWait();
            return;
        }

        TextInputDialog nome=new TextInputDialog(); nome.setHeaderText("Nome da atividade (até 25 caracteres)");
        var n=nome.showAndWait(); if(n.isEmpty()) return;
        TextInputDialog carga=new TextInputDialog("4"); carga.setHeaderText("Carga horária do encontro (1 a 24 horas inteiras)");
        var h=carga.showAndWait(); if(h.isEmpty()) return;
        try {
            int horas=Integer.parseInt(h.get().trim());
            if(n.get().isBlank() || n.get().trim().length()>25 || horas<1 || horas>24) throw new IllegalArgumentException("Confira nome e carga horária.");
            Atividade a=new Atividade(); a.setNomeAtividade(n.get().trim()); a.setCargaHoraria(horas);
            if(!new AtividadeDAO().inserir(a)) throw new IllegalStateException("Não foi possível cadastrar a atividade.");
            cmbAtividade.setItems(FXCollections.observableArrayList(new AtividadeDAO().listar()));
            cmbAtividade.setValue(cmbAtividade.getItems().stream().filter(x->x.getIdAtividade()==a.getIdAtividade()).findFirst().orElse(null));
        } catch(RuntimeException e) { erro(e); }
    }

    private void erro(RuntimeException e) { lblMensagem.setText(e.getMessage()); if(!(e instanceof IllegalArgumentException)) e.printStackTrace(); }

    public static void abrir(long cpf,LocalDate data,Runnable retorno) {
        abrir(cpf, data, retorno, null);
    }

    public static void abrir(Frequencia frequencia, Runnable retorno) {
        abrir(frequencia.getCpfAdolescente(), frequencia.getDataPresenca(), retorno, frequencia);
    }

    private static void abrir(long cpf, LocalDate data, Runnable retorno, Frequencia frequencia) {
        try {
            FXMLLoader loader=new FXMLLoader(RegistroFrequenciaController.class.getResource("/View/RegistroFrequenciaView.fxml"));
            Parent root=loader.load();
            RegistroFrequenciaController controller=loader.getController(); controller.carregar(cpf,data,retorno);
            if (frequencia != null) {
                controller.listaRegistros.getItems().stream()
                        .filter(f -> f.getIdAtividade() == frequencia.getIdAtividade())
                        .findFirst().ifPresent(f -> controller.listaRegistros.getSelectionModel().select(f));
            }
            Dialog<ButtonType> dialogo=new Dialog<>(); dialogo.setTitle("Registrar / corrigir frequência");
            dialogo.getDialogPane().setContent(root); dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialogo.setResizable(true); dialogo.showAndWait();
        } catch(Exception e) { e.printStackTrace(); new Alert(Alert.AlertType.ERROR,"Não foi possível abrir a frequência: "+e.getMessage()).showAndWait(); }
    }
}
