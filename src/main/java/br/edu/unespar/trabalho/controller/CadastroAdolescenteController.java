package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.dao.*;
import br.edu.unespar.trabalho.model.*;
import javafx.scene.control.*;
import java.time.Period;
import java.time.format.TextStyle;
import java.util.Locale;
import br.edu.unespar.trabalho.model.Adolescente;
import br.edu.unespar.trabalho.model.StatusAdolescente;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import br.edu.unespar.trabalho.model.AdolescenteDTO;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.time.LocalDate;

public class CadastroAdolescenteController {

    @FXML private TextField txtNome;
    @FXML private TextField txtCpf;
    @FXML private DatePicker dpNascimento;
    @FXML private TextField txtContato;
    @FXML private TextField txtEmail;

    @FXML private TextField txtNaturalidade;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private ComboBox<String> cmbCorRaca;
    @FXML private TextField txtBairro; // NOVO CAMPO BAIRRO LIGADO AO FXML

    @FXML private Label lblTitulo, lblIdade, lblAvisoIdade;
    @FXML private ComboBox<String> cmbMedidaProtetiva;
    @FXML private TextField txtEndereco, txtRenda, txtBeneficios, txtNis, txtCras;
    @FXML private TextField txtUbs, txtSpa, txtEscola, txtSerie, txtLocalTrabalho, txtFuncao, txtVinculo;
    @FXML private TextArea txtObservacoes;
    @FXML private CheckBox chkEstuda, chkTrabalha, chkSpa, chkImm, chkValeTransporte, chkPiaEnviado;
    @FXML private ComboBox<StatusAdolescente> cmbStatus;
    private boolean edicao;
    private Saude saudeAtual;

    private AdolescenteDAO adolescenteDAO = new AdolescenteDAO();

    @FXML
    public void initialize() {
        cmbGenero.setItems(FXCollections.observableArrayList("Masculino", "Feminino", "Outro", "Não informado"));
        cmbCorRaca.setItems(FXCollections.observableArrayList("Branca", "Preta", "Parda", "Amarela", "Indígena"));

        cmbStatus.setItems(FXCollections.observableArrayList(StatusAdolescente.values()));
        cmbStatus.setValue(StatusAdolescente.ATIVO);
        cmbMedidaProtetiva.setItems(FXCollections.observableArrayList("Não", "Sim"));
        cmbMedidaProtetiva.setValue("Não");
        txtRenda.setText("0");
        txtEscola.disableProperty().bind(chkEstuda.selectedProperty().not());
        txtSerie.disableProperty().bind(chkEstuda.selectedProperty().not());
        txtLocalTrabalho.disableProperty().bind(chkTrabalha.selectedProperty().not());
        txtFuncao.disableProperty().bind(chkTrabalha.selectedProperty().not());
        txtVinculo.disableProperty().bind(chkTrabalha.selectedProperty().not());
        txtSpa.disableProperty().bind(chkSpa.selectedProperty().not());
        dpNascimento.valueProperty().addListener((o,a,b) -> atualizarIdade());
        aplicarMascaraCPF(txtCpf);
        aplicarMascaraTelefone(txtContato);
    }

    private void aplicarMascaraCPF(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            String limpo = newValue.replaceAll("[^0-9]", "");
            if (limpo.length() > 11) limpo = limpo.substring(0, 11);

            StringBuilder formatado = new StringBuilder();
            for (int i = 0; i < limpo.length(); i++) {
                if (i == 3 || i == 6) formatado.append(".");
                if (i == 9) formatado.append("-");
                formatado.append(limpo.charAt(i));
            }

            if (!newValue.equals(formatado.toString())) {
                textField.setText(formatado.toString());
                textField.positionCaret(formatado.length());
            }
        });
    }

    private void aplicarMascaraTelefone(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            String limpo = newValue.replaceAll("[^0-9]", "");
            if (limpo.length() > 11) limpo = limpo.substring(0, 11);

            StringBuilder formatado = new StringBuilder();
            for (int i = 0; i < limpo.length(); i++) {
                if (i == 0) formatado.append("(");
                if (i == 2) formatado.append(") ");
                if (limpo.length() == 11 && i == 7) formatado.append("-");
                if (limpo.length() < 11 && i == 6) formatado.append("-");
                formatado.append(limpo.charAt(i));
            }

            if (!newValue.equals(formatado.toString())) {
                textField.setText(formatado.toString());
                textField.positionCaret(formatado.length());
            }
        });
    }


    private void atualizarIdade() {
        LocalDate data=dpNascimento.getValue();
        lblIdade.setText(data==null ? "Idade e mês de aniversário calculados pelo nascimento." :
                Period.between(data,LocalDate.now()).getYears()+" anos · aniversário em "+data.getMonth().getDisplayName(TextStyle.FULL,new Locale("pt","BR")));
        int idade=data==null ? 0 : Period.between(data,LocalDate.now()).getYears();
        boolean foraDaFaixa=data!=null && !data.isAfter(LocalDate.now()) && (idade<12 || idade>21);
        lblAvisoIdade.setText(foraDaFaixa ? "Aviso: esta pessoa tem "+idade+" anos e está fora da faixa de atendimento de 12 a 21 anos. O cadastro pode ser salvo." : "");
        lblAvisoIdade.setVisible(foraDaFaixa);
        lblAvisoIdade.setManaged(foraDaFaixa);
    }

    public void carregarParaEdicao(long cpf) {
        Adolescente a=adolescenteDAO.buscarPorCpf(cpf);
        if(a==null) throw new IllegalArgumentException("Cadastro não encontrado.");
        SituacaoSocial ss=new SituacaoSocialDAO().buscarPorCpf(cpf);
        saudeAtual=new SaudeDAO().buscarPorCpf(cpf);
        EducacaoTrabalho et=new EducacaoTrabalhoDAO().buscarPorCpf(cpf);
        edicao=true; lblTitulo.setText("Editar cadastro"); txtCpf.setDisable(true);
        txtCpf.setText(String.format("%011d",cpf)); txtNome.setText(a.getNomeCompleto());
        dpNascimento.setValue(a.getDataNascimento()); txtContato.setText(a.getContato()); txtEmail.setText(a.getEmail());
        txtNaturalidade.setText(a.getNaturalidade()); cmbGenero.setValue(a.getGenero()); cmbCorRaca.setValue(a.getCorRaca());
        cmbStatus.setValue(a.getStatus()); txtObservacoes.setText(a.getObservacoes());
        chkImm.setSelected(a.isImm()); chkValeTransporte.setSelected(a.isValeTransporte());
        chkPiaEnviado.setSelected(a.isPiaEnviado());
        cmbMedidaProtetiva.setValue(a.isMedidaProtetiva() ? "Sim" : "Não");
        if(ss!=null) {
            txtEndereco.setText(ss.getEndereco()); txtBairro.setText(ss.getBairro());
            txtRenda.setText(Double.toString(ss.getRendaFamiliar())); txtBeneficios.setText(ss.getBeneficioSocial());
            txtNis.setText(ss.getNumeroNis()==0 ? "" : Long.toString(ss.getNumeroNis())); txtCras.setText(ss.getCrasNome());
        }
        if(saudeAtual!=null) { txtUbs.setText(saudeAtual.getUbsReferencia()); chkSpa.setSelected(saudeAtual.isUsoSpa()); txtSpa.setText(saudeAtual.getSubstanciasUtilizadas()); }
        if(et!=null) {
            chkEstuda.setSelected(et.isEstuda()); txtEscola.setText(et.getEscola()); txtSerie.setText(et.getSerie());
            chkTrabalha.setSelected(et.isTrabalha()); txtLocalTrabalho.setText(et.getLocalTrabalho());
            txtFuncao.setText(et.getFuncao()); txtVinculo.setText(et.getVinculoEmpregaticio());
        }
    }

    @FXML
    public void salvarAdolescente(ActionEvent event) {
        try {
            // O DatePicker pode conter texto digitado ainda não confirmado com Enter.
            dpNascimento.commitValue();
            if(txtNome.getText().isBlank() || dpNascimento.getValue()==null)
                throw new IllegalArgumentException("Preencha nome, CPF e data de nascimento.");
            String cpf=txtCpf.getText().replaceAll("\\D","");
            if(cpf.length()!=11) throw new IllegalArgumentException("O CPF deve ter 11 dígitos.");
            if(dpNascimento.getValue().isAfter(LocalDate.now())) throw new IllegalArgumentException("Nascimento não pode estar no futuro.");
            String email=texto(txtEmail,90,"E-mail");
            if(!email.isBlank() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("E-mail inválido.");
            Adolescente a=new Adolescente();
            a.setCpf(Long.parseLong(cpf)); a.setNomeCompleto(texto(txtNome,80,"Nome")); a.setDataNascimento(dpNascimento.getValue());
            a.setContato(texto(txtContato,30,"Telefone")); a.setEmail(email);
            a.setNaturalidade(texto(txtNaturalidade,120,"Naturalidade"));
            a.setGenero(cmbGenero.getValue()==null ? "Não informado" : cmbGenero.getValue());
            a.setCorRaca(cmbCorRaca.getValue()==null ? "Não informada" : cmbCorRaca.getValue());
            a.setStatus(cmbStatus.getValue()); a.setObservacoes(txtObservacoes.getText().trim());
            a.setImm(chkImm.isSelected()); a.setValeTransporte(chkValeTransporte.isSelected());
            a.setPiaEnviado(chkPiaEnviado.isSelected());
            a.setMedidaProtetiva("Sim".equals(cmbMedidaProtetiva.getValue()));
            SituacaoSocial ss=new SituacaoSocial();
            ss.setRendaFamiliar(Double.parseDouble(txtRenda.getText().trim().replace(',','.')));
            ss.setEndereco(texto(txtEndereco,60,"Endereço")); ss.setBairro(texto(txtBairro,60,"Bairro"));
            ss.setBeneficioSocial(texto(txtBeneficios,256,"Benefícios")); ss.setCrasNome(texto(txtCras,120,"CRAS"));
            String nis=txtNis.getText().trim();
            if(!nis.isEmpty() && !nis.matches("[0-9]{1,11}")) throw new IllegalArgumentException("NIS: informe até 11 dígitos.");
            ss.setNumeroNis(nis.isEmpty()?0:Long.parseLong(nis)); a.setBairro(ss.getBairro());
            Saude saude=saudeAtual==null ? new Saude() : saudeAtual;
            saude.setUbsReferencia(texto(txtUbs,120,"UBS")); saude.setUsoSpa(chkSpa.isSelected());
            saude.setSubstanciasUtilizadas(chkSpa.isSelected()?texto(txtSpa,90,"Substâncias"):null);
            EducacaoTrabalho et=new EducacaoTrabalho();
            et.setEstuda(chkEstuda.isSelected()); et.setEscola(et.isEstuda()?texto(txtEscola,120,"Escola"):null);
            et.setSerie(et.isEstuda()?texto(txtSerie,40,"Série"):null); et.setTrabalha(chkTrabalha.isSelected());
            et.setLocalTrabalho(et.isTrabalha()?texto(txtLocalTrabalho,80,"Local de trabalho"):null);
            et.setFuncao(et.isTrabalha()?texto(txtFuncao,120,"Função"):null);
            et.setVinculoEmpregaticio(et.isTrabalha()?texto(txtVinculo,80,"Vínculo empregatício"):null);
            adolescenteDAO.salvarCadastro(a,ss,saude,et,edicao);
            mostrarAlerta(Alert.AlertType.INFORMATION,"Cadastro salvo","Dados salvos. Responsáveis, integrantes da família, medidas e técnico de referência ficam no perfil.");
            abrirPerfil(event,a.getCpf());
        } catch(NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING,"Valor inválido","Confira os campos numéricos. Renda é informada em salários mínimos, por exemplo: 1,5.");
        } catch(IllegalArgumentException | java.time.format.DateTimeParseException e) {
            mostrarAlerta(Alert.AlertType.WARNING,"Confira os dados",e.getMessage());
        } catch(RuntimeException e) {
            e.printStackTrace(); mostrarAlerta(Alert.AlertType.ERROR,"Erro ao salvar",e.getMessage()+"\nConfira a conexão e o script sql/ajustes_cadastro_frequencia.sql.");
        }
    }

    private String texto(TextField campo,int max,String nome) {
        String v=campo.getText()==null ? "" : campo.getText().trim();
        if(v.length()>max) throw new IllegalArgumentException(nome+": máximo de "+max+" caracteres.");
        return v;
    }

    private void abrirPerfil(ActionEvent event, long cpf) {
        try {
            String cpfDigitos = String.format("%011d", cpf);
            AdolescenteDTO dto = null;
            for (AdolescenteDTO d : adolescenteDAO.listarResumoDTO()) {
                if (d.getCpf().replaceAll("\\D", "").equals(cpfDigitos)) {
                    dto = d;
                    break;
                }
            }
            if (dto == null) {
                voltarParaLista(event);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/DetalhesAdolescenteView.fxml"));
            Parent root = loader.load();
            DetalhesAdolescenteController controller = loader.getController();
            controller.carregarDados(dto);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            NavegacaoUtil.trocarRaiz(stage, root, "Instituto C.A.S.A. - Perfil de " + dto.getNome());
        } catch (Exception e) {
            e.printStackTrace();
            voltarParaLista(event);
        }
    }

    @FXML public void voltarParaLista(ActionEvent event) { NavegacaoUtil.mudarTela(event, "/View/AdolescentesView.fxml", "Adolescentes"); }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    @FXML public void irParaPainel(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Dashboard.fxml", "Painel de Controle"); }
    @FXML public void irParaAgenda(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/AgendaView.fxml", "Agenda institucional"); }
    @FXML public void irParaRelatorios(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/RelatoriosView.fxml", "Relatórios"); }
    @FXML public void irParaEquipeTecnica(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/EquipeTecnicaView.fxml", "Equipe Técnica"); }
    @FXML public void fazerLogout(ActionEvent e) { NavegacaoUtil.mudarTela(e, "/View/Login.fxml", "Login"); }
}
