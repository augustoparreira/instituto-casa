package br.edu.unespar.trabalho.controller;

import static br.edu.unespar.trabalho.service.AutorizacaoService.*;
import static br.edu.unespar.trabalho.model.Operacao.*;
import br.edu.unespar.trabalho.dao.*;
import br.edu.unespar.trabalho.model.*;
import javafx.scene.control.*;
import java.time.Period;
import java.time.format.TextStyle;
import java.util.Locale;
import br.edu.unespar.trabalho.model.Adolescente;
import br.edu.unespar.trabalho.model.StatusAdolescente;
import br.edu.unespar.trabalho.util.DatasFormulario;
import br.edu.unespar.trabalho.util.Formatadores;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import br.edu.unespar.trabalho.util.MascarasFormulario;
import br.edu.unespar.trabalho.util.ValidacaoFormulario;
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

/**
 * Controla o cadastro e a edição dos dados pessoais, sociais, de saúde e de escolaridade do adolescente.
 */
public class CadastroAdolescenteController {

    @FXML private TextField txtNome;
    @FXML private TextField txtCpf;
    @FXML private DatePicker dpNascimento;
    @FXML private TextField txtContato;
    @FXML private TextField txtEmail;

    @FXML private TextField txtNaturalidade;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private ComboBox<String> cmbCorRaca;
    @FXML private TextField txtBairro;

    @FXML private Label lblTitulo, lblIdade, lblAvisoIdade, lblCpf, lblSituacao, lblAvisoOperacional;
    @FXML private Parent secaoProtegida, linhaEstudo, camposTrabalho, camposSpa;
    @FXML private ComboBox<String> cmbMedidaProtetiva;
    @FXML private TextField txtEndereco, txtRenda, txtBeneficios, txtNis, txtCras;
    @FXML private TextField txtUbs, txtSpa, txtEscola, txtSerie, txtLocalTrabalho, txtFuncao, txtVinculo;
    @FXML private TextArea txtObservacoes;
    @FXML private CheckBox chkEstuda, chkTrabalha, chkSpa, chkImm, chkValeTransporte, chkPiaEnviado;
    @FXML private ComboBox<StatusAdolescente> cmbStatus;
    private boolean edicao;
    private Saude saudeAtual;

    private StatusAdolescente situacaoOriginal;
    private String contatoOriginal, emailOriginal;
    private AdolescenteDAO adolescenteDAO = new AdolescenteDAO();

    @FXML
    public void initialize() {
        exigir(ADOLESCENTE_CONSULTAR);
        boolean completo = podeSalvarCadastroCompleto();
        exibir(secaoProtegida, completo);
        secaoProtegida.setDisable(!completo);
        exibir(lblAvisoOperacional, !completo);
        cmbGenero.setItems(FXCollections.observableArrayList("Masculino", "Feminino", "Outro", "Não informado"));
        cmbCorRaca.setItems(FXCollections.observableArrayList("Branca", "Preta", "Parda", "Amarela", "Indígena"));

        cmbStatus.setDisable(true);
        cmbStatus.setItems(FXCollections.observableArrayList(StatusAdolescente.values()));
        cmbStatus.setValue(StatusAdolescente.ATIVO);
        cmbMedidaProtetiva.setItems(FXCollections.observableArrayList("Não", "Sim"));
        cmbMedidaProtetiva.setValue("Não");
        linhaEstudo.visibleProperty().bind(chkEstuda.selectedProperty());
        linhaEstudo.managedProperty().bind(linhaEstudo.visibleProperty());
        camposTrabalho.visibleProperty().bind(chkTrabalha.selectedProperty());
        camposTrabalho.managedProperty().bind(camposTrabalho.visibleProperty());
        camposSpa.visibleProperty().bind(chkSpa.selectedProperty());
        camposSpa.managedProperty().bind(camposSpa.visibleProperty());
        txtEscola.disableProperty().bind(chkEstuda.selectedProperty().not());
        txtSerie.disableProperty().bind(chkEstuda.selectedProperty().not());
        txtLocalTrabalho.disableProperty().bind(chkTrabalha.selectedProperty().not());
        txtFuncao.disableProperty().bind(chkTrabalha.selectedProperty().not());
        txtVinculo.disableProperty().bind(chkTrabalha.selectedProperty().not());
        txtSpa.disableProperty().bind(chkSpa.selectedProperty().not());
        dpNascimento.valueProperty().addListener((o,a,b) -> atualizarIdade());
        MascarasFormulario.cpf(txtCpf);
        MascarasFormulario.telefone(txtContato);
        DatasFormulario.configurarDigitacao(dpNascimento);
        ValidacaoFormulario.limitarTexto(txtNome,80,"Nome");
        ValidacaoFormulario.limitarTexto(txtEmail,90,"E-mail");
        ValidacaoFormulario.limitarTexto(txtNaturalidade,120,"Naturalidade");
        ValidacaoFormulario.limitarTexto(txtEscola,120,"Escola");
        ValidacaoFormulario.limitarTexto(txtSerie,40,"Ano ou série");
        ValidacaoFormulario.limitarTexto(txtLocalTrabalho,80,"Local de trabalho");
        ValidacaoFormulario.limitarTexto(txtFuncao,120,"Função");
        ValidacaoFormulario.limitarTexto(txtVinculo,80,"Vínculo empregatício");
        ValidacaoFormulario.limitarTexto(txtBairro,60,"Bairro");
        ValidacaoFormulario.limitarTexto(txtEndereco,60,"Endereço");
        ValidacaoFormulario.limitarTexto(txtCras,120,"CRAS");
        ValidacaoFormulario.limitarTexto(txtBeneficios,256,"Benefícios sociais");
        ValidacaoFormulario.limitarTexto(txtUbs,120,"UBS");
        ValidacaoFormulario.limitarTexto(txtSpa,90,"Substâncias utilizadas");
        ValidacaoFormulario.limitarDigitos(txtNis,11,"NIS");
        ValidacaoFormulario.limitarDecimal(txtRenda,false);
    }

    private boolean podeSalvarCadastroCompleto() {
        return pode(SOCIAL_ALTERAR) && pode(SAUDE_ALTERAR) && pode(PIA_ALTERAR)
                && pode(SIGILOSO_ALTERAR) && pode(EDUCACAO_ALTERAR);
    }

    private void exibir(Node componente, boolean visivel) {
        componente.setVisible(visivel);
        componente.setManaged(visivel);
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
        exigir(ADOLESCENTE_CONSULTAR);

        Adolescente a=adolescenteDAO.buscarPorCpf(cpf);
        if(a==null) throw new IllegalArgumentException("Cadastro não encontrado.");
        SituacaoSocial ss=pode(SOCIAL_CONSULTAR)?new SituacaoSocialDAO().buscarPorCpf(cpf):null;
        saudeAtual=pode(SAUDE_CONSULTAR)?new SaudeDAO().buscarPorCpf(cpf):null;
        EducacaoTrabalho et=new EducacaoTrabalhoDAO().buscarPorCpf(cpf);
        edicao=true; lblTitulo.setText("Editar cadastro"); txtCpf.setDisable(true);
        lblCpf.setText("CPF");
        txtCpf.setText(String.format("%011d",cpf)); txtNome.setText(a.getNomeCompleto());
        dpNascimento.setValue(a.getDataNascimento());
        contatoOriginal=a.getContato();
        MascarasFormulario.carregarTelefoneExistente(txtContato,contatoOriginal);
        emailOriginal=a.getEmail();
        txtEmail.setText(emailOriginal);
        txtNaturalidade.setText(a.getNaturalidade()); cmbGenero.setValue(a.getGenero()); cmbCorRaca.setValue(a.getCorRaca());
        situacaoOriginal=a.getStatus();
        cmbStatus.setValue(situacaoOriginal);
        lblSituacao.setText(situacaoOriginal==null ? "Não informada" : situacaoOriginal.getDescricao());
        boolean podeAlterarSituacao = pode(ADOLESCENTE_SITUACAO_ALTERAR);
        cmbStatus.setDisable(!podeAlterarSituacao);
        exibir(cmbStatus, podeAlterarSituacao);
        exibir(lblSituacao, !podeAlterarSituacao);
        txtObservacoes.setText(a.getObservacoes());
        chkImm.setSelected(a.isImm()); chkValeTransporte.setSelected(a.isValeTransporte());
        chkPiaEnviado.setSelected(a.isPiaEnviado());
        cmbMedidaProtetiva.setValue(a.isMedidaProtetiva() ? "Sim" : "Não");
        if(ss!=null) {
            txtEndereco.setText(ss.getEndereco()); txtBairro.setText(ss.getBairro());
            txtRenda.setText(Formatadores.decimal(ss.getRendaFamiliar())); txtBeneficios.setText(ss.getBeneficioSocial());
            ValidacaoFormulario.carregarExistente(txtNis,ss.getNumeroNis()==0 ? "" : Long.toString(ss.getNumeroNis()));
            txtCras.setText(ss.getCrasNome());
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
        try { exigir(edicao ? ADOLESCENTE_EDITAR : ADOLESCENTE_CADASTRAR); }
        catch(br.edu.unespar.trabalho.service.AcessoNegadoException negado) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING,negado.getMessage()).showAndWait();
            return;
        }

        try {
            dpNascimento.commitValue();

            String nome = ValidacaoFormulario.texto(txtNome.getText(),80,"Nome",true);
            if (dpNascimento.getValue()==null) throw new IllegalArgumentException("Informe a data de nascimento.");

            String cpf=txtCpf.getText().replaceAll("\\D","");
            if(!edicao && !MascarasFormulario.cpfValido(txtCpf.getText())) throw new IllegalArgumentException(MascarasFormulario.CPF_INVALIDO);
            if(dpNascimento.getValue().isAfter(LocalDate.now())) throw new IllegalArgumentException("Nascimento não pode estar no futuro.");

            String email=ValidacaoFormulario.email(txtEmail.getText(),emailOriginal);

            Adolescente a=new Adolescente();
            a.setCpf(Long.parseLong(cpf));
            a.setNomeCompleto(nome);
            a.setDataNascimento(dpNascimento.getValue());
            a.setContato(ValidacaoFormulario.texto(txtContato.getText(),30,"Telefone",true));
            if (!MascarasFormulario.telefoneValidoOuExistente(a.getContato(),contatoOriginal))
                throw new IllegalArgumentException(MascarasFormulario.TELEFONE_INVALIDO);
            a.setEmail(email);
            a.setNaturalidade(texto(txtNaturalidade,120,"Naturalidade"));
            a.setGenero(cmbGenero.getValue()==null ? "Não informado" : cmbGenero.getValue());
            a.setCorRaca(cmbCorRaca.getValue()==null ? "Não informada" : cmbCorRaca.getValue());
            boolean alterarSituacao = edicao && pode(ADOLESCENTE_SITUACAO_ALTERAR)
                    && cmbStatus.getValue()!=situacaoOriginal;
            a.setStatus(alterarSituacao ? cmbStatus.getValue() : StatusAdolescente.ATIVO);
            a.setImm(chkImm.isSelected());
            a.setValeTransporte(chkValeTransporte.isSelected());
            a.setMedidaProtetiva("Sim".equals(cmbMedidaProtetiva.getValue()));

            EducacaoTrabalho et=new EducacaoTrabalho();
            et.setEstuda(chkEstuda.isSelected());
            et.setEscola(et.isEstuda()?texto(txtEscola,120,"Escola"):null);
            et.setSerie(et.isEstuda()?texto(txtSerie,40,"Série"):null);
            et.setTrabalha(chkTrabalha.isSelected());
            et.setLocalTrabalho(et.isTrabalha()?texto(txtLocalTrabalho,80,"Local de trabalho"):null);
            et.setFuncao(et.isTrabalha()?texto(txtFuncao,120,"Função"):null);
            et.setVinculoEmpregaticio(et.isTrabalha()?texto(txtVinculo,80,"Vínculo empregatício"):null);

            if (podeSalvarCadastroCompleto()) {
                SituacaoSocial social = lerSituacaoSocial();
                Saude saude = saudeAtual==null ? new Saude() : saudeAtual;
                saude.setUbsReferencia(texto(txtUbs,120,"UBS"));
                saude.setUsoSpa(chkSpa.isSelected());
                saude.setSubstanciasUtilizadas(chkSpa.isSelected() ? texto(txtSpa,90,"Substâncias") : null);
                a.setObservacoes(txtObservacoes.getText()==null ? "" : txtObservacoes.getText().trim());
                a.setPiaEnviado(chkPiaEnviado.isSelected());
                adolescenteDAO.salvarCadastro(a,social,saude,et,edicao,alterarSituacao);
            } else {
                adolescenteDAO.salvarDadosOperacionais(a, et, edicao);
            }
            mostrarAlerta(Alert.AlertType.INFORMATION,"Cadastro salvo","Dados salvos com sucesso.");
            abrirPerfil(event,a.getCpf());

        } catch(NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING,"Valor inválido","Confira os campos numéricos. Renda é informada em salários mínimos, por exemplo: 1,5.");
        } catch(java.time.format.DateTimeParseException e) {

            mostrarAlerta(Alert.AlertType.WARNING,"Data inválida",DatasFormulario.DATA_INVALIDA);
        } catch(IllegalArgumentException e) {
            mostrarAlerta(Alert.AlertType.WARNING,"Confira os dados",e.getMessage());
        } catch(RuntimeException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR,"Erro ao salvar",e.getMessage());
        }
    }

    private SituacaoSocial lerSituacaoSocial() {
        SituacaoSocial social = new SituacaoSocial();
        social.setEndereco(texto(txtEndereco,60,"Endereço"));
        social.setBairro(texto(txtBairro,60,"Bairro"));
        social.setBeneficioSocial(texto(txtBeneficios,256,"Benefícios"));
        social.setCrasNome(texto(txtCras,120,"CRAS"));

        String renda = txtRenda.getText().trim();
        if (renda.isEmpty()) throw new IllegalArgumentException("Informe a renda familiar em salários mínimos.");
        social.setRendaFamiliar(Double.parseDouble(renda.replace(',','.')));
        social.validarCadastro();

        String nis = txtNis.getText().trim();
        if (!nis.isEmpty() && !nis.matches("[0-9]{1,11}"))
            throw new IllegalArgumentException("NIS: informe até 11 dígitos.");
        social.setNumeroNis(nis.isEmpty() ? 0 : Long.parseLong(nis));
        return social;
    }

    private String texto(TextField campo,int max,String nome) {
        return ValidacaoFormulario.texto(campo.getText(),max,nome,false);
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
