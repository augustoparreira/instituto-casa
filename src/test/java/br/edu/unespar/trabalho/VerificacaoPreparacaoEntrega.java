package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.controller.*;
import br.edu.unespar.trabalho.dao.*;
import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.util.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Cenário dirigido aos formulários e à propagação de alterações entre módulos. */
public class VerificacaoPreparacaoEntrega {
    private static final long CPF=12345678909L;
    private static int verificacoes;
    private static Throwable falhaDialogo;

    public static void main(String[] args) throws Exception {
        String url=System.getProperty("casa.db.url");
        if(url==null || url.contains("currentSchema=")) throw new IllegalArgumentException("Informe explicitamente o servidor de testes sem currentSchema.");
        String schema="casa_entrega_"+UUID.randomUUID().toString().replace("-","");
        try(Connection admin=ConnectionFactory.getConnection(); Statement sql=admin.createStatement()) {
            sql.execute("CREATE SCHEMA "+schema);
            try {
                System.setProperty("casa.db.url",url+"?currentSchema="+schema);
                try(Connection c=ConnectionFactory.getConnection(); Statement ddl=c.createStatement()) {
                    for(String arquivo:List.of("src/test/resources/schema_original.sql","sql/ajustes_cadastro_frequencia.sql","sql/agenda.sql","sql/relatorios_acompanhamento.sql"))
                        ddl.execute(Files.readString(Path.of(arquivo)));
                }
                Sessao.setUsuarioLogado(new EquipeTecnicaDTO(1L,"teste.entrega","Profissional de teste","Teste",NivelAcesso.ADMINISTRADOR));
                CompletableFuture<Void> fim=new CompletableFuture<>();
                Platform.startup(() -> {
                    Platform.setImplicitExit(false);
                    try { executar(); fim.complete(null); }
                    catch(Throwable t) { fim.completeExceptionally(t); }
                });
                try { fim.get(45,TimeUnit.SECONDS); } finally { Platform.exit(); }
            } finally {
                Sessao.limparSessao();
                System.setProperty("casa.db.url",url);
                sql.execute("DROP SCHEMA "+schema+" CASCADE");
            }
        }
        System.out.println("OK: "+verificacoes+" verificações dirigidas da preparação.");
    }

    private static FXMLLoader carregar(String nome) throws Exception {
        FXMLLoader loader=new FXMLLoader(VerificacaoPreparacaoEntrega.class.getResource("/View/"+nome+".fxml"));
        loader.load();
        return loader;
    }

    private static void executar() throws Exception {
        Stage janela=new Stage();
        try {
            FXMLLoader cadastro=carregar("CadastroAdolescenteView");
            Parent raiz=cadastro.getRoot();
            janela.setScene(new Scene(raiz,1280,850));
            raiz.applyCss(); raiz.layout();
            texto(raiz,"txtCpf").setText("12345678909");
            texto(raiz,"txtContato").setText("11987654321");
            texto(raiz,"txtNome").setText("Adolescente da entrega");
            texto(raiz,"txtEndereco").setText("Rua de teste");
            texto(raiz,"txtBairro").setText("Bairro de teste");
            DatePicker nascimento=(DatePicker)raiz.lookup("#dpNascimento");
            nascimento.setValue(LocalDate.of(2008,1,1));
            nascimento.getEditor().setText("31022008");
            boolean rejeitou=false;
            try { nascimento.commitValue(); } catch(java.time.DateTimeException e) { rejeitou=true; }
            verificar(rejeitou,"nascimento digitado inexistente é rejeitado");
            nascimento.getEditor().setText("29022008"); nascimento.commitValue();
            verificar(nascimento.getValue().equals(LocalDate.of(2008,2,29)),"máscara confirma nascimento bissexto");
            verificar(texto(raiz,"txtCpf").getText().equals("123.456.789-09")
                    && texto(raiz,"txtContato").getText().equals("(11) 98765-4321"),"máscaras preservam CPF e telefone colados");
            dialogo(ButtonType.OK,p -> {});
            ((CadastroAdolescenteController)cadastro.getController()).salvarAdolescente(new ActionEvent(raiz,raiz));
            verificar(new AdolescenteDAO().buscarPorCpf(CPF).getDataNascimento().equals(nascimento.getValue()),"cadastro pela tela persiste data confirmada");

            FXMLLoader perfil=carregar("DetalhesAdolescenteView");
            Parent perfilRaiz=perfil.getRoot();
            janela.setScene(new Scene(perfilRaiz,1280,850));
            perfilRaiz.applyCss(); perfilRaiz.layout();
            DetalhesAdolescenteController controller=perfil.getController();
            controller.carregarDados(new AdolescenteDAO().listarResumoDTO().getFirst());
            ApresentacaoAcesso.identificar(perfilRaiz);
            verificar(perfilRaiz.lookupAll(".user-name").stream().allMatch(n -> ((Label)n).getText().equals("Profissional de teste")),
                    "identificação operacional utiliza nome da sessão");

            Platform.runLater(() -> {
                try {
                    DialogPane pane=painelAberto();
                    GridPane grade=(GridPane)((VBox)pane.getContent()).getChildren().getFirst();
                    ((TextField)campo(grade,0)).setText("Responsável da entrega");
                    ((TextField)campo(grade,1)).setText("99999999901");
                    DatePicker data=(DatePicker)campo(grade,2);
                    data.setValue(LocalDate.of(1980,1,1));
                    data.getEditor().setText("31/02/1980");
                    ((TextField)campo(grade,3)).setText("Mãe");
                    botaoSalvar(pane).fire();
                    verificar(pane.getScene().getWindow().isShowing(),"data inválida mantém formulário de responsável aberto");
                    data.getEditor().setText("15/04/1980");
                    botaoSalvar(pane).fire();
                } catch(Throwable t) { falhaDialogo=t; fecharTudo(); }
            });
            controller.adicionarResponsavel();
            conferirDialogo();
            verificar(new ResponsavelDAO().listarResponsaveis(CPF).getFirst().getDataNascimento().equals(LocalDate.of(1980,4,15)),
                    "responsável persiste o texto da data corrigida");
            dialogo(ButtonType.CANCEL,p -> {
                GridPane grade=(GridPane)((VBox)p.getContent()).getChildren().getFirst();
                ((TextField)campo(grade,0)).setText("Familiar cancelado");
            });
            controller.adicionarFamiliar(); conferirDialogo();
            verificar(new ComposicaoFamiliarDAO().listarPorAdolescente(CPF).isEmpty(),"cancelar familiar não grava registro");
            Platform.runLater(() -> {
                try {
                    DialogPane p=painelAberto();
                    GridPane grade=(GridPane)((VBox)p.getContent()).getChildren().getFirst();
                    ((TextField)campo(grade,0)).setText("Familiar da entrega");
                    ((TextField)campo(grade,1)).setText("Irmão");
                    ((TextField)campo(grade,3)).setText("NaN");
                    botaoSalvar(p).fire();
                    verificar(p.getScene().getWindow().isShowing(),"renda não finita mantém formulário aberto");
                    ((TextField)campo(grade,3)).setText("1234,56"); botaoSalvar(p).fire();
                } catch(Throwable t) { falhaDialogo=t; fecharTudo(); }
            });
            controller.adicionarFamiliar(); conferirDialogo();

            MedidaSocioeducativa psc=medida(TipoMedida.PSC), la=medida(TipoMedida.LA);
            MedidaSocioeducativaDAO medidas=new MedidaSocioeducativaDAO();
            medidas.inserir(psc); medidas.inserir(la);
            Atividade atividade=new Atividade(); atividade.setNomeAtividade("Oficina da entrega"); atividade.setCargaHoraria(4);
            new AtividadeDAO().inserir(atividade);
            FXMLLoader registro=carregar("RegistroFrequenciaView");
            Parent frequenciaRaiz=registro.getRoot(); new Scene(frequenciaRaiz); frequenciaRaiz.applyCss(); frequenciaRaiz.layout();
            RegistroFrequenciaController frequencia=registro.getController();
            frequencia.carregar(CPF,LocalDate.now(),() -> {});
            ((ComboBox<?>)frequenciaRaiz.lookup("#cmbAtividade")).getSelectionModel().selectFirst();
            @SuppressWarnings("unchecked")
            ComboBox<MedidaSocioeducativa> escolha=(ComboBox<MedidaSocioeducativa>)frequenciaRaiz.lookup("#cmbMedida");
            escolha.setValue(escolha.getItems().stream().filter(MedidaSocioeducativa::isPSC).findFirst().orElseThrow());
            texto(frequenciaRaiz,"txtHoras").setText("4"); frequencia.salvar();
            ((ListView<?>)frequenciaRaiz.lookup("#listaRegistros")).getSelectionModel().selectFirst();
            texto(frequenciaRaiz,"txtHoras").setText("2"); frequencia.salvar();
            verificar(medidas.consultarHorasDaMedida(psc.getIdMedida(),LocalDate.now())==2,"correção na tela atualiza horas da PSC");
            DatePicker dataFrequencia=(DatePicker)frequenciaRaiz.lookup("#dpData");
            dataFrequencia.getEditor().setText("31/02/2026"); frequencia.salvar();
            verificar(((Label)frequenciaRaiz.lookup("#lblMensagem")).getText().equals("Informe uma data existente no formato DD/MM/AAAA."),
                    "frequência mostra mensagem compreensível para data inválida");
            dataFrequencia.getEditor().setText(dataFrequencia.getConverter().toString(LocalDate.now()));

            var mensal=new FrequenciaMensalDAO().listar(YearMonth.now()).getFirst();
            verificar(mensal.getMse().equals("LA/PSC") && mensal.getHorasPendentes()==8,"planilha reflete medidas e horas corrigidas");

            EventoAgenda evento=new EventoAgenda(); evento.setTitulo("Compromisso da entrega");
            evento.setTipo(EventoAgenda.Tipo.ATENDIMENTO); evento.setData(LocalDate.now().plusDays(1));
            evento.setHoraInicio(LocalTime.of(9,0)); evento.setHoraFim(LocalTime.of(10,0)); evento.setCpfAdolescente(CPF);
            EventoAgendaDAO agenda=new EventoAgendaDAO(); agenda.inserir(evento); evento.setTitulo("Compromisso atualizado"); agenda.atualizar(evento);
            verificar(agenda.listar(evento.getData(),evento.getData()).getFirst().getTitulo().equals("Compromisso atualizado"),"agenda reabre compromisso atualizado");
            ((CheckBox)perfilRaiz.lookup("#chkEnvioPia")).setSelected(true);
            dialogo(ButtonType.OK,p -> {}); controller.salvarConfirmacaoPia();
            verificar(new PIADAO().consultarEnvio(CPF),"envio do PIA persiste pela tela");

            FXMLLoader relatorio=carregar("RelatoriosView");
            Parent relatorioRaiz=relatorio.getRoot(); janela.setScene(new Scene(relatorioRaiz,1280,850));
            relatorioRaiz.applyCss(); relatorioRaiz.layout();
            ((ComboBox<?>)relatorioRaiz.lookup("#cmbAdolescente")).getSelectionModel().selectFirst();
            RelatoriosController relatorios=relatorio.getController(); relatorios.gerarNovoRelatorio();
            ((TextArea)relatorioRaiz.lookup("#txtDescumprimento")).setText("Sem descumprimento no cenário fictício.");
            relatorios.salvarAtualizacaoRelatorio();
            var salvo=new RelatorioAcompanhamentoDAO().listar().getFirst();
            verificar(salvo.getCampo(RelatorioAcompanhamento.Campo.HORAS).contains("2h cumpridas no mês"),
                    "relatório recebe horas corrigidas em outro módulo");
            FXMLLoader reaberto=carregar("RelatoriosView"); Parent reabertoRaiz=reaberto.getRoot();
            janela.setScene(new Scene(reabertoRaiz,1280,850));
            reabertoRaiz.applyCss(); reabertoRaiz.layout();
            ((ListView<?>)reabertoRaiz.lookup("#listRelatorios")).getSelectionModel().selectFirst();
            verificar(((TextArea)reabertoRaiz.lookup("#txtDescumprimento")).getText().equals(salvo.getDescumprimento()),"reabertura do rascunho preserva texto revisado");
            DatePicker competencia=(DatePicker)reabertoRaiz.lookup("#dpCompetencia");
            competencia.getEditor().setText("31/02/2026");
            ((RelatoriosController)reaberto.getController()).gerarNovoRelatorio();
            verificar(((Label)reabertoRaiz.lookup("#lblMensagem")).getText().equals("Informe datas existentes no formato DD/MM/AAAA.")
                    && ((TextArea)reabertoRaiz.lookup("#txtDescumprimento")).getText().equals(salvo.getDescumprimento()),
                    "competência inválida preserva rascunho aberto e orienta usuário");
            competencia.getEditor().setText(competencia.getConverter().toString(competencia.getValue()));
            dialogo(ButtonType.CLOSE,p -> verificar(p.getContent() instanceof ScrollPane,"prévia de impressão abre no fluxo integrado"));
            ((RelatoriosController)reaberto.getController()).visualizarRelatorio(); conferirDialogo();
        } finally { janela.close(); }
    }

    private static MedidaSocioeducativa medida(TipoMedida tipo) {
        var m=new MedidaSocioeducativa(); m.setCpfAdolescente(CPF); m.setTipoMedida(tipo); m.setDataInicio(LocalDate.now().minusMonths(2));
        if(tipo==TipoMedida.PSC) m.setDuracaoHoras(10); else m.setDuracaoMeses(6);
        return m;
    }

    private static TextField texto(Parent p,String id) { return (TextField)p.lookup("#"+id); }
    private static Node campo(GridPane grade,int row) {
        return grade.getChildren().stream().filter(n -> Objects.equals(GridPane.getRowIndex(n),row)
                && Objects.equals(GridPane.getColumnIndex(n),1)).findFirst().orElseThrow();
    }
    private static Button botaoSalvar(DialogPane p) {
        return (Button)p.lookupButton(p.getButtonTypes().stream()
                .filter(t -> t.getButtonData()==ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow());
    }
    private static DialogPane painelAberto() {
        return Window.getWindows().stream().filter(Window::isShowing).map(w -> w.getScene().getRoot())
                .map(p -> p.lookup(".dialog-pane")).filter(DialogPane.class::isInstance).map(DialogPane.class::cast).findFirst().orElseThrow();
    }
    private static void dialogo(ButtonType resposta,Consumer<DialogPane> verificar) {
        Platform.runLater(() -> {
            try {
                DialogPane p=painelAberto(); verificar.accept(p); ((Button)p.lookupButton(resposta)).fire();
            } catch(Throwable t) { falhaDialogo=t; fecharTudo(); }
        });
    }
    private static void fecharTudo() { for(Window w:List.copyOf(Window.getWindows())) if(w.isShowing()) w.hide(); }
    private static void conferirDialogo() { if(falhaDialogo!=null) throw new AssertionError("Falha no diálogo",falhaDialogo); }
    private static void verificar(boolean ok,String mensagem) {
        if(!ok) throw new AssertionError(mensagem);
        verificacoes++; System.out.println("OK: "+mensagem);
    }
}
