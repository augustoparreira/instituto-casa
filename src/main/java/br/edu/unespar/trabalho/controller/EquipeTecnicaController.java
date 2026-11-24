package br.edu.unespar.trabalho.controller;

import br.edu.unespar.trabalho.dao.EquipeTecnicaDAO;
import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.service.AutorizacaoService;
import br.edu.unespar.trabalho.util.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import java.util.List;
import java.util.Locale;
import static br.edu.unespar.trabalho.model.Operacao.*;

/** Administração de profissionais e contas, preservando vínculos ao desativar. */
public class EquipeTecnicaController {
    @FXML private TextField txtPesquisa;
    @FXML private Label lblTotalRegistros;
    @FXML private ListView<EquipeTecnica> listMembros;
    @FXML private Button btnCargoTodos,btnCargoAssistente,btnCargoPsicologo,btnCargoPedagogo;
    private final EquipeTecnicaDAO dao=new EquipeTecnicaDAO();
    private List<EquipeTecnica> membros=List.of();
    private String cargo="";

    @FXML public void initialize() {
        AutorizacaoService.exigir(USUARIO_GERENCIAR);
        listMembros.setPlaceholder(new Label("Nenhum profissional cadastrado."));
        listMembros.setCellFactory(lista -> new ListCell<>() {
            @Override protected void updateItem(EquipeTecnica m,boolean vazio) {
                super.updateItem(m,vazio); setText(null); setGraphic(null);
                if(vazio || m==null) return;
                VBox dados=new VBox(4,new Label(m.getNomeCompleto()),new Label(m.getCargoFuncao()+" · "+m.getLogin()),
                        new Label(m.getNivelAcesso()+" · "+(m.isAtivo()?"Conta ativa":"Conta desativada")));
                Region espaco=new Region(); HBox.setHgrow(espaco,Priority.ALWAYS);
                Button editar=new Button("Editar"),senha=new Button("Redefinir senha"),ativo=new Button(m.isAtivo()?"Desativar":"Reativar");
                editar.setOnAction(e -> abrirFormulario(m)); senha.setOnAction(e -> redefinirSenha(m));
                ativo.setOnAction(e -> alterarAtivo(m));
                HBox linha=new HBox(12,dados,espaco,editar,senha,ativo); linha.setPadding(new Insets(12)); setGraphic(linha);
            }
        });
        atualizar();
    }

    private void atualizar() {
        membros=dao.listar(); filtrarMembros();
    }

    @FXML public void filtrarMembros() {
        AutorizacaoService.exigir(EQUIPE_CONSULTAR);
        String termo=txtPesquisa.getText()==null?"":txtPesquisa.getText().trim().toLowerCase(Locale.ROOT);
        var resultado=membros.stream().filter(m -> (m.getNomeCompleto()+" "+m.getCargoFuncao()+" "+m.getLogin())
                .toLowerCase(Locale.ROOT).contains(termo) && m.getCargoFuncao().toLowerCase(Locale.ROOT).contains(cargo)).toList();
        listMembros.setItems(FXCollections.observableArrayList(resultado)); lblTotalRegistros.setText(resultado.size()+" profissional(is)");
    }
    private void filtrarCargo(String valor,Button ativo) {
        cargo=valor;
        for(Button b:List.of(btnCargoTodos,btnCargoAssistente,btnCargoPsicologo,btnCargoPedagogo)) {
            b.getStyleClass().removeAll("filter-btn","filter-btn-active"); b.getStyleClass().add(b==ativo?"filter-btn-active":"filter-btn");
        }
        filtrarMembros();
    }
    @FXML public void filtrarCargoTodos(ActionEvent e) { filtrarCargo("",btnCargoTodos); }
    @FXML public void filtrarCargoAssistente(ActionEvent e) { filtrarCargo("assistente",btnCargoAssistente); }
    @FXML public void filtrarCargoPsicologo(ActionEvent e) { filtrarCargo("psic",btnCargoPsicologo); }
    @FXML public void filtrarCargoPedagogo(ActionEvent e) { filtrarCargo("pedagog",btnCargoPedagogo); }
    @FXML public void novoMembro() { abrirFormulario(null); }

    private void abrirFormulario(EquipeTecnica existente) {
        AutorizacaoService.exigir(USUARIO_GERENCIAR);
        Dialog<ButtonType> dialogo=new Dialog<>(); dialogo.setTitle(existente==null?"Cadastrar profissional e conta":"Editar profissional e conta");
        dialogo.initOwner(listMembros.getScene().getWindow());
        GridPane grade=new GridPane(); grade.setHgap(14); grade.setVgap(10); grade.setPadding(new Insets(20));
        TextField cpf=new TextField(),nome=new TextField(),contato=new TextField(),email=new TextField(),login=new TextField(),funcao=new TextField();
        DatePicker nascimento=new DatePicker(); ComboBox<NivelAcesso> perfil=new ComboBox<>(FXCollections.observableArrayList(NivelAcesso.values()));
        CheckBox ativo=new CheckBox("Conta ativa"); ativo.setSelected(true);
        PasswordField senha=new PasswordField(),confirmar=new PasswordField();
        Label erro=new Label(); erro.setWrapText(true); erro.setMaxWidth(480); erro.setStyle("-fx-text-fill: #a02020;");
        String[] rotulos={"CPF (11 dígitos)","Nome completo","Nascimento","Telefone (até 15 caracteres)","E-mail","Cargo / função","Login","Perfil"};
        javafx.scene.Node[] campos={cpf,nome,nascimento,contato,email,funcao,login,perfil};
        for(int i=0;i<campos.length;i++) grade.addRow(i,new Label(rotulos[i]),campos[i]);
        grade.add(ativo,1,8);
        if(existente==null) { grade.addRow(9,new Label("Senha (12 a 128 caracteres)"),senha); grade.addRow(10,new Label("Confirme a senha"),confirmar); }
        else {
            cpf.setText(String.format("%011d",existente.getCpf())); cpf.setDisable(true); nome.setText(existente.getNomeCompleto());
            nascimento.setValue(existente.getDataNascimento()); contato.setText(existente.getContato()); email.setText(existente.getEmail());
            funcao.setText(existente.getCargoFuncao()); login.setText(existente.getLogin()); perfil.setValue(existente.getNivelAcesso()); ativo.setSelected(existente.isAtivo());
        }
        grade.add(erro,0,11,2,1); dialogo.getDialogPane().setContent(grade);
        ButtonType salvar=new ButtonType("Salvar",ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(salvar,ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(salvar).addEventFilter(ActionEvent.ACTION,event -> {
            try {
                AutorizacaoService.exigir(USUARIO_GERENCIAR);
                if(!cpf.getText().matches("[0-9]{11}")) throw new IllegalArgumentException("Informe os 11 dígitos do CPF.");
                nascimento.commitValue();
                EquipeTecnica m=new EquipeTecnica(); m.setCpf(Long.parseLong(cpf.getText())); m.setNomeCompleto(nome.getText());
                m.setDataNascimento(nascimento.getValue()); m.setContato(contato.getText()); m.setEmail(email.getText());
                m.setCargoFuncao(funcao.getText()); m.setLogin(login.getText()); m.setNivelAcesso(perfil.getValue()); m.setAtivo(ativo.isSelected());
                if(existente==null) {
                    if(!senha.getText().equals(confirmar.getText())) throw new IllegalArgumentException("As senhas não coincidem.");
                    m.setSenha(senha.getText());
                }
                try { if(existente==null) dao.inserir(m); else dao.atualizar(m); }
                finally { m.setSenha(null); }
                if(Sessao.validarContaAtual()) atualizar();
            } catch(RuntimeException ex) { erro.setText(ex.getMessage()); event.consume(); }
        });
        dialogo.showAndWait(); senha.clear(); confirmar.clear();
    }

    private void alterarAtivo(EquipeTecnica m) {
        AutorizacaoService.exigir(USUARIO_GERENCIAR);
        if(new Alert(Alert.AlertType.CONFIRMATION,(m.isAtivo()?"Desativar":"Reativar")+" esta conta? Os vínculos serão preservados.",ButtonType.YES,ButtonType.NO)
                .showAndWait().orElse(ButtonType.NO)!=ButtonType.YES) return;
        try { dao.definirAtivo(m.getCpf(),!m.isAtivo()); if(Sessao.validarContaAtual()) atualizar(); }
        catch(RuntimeException ex) { new Alert(Alert.AlertType.WARNING,ex.getMessage()).showAndWait(); }
    }

    private void redefinirSenha(EquipeTecnica m) {
        AutorizacaoService.exigir(USUARIO_GERENCIAR);
        Dialog<ButtonType> dialogo=new Dialog<>(); dialogo.setTitle("Redefinir senha"); dialogo.initOwner(listMembros.getScene().getWindow());
        PasswordField senha=new PasswordField(),confirmar=new PasswordField(); Label erro=new Label();
        dialogo.getDialogPane().setContent(new VBox(10,new Label("Nova senha (12 a 128 caracteres)"),senha,new Label("Confirmação"),confirmar,erro));
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION,event -> {
            try {
                if(!senha.getText().equals(confirmar.getText())) throw new IllegalArgumentException("As senhas não coincidem.");
                dao.redefinirSenha(m.getCpf(),senha.getText());
            } catch(RuntimeException ex) { erro.setText(ex.getMessage()); event.consume(); }
        });
        dialogo.showAndWait(); senha.clear(); confirmar.clear();
    }
    @FXML public void irParaPainel(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/Dashboard.fxml","Painel"); }
    @FXML public void irParaDashboard(ActionEvent e) { irParaPainel(e); }
    @FXML public void irParaAdolescentes(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/AdolescentesView.fxml","Adolescentes"); }
    @FXML public void irParaAgenda(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/AgendaView.fxml","Agenda"); }
    @FXML public void irParaRelatorios(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/RelatoriosView.fxml","Relatórios"); }
    @FXML public void irParaEquipeTecnica(ActionEvent e) { }
    @FXML public void fazerLogout(ActionEvent e) { NavegacaoUtil.mudarTela(e,"/View/Login.fxml","Login"); }
}
