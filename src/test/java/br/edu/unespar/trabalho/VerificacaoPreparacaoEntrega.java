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
                    for(String arquivo:List.of("src/test/resources/schema_original.sql","sql/ajustes_cadastro_frequencia.sql","sql/agenda.sql","sql/relatorios_acompanhamento.sql","sql/usuarios_seguranca.sql"))
                        ddl.execute(Files.readString(Path.of(arquivo)));
                }
                var adminTeste = VerificacaoContas.conta(85555555551L, "teste.entrega", NivelAcesso.ADMINISTRADOR);
                adminTeste.setNomeCompleto("Profissional de teste");
                new EquipeTecnicaDAO().criarPrimeiroAdministrador(adminTeste);
                VerificacaoContas.entrar(adminTeste.getLogin(), VerificacaoContas.SENHA);
                CompletableFuture<Void> fim=new CompletableFuture<>();
                Platform.startup(() -> Platform.setImplicitExit(false));
                Platform.runLater(() -> {
                    try { executar(); fim.complete(null); }
                    catch(Throwable t) { fim.completeExceptionally(t); }
                });
                try { fim.get(120,TimeUnit.SECONDS); } finally { Platform.exit(); }
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
            verificarMascaras();
            FXMLLoader cadastro=carregar("CadastroAdolescenteView");
            Parent raiz=cadastro.getRoot();
            janela.setScene(new Scene(raiz,1280,850));
            raiz.applyCss(); raiz.layout();
            texto(raiz,"txtCpf").setText("12345678909");
            texto(raiz,"txtContato").setText("11987654321");
            var nomeCadastro = texto(raiz,"txtNome");
            nomeCadastro.setText("Á".repeat(80));
            nomeCadastro.appendText("x");
            verificar(nomeCadastro.getText().equals("Á".repeat(80)), "cadastro aplica o limite de 80 ao campo Nome");
            nomeCadastro.setText("Adolescente da entrega");
            var nisCadastro = texto(raiz,"txtNis");
            nisCadastro.setText("12345678901");
            nisCadastro.appendText("2");
            verificar(nisCadastro.getText().equals("12345678901"), "cadastro aplica o limite de 11 dígitos ao NIS");
            nisCadastro.clear();
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
            var formulario = (CadastroAdolescenteController)cadastro.getController();
            verificar(raiz.lookup("#secaoProtegida").isVisible() && !raiz.lookup("#cmbStatus").isVisible()
                    && ((Label)raiz.lookup("#lblSituacao")).getText().equals("Ativo"),
                    "inclusão administrativa mostra dados protegidos e situação inicial fixa");
            verificar(texto(raiz,"txtRenda").getText().isBlank()
                    && raiz.lookupAll(".form-label-mini").stream().filter(Label.class::isInstance).map(Label.class::cast)
                    .anyMatch(l -> l.getText().startsWith("RENDA FAMILIAR") && l.getText().endsWith("*")),
                    "renda começa em branco e está marcada como obrigatória no fluxo completo");
            dialogo(ButtonType.OK,p -> verificar(p.lookupAll(".label").stream().filter(Label.class::isInstance)
                    .map(Label.class::cast).anyMatch(l -> l.getText()!=null && l.getText().contains("Informe a renda familiar")),
                    "formulário completo exige renda antes de salvar"));
            formulario.salvarAdolescente(new ActionEvent(raiz,raiz)); conferirDialogo();
            verificar(new AdolescenteDAO().buscarPorCpf(CPF)==null,"renda vazia não cria cadastro parcial");
            texto(raiz,"txtRenda").setText("1,25");
            texto(raiz,"txtContato").setText("+55 (11) 98765-4321");
            dialogo(ButtonType.OK,p -> {});
            formulario.salvarAdolescente(new ActionEvent(raiz,raiz));
            var salvoCadastro = new AdolescenteDAO().buscarPorCpf(CPF);
            verificar(salvoCadastro.getDataNascimento().equals(nascimento.getValue())
                    && salvoCadastro.getStatus()==StatusAdolescente.ATIVO
                    && salvoCadastro.getContato().equals("+55 (11) 98765-4321")
                    && new SituacaoSocialDAO().buscarPorCpf(CPF).getRendaFamiliar()==1.25,
                    "cadastro completo pela tela persiste data, renda declarada e situação Ativo");

            String contatoLegado = "3033-2023";
            try (var conexao = ConnectionFactory.getConnection();
                 var comando = conexao.prepareStatement("UPDATE Pessoa SET contato=? WHERE cpf=?")) {
                comando.setString(1,contatoLegado);
                comando.setLong(2,CPF);
                comando.executeUpdate();
            }
            FXMLLoader edicao = carregar("CadastroAdolescenteView");
            Parent raizEdicao = edicao.getRoot();
            janela.setScene(new Scene(raizEdicao,1280,850));
            raizEdicao.applyCss(); raizEdicao.layout();
            var formularioEdicao = (CadastroAdolescenteController)edicao.getController();
            formularioEdicao.carregarParaEdicao(CPF);
            verificar(texto(raizEdicao,"txtContato").getText().equals(contatoLegado),
                    "edição mantém telefone legado sem DDD visível");
            texto(raizEdicao,"txtNome").setText("Adolescente da entrega revisado");
            dialogo(ButtonType.OK,p -> {});
            formularioEdicao.salvarAdolescente(new ActionEvent(raizEdicao,raizEdicao)); conferirDialogo();
            var revisado = new AdolescenteDAO().buscarPorCpf(CPF);
            verificar(revisado.getNomeCompleto().equals("Adolescente da entrega revisado")
                    && revisado.getContato().equals(contatoLegado),
                    "edição de outro campo preserva telefone legado");
            revisado.setContato("3033-2024");
            try {
                new AdolescenteDAO().salvarDadosOperacionais(revisado,new EducacaoTrabalhoDAO().buscarPorCpf(CPF));
                throw new AssertionError("Telefone novo sem DDD foi salvo.");
            } catch (IllegalArgumentException esperado) {
                verificar(new AdolescenteDAO().buscarPorCpf(CPF).getContato().equals(contatoLegado),
                        "DAO recusa novo contato sem DDD e preserva o legado");
            }

            FXMLLoader perfil=carregar("DetalhesAdolescenteView");
            Parent perfilRaiz=perfil.getRoot();
            janela.setScene(new Scene(perfilRaiz,1280,850));
            perfilRaiz.applyCss(); perfilRaiz.layout();
            DetalhesAdolescenteController controller=perfil.getController();
            controller.carregarDados(new AdolescenteDAO().listarResumoDTO().getFirst());
            SegurancaInterface.aplicar(perfilRaiz);
            verificar(((Label)perfilRaiz.lookup("#lblInfoNascimento")).getText().equals("29/02/2008")
                    && ((Label)perfilRaiz.lookup("#lblInfoAniversario")).getText().equals("29/02")
                    && ((Label)perfilRaiz.lookup("#lblInfoNome")).isWrapText()
                    && ((Label)perfilRaiz.lookup("#lblInfoBairro")).isWrapText(),
                    "perfil distingue nascimento de aniversário e permite quebra dos dados longos");
            Label nomeCabecalho = (Label)perfilRaiz.lookup("#lblNome");
            Label bairroPerfil = (Label)perfilRaiz.lookup("#lblInfoBairro");
            nomeCabecalho.setText("Adolescente com nome comprido para conferência do cadastro de apresentação");
            bairroPerfil.setText("Bairro histórico da comunidade do centro de apoio social");
            ((Region)perfilRaiz).resize(850,760);
            perfilRaiz.layout();
            verificar(nomeCabecalho.getHeight()>nomeCabecalho.prefHeight(1500)
                    && bairroPerfil.getHeight()>bairroPerfil.prefHeight(1500),
                    "nome e bairro ocupam mais de uma linha no perfil estreito: "
                            + nomeCabecalho.getWidth()+"x"+nomeCabecalho.getHeight()+" / "
                            + bairroPerfil.getWidth()+"x"+bairroPerfil.getHeight());
            verificar(perfilRaiz.lookupAll(".user-name").stream().allMatch(n -> ((Label)n).getText().equals("Profissional de teste")),
                    "identificação operacional utiliza nome da sessão");

            Platform.runLater(() -> {
                try {
                    DialogPane pane=painelAberto();
                    GridPane grade=(GridPane)((VBox)pane.getContent()).getChildren().getFirst();
                    ((TextField)campo(grade,0)).setText("Responsável da entrega");
                    ((TextField)campo(grade,1)).setText("52998224725");
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
                    ((TextField)campo(grade,3)).setText("9".repeat(310));
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
            ((Button)perfilRaiz.lookup("#btnAbaPia")).fire();
            ((CheckBox)perfilRaiz.lookup("#chkEnvioPia")).setSelected(true);
            dialogo(ButtonType.OK,p -> {}); controller.salvarConfirmacaoPia();
            verificar(new PIADAO().consultarEnvio(CPF),"envio do PIA persiste pela tela");

            FXMLLoader relatorio=carregar("RelatoriosView");
            Parent relatorioRaiz=relatorio.getRoot(); janela.setScene(new Scene(relatorioRaiz,1280,850));
            relatorioRaiz.applyCss(); relatorioRaiz.layout();
            ((ComboBox<?>)relatorioRaiz.lookup("#cmbAdolescente")).getSelectionModel().selectFirst();
            RelatoriosController relatorios=relatorio.getController(); relatorios.gerarNovoRelatorio();
            var frequenciaRelatorio = (TextArea)relatorioRaiz.lookup("#txtRegistroFrequencia");
            var descumprimento = (TextArea)relatorioRaiz.lookup("#txtDescumprimento");
            String textoFrequencia = frequenciaRelatorio.getText();
            var nomeRelatorio = (TextField)relatorioRaiz.lookup("#campoNOME");
            var dataRelatorio = (DatePicker)relatorioRaiz.lookup("#dpEmissao");
            String nomeAnterior = nomeRelatorio.getText();
            LocalDate dataAnterior = dataRelatorio.getValue();
            nomeRelatorio.clear();
            dataRelatorio.setValue(null);
            frequenciaRelatorio.clear();
            relatorios.imprimirRelatorio();
            String mensagem = ((Label)relatorioRaiz.lookup("#lblMensagem")).getText();
            var pendente = javafx.css.PseudoClass.getPseudoClass("pendente");
            var processo = (TextField)relatorioRaiz.lookup("#campoPROCESSO");
            verificar(mensagem.contains("nome do adolescente") && mensagem.contains("data de emissão")
                    && nomeRelatorio.getPseudoClassStates().contains(pendente)
                    && dataRelatorio.getPseudoClassStates().contains(pendente)
                    && mensagem.contains("número do processo") && mensagem.contains("registro de frequência")
                    && mensagem.contains("seção de descumprimento") && processo.getPseudoClassStates().contains(pendente)
                    && frequenciaRelatorio.getPseudoClassStates().contains(pendente) && descumprimento.getPseudoClassStates().contains(pendente),
                    "impressão identifica e destaca todos os campos vazios de emissão");
            nomeRelatorio.setText(nomeAnterior);
            dataRelatorio.setValue(dataAnterior);
            frequenciaRelatorio.setText(textoFrequencia);
            descumprimento.setText("Sem descumprimento no cenário fictício.");
            processo.setText("Processo fictício 001");
            Path temporario = Files.createTempFile("exportacao-casa-",".txt");
            try {
                relatorios.exportarPara(temporario);
                verificar(Files.readString(temporario).contains("Processo fictício 001")
                        && !processo.getPseudoClassStates().contains(pendente), "exportação funciona após preencher as pendências");
            } finally { Files.deleteIfExists(temporario); }
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

            FXMLLoader listaLoader = carregar("AdolescentesView");
            Parent listaRaiz = listaLoader.getRoot();
            janela.setScene(new Scene(listaRaiz,1280,850));
            var listaAdolescentes = (ListView<?>)listaRaiz.lookup("#listAdolescentes");
            verificar(listaAdolescentes.getItems().isEmpty()
                    && ((Label)listaRaiz.lookup("#lblTotalRegistros")).getText().startsWith("Carregando"),
                    "lista apresenta a tela antes de concluir a consulta JDBC");
            aguardarInterface(() -> !listaAdolescentes.getItems().isEmpty());
            listaAdolescentes.getSelectionModel().selectFirst();
            ((AdolescentesController)listaLoader.getController()).clicarAdolescente(null);
            Parent perfilAberto = janela.getScene().getRoot();
            Label carregamento = (Label)perfilAberto.lookup("#lblCarregamentoPerfil");
            verificar(carregamento.isVisible() && carregamento.getText().startsWith("Carregando")
                    && ((Label)perfilAberto.lookup("#lblNome")).getText().equals(revisado.getNomeCompleto()),
                    "clique abre o perfil com cabeçalho antes de concluir a consulta pessoal");
            aguardarInterface(() -> !carregamento.isVisible());
            perfilAberto.applyCss();
            perfilAberto.layout();
            verificar(((Label)perfilAberto.lookup("#lblInfoAniversario")).getText().equals("29/02"),
                    "consulta em segundo plano entrega dados à tela do perfil");
            verificar(((ListView<?>)perfilAberto.lookup("#listaFrequenciaPerfil")).getItems().isEmpty(),
                    "abrir o perfil não consulta nem preenche a aba de frequência oculta");
            medirAberturaFrequencia(perfilAberto);

            FXMLLoader painelLoader = carregar("Dashboard");
            Parent painel = painelLoader.getRoot();
            janela.getScene().setRoot(painel);
            ((DashboardController)painelLoader.getController()).irParaAdolescentes(new ActionEvent(painel,painel));
            Parent listaDoPainel = janela.getScene().getRoot();
            listaDoPainel.applyCss();
            listaDoPainel.layout();
            var linhasImediatas = (ListView<?>)listaDoPainel.lookup("#listAdolescentes");
            verificar(linhasImediatas!=null && !linhasImediatas.getItems().isEmpty(),
                    "painel abre a lista com os adolescentes já na primeira renderização");
            verificarRespostaObsoletaFrequencia(janela);
            verificarFechamentoRelatorios(janela);
        } finally { janela.close(); }
    }

    private static void verificarFechamentoRelatorios(Stage janela) throws Exception {
        FXMLLoader relatorio=carregar("RelatoriosView");
        Parent raiz=relatorio.getRoot();
        janela.setScene(new Scene(raiz,1280,850));
        raiz.applyCss(); raiz.layout();
        TextArea rascunho=(TextArea)raiz.lookup("#txtDescumprimento");
        rascunho.setText("Rascunho fictício ainda não salvo");

        dialogo(ButtonType.NO,p -> verificar(p.getContentText().contains("alterações não salvas"),
                "X e Alt+F4 avisam sobre o rascunho pendente"));
        WindowEvent cancelar=new WindowEvent(janela,WindowEvent.WINDOW_CLOSE_REQUEST);
        janela.getOnCloseRequest().handle(cancelar); conferirDialogo();
        verificar(cancelar.isConsumed() && janela.getScene().getRoot()==raiz
                && rascunho.getText().equals("Rascunho fictício ainda não salvo"),
                "cancelar o X mantém a janela e preserva o rascunho");

        dialogo(ButtonType.YES,p -> {});
        WindowEvent confirmar=new WindowEvent(janela,WindowEvent.WINDOW_CLOSE_REQUEST);
        janela.getOnCloseRequest().handle(confirmar); conferirDialogo();
        verificar(!confirmar.isConsumed(),"confirmar o descarte permite fechar a janela");

        Parent outraTela=new StackPane();
        janela.getScene().setRoot(outraTela);
        WindowEvent foraDosRelatorios=new WindowEvent(janela,WindowEvent.WINDOW_CLOSE_REQUEST);
        janela.getOnCloseRequest().handle(foraDosRelatorios);
        verificar(!foraDosRelatorios.isConsumed(),"outra tela não herda o aviso de Relatórios");

        FXMLLoader limpo=carregar("RelatoriosView");
        Parent raizLimpa=limpo.getRoot();
        janela.setScene(new Scene(raizLimpa,1280,850));
        raizLimpa.applyCss(); raizLimpa.layout();
        WindowEvent semAlteracoes=new WindowEvent(janela,WindowEvent.WINDOW_CLOSE_REQUEST);
        janela.getOnCloseRequest().handle(semAlteracoes);
        verificar(!semAlteracoes.isConsumed(),"janela de Relatórios sem alterações fecha normalmente");

        ((TextArea)raizLimpa.lookup("#txtDescumprimento")).setText("Outro rascunho fictício");
        Sessao.limparSessao();
        WindowEvent sessaoEncerrada=new WindowEvent(janela,WindowEvent.WINDOW_CLOSE_REQUEST);
        janela.getOnCloseRequest().handle(sessaoEncerrada);
        verificar(!sessaoEncerrada.isConsumed(),"sessão inválida não abre aviso de rascunho");

        VerificacaoContas.entrar("teste.entrega",VerificacaoContas.SENHA);
        Sessao.configurarInatividade(Duration.ofNanos(1));
        try {
            WindowEvent sessaoExpirada=new WindowEvent(janela,WindowEvent.WINDOW_CLOSE_REQUEST);
            janela.getOnCloseRequest().handle(sessaoExpirada);
            verificar(!sessaoExpirada.isConsumed() && Sessao.getUsuarioLogado()==null,
                    "sessão expirada não bloqueia o fechamento por rascunho");
        } finally { Sessao.configurarInatividade(Duration.ofMinutes(15)); }
    }


    private static void medirAberturaFrequencia(Parent perfil) {
        var mes = YearMonth.from(((DatePicker)perfil.lookup("#dpMesFrequencia")).getValue());
        var abrir = (Button)perfil.lookup("#btnAbaFrequencia");
        var dados = (Button)perfil.lookup("#btnAbaDados");
        var conteudo = perfil.lookup("#conteudoFrequencia");
        var aviso = (Label)perfil.lookup("#lblCarregamentoFrequencia");
        var lista = (ListView<?>)perfil.lookup("#listaFrequenciaPerfil");
        long inicio = System.nanoTime();
        abrir.fire();
        double troca = (System.nanoTime()-inicio)/1_000_000.0;
        perfil.applyCss();
        perfil.layout();
        double layout = (System.nanoTime()-inicio)/1_000_000.0;
        verificar(perfil.lookup("#paneFrequencia").isVisible() && aviso.isVisible()
                        && conteudo.isDisabled() && !conteudo.isVisible(),
                "primeira abertura mostra frequência e bloqueia ações antes de concluir a consulta");
        dados.fire();
        verificar(perfil.lookup("#paneDados").isVisible(),"pode sair da frequência durante o carregamento");
        abrir.fire();
        verificar(aviso.isVisible() && !conteudo.isVisible(),"retornar durante a consulta mantém o carregamento pendente");

        aguardarInterface(conteudo::isVisible);
        double pronta = (System.nanoTime()-inicio)/1_000_000.0;
        var registros = lista.getItems();
        verificar(!aviso.isVisible() && !conteudo.isDisabled() && !registros.isEmpty(),
                "frequência libera os controles com os registros carregados");
        dados.fire();
        abrir.fire();
        verificar(!aviso.isVisible() && lista.getItems()==registros,
                "retornar à frequência reutiliza os registros carregados");

        var tempo = new CompletableFuture<Double>();
        ConsultaInterface.iniciar(perfil,() -> medirConsultasFrequencia(mes),tempo::complete,tempo::completeExceptionally);
        aguardarInterface(tempo::isDone);
        System.out.printf(java.util.Locale.ROOT,
                "FREQUENCIA: consultas JDBC + autorizacao independente %.1f ms | clique/retorno %.1f ms | clique/CSS/layout %.1f ms | clique/dados visiveis (amostrado) %.1f ms%n",
                tempo.join(),troca,layout,pronta);
    }

    private static double medirConsultasFrequencia(YearMonth mes) {
        FrequenciaDAO frequencias = new FrequenciaDAO();
        long inicio = System.nanoTime();
        frequencias.listar(CPF,mes.atDay(1),mes.atEndOfMonth());
        var medidas = new MedidaSocioeducativaDAO().listarPorAdolescente(CPF);
        LocalDate hoje = LocalDate.now();
        LocalDate desde = medidas.stream().map(MedidaSocioeducativa::getDataInicio).min(LocalDate::compareTo).orElse(hoje);
        if (!desde.isAfter(hoje)) frequencias.listar(CPF,desde,hoje);
        return (System.nanoTime()-inicio)/1_000_000.0;
    }

    private static void verificarRespostaObsoletaFrequencia(Stage janela) throws Exception {
        var dao = new AdolescenteDAO();
        var outro = new Adolescente();
        outro.setCpf(CPF+8);
        outro.setNomeCompleto("Segunda pessoa do cenário de teste");
        outro.setDataNascimento(LocalDate.of(2009,1,1));
        outro.setContato("(43) 99999-0808");
        outro.setNaturalidade("");
        outro.setCorRaca("Não informada");
        dao.salvarDadosOperacionais(outro,new EducacaoTrabalho(),false);
        var primeiro = dao.buscarResumoDTO(CPF);
        var segundo = dao.buscarResumoDTO(outro.getCpf());

        var campoCarga = DetalhesAdolescenteController.class.getDeclaredField("cargaFrequencia");
        campoCarga.setAccessible(true);
        for (boolean trocaCpf : new boolean[]{false,true}) {
            FXMLLoader loader = carregar("DetalhesAdolescenteView");
            Parent perfil = loader.getRoot();
            janela.getScene().setRoot(perfil);
            perfil.applyCss();
            perfil.layout();
            var controller = (DetalhesAdolescenteController)loader.getController();
            controller.carregarDados(primeiro);
            @SuppressWarnings("unchecked")
            var lista = (ListView<Frequencia>)perfil.lookup("#listaFrequenciaPerfil");
            var mes = (DatePicker)perfil.lookup("#dpMesFrequencia");
            var conteudo = perfil.lookup("#conteudoFrequencia");
            var antigaAplicada = new java.util.concurrent.atomic.AtomicBoolean();
            lista.itemsProperty().addListener((obs,anterior,registros) -> {
                if (registros.stream().anyMatch(f -> f.getCpfAdolescente()==CPF
                        && YearMonth.from(f.getDataPresenca()).equals(YearMonth.now())))
                    antigaAplicada.set(true);
            });
            ((Button)perfil.lookup("#btnAbaFrequencia")).fire();
            var antiga = (javafx.concurrent.Task<?>)campoCarga.get(controller);
            if (!trocaCpf) mes.setValue(LocalDate.now().minusMonths(1));
            controller.carregarDados(trocaCpf ? segundo : primeiro);
            aguardarInterface(() -> conteudo.isVisible() && !antiga.isRunning()
                    && antiga.getState()==javafx.concurrent.Worker.State.SUCCEEDED);
            verificar(!antigaAplicada.get() && lista.getItems().isEmpty(),
                    trocaCpf ? "resposta atrasada não aparece no perfil de outro adolescente"
                            : "resposta atrasada não sobrescreve a solicitação mais recente");
        }
    }

    static void aguardarInterface(java.util.function.BooleanSupplier pronta) {
        if (pronta.getAsBoolean()) return;
        Object chave = new Object();
        boolean[] esperando = {true};
        var relogio = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().factory());
        relogio.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            if (esperando[0] && pronta.getAsBoolean()) {
                esperando[0] = false;
                Platform.exitNestedEventLoop(chave,true);
            }
        }),0,20,TimeUnit.MILLISECONDS);
        relogio.schedule(() -> Platform.runLater(() -> {
            if (esperando[0]) {
                esperando[0] = false;
                Platform.exitNestedEventLoop(chave,false);
            }
        }),15,TimeUnit.SECONDS);
        try {
            if (!Boolean.TRUE.equals(Platform.enterNestedEventLoop(chave)))
                throw new AssertionError("O carregamento em segundo plano não concluiu.");
        } finally {
            esperando[0] = false;
            relogio.shutdownNow();
        }
    }

    private static void verificarMascaras() {
        TextField cpf = new TextField();
        MascarasFormulario.cpf(cpf);
        cpf.replaceText(0,0,"12345678909");
        verificar(cpf.getText().equals("123.456.789-09") && MascarasFormulario.cpfValido(cpf.getText()),"CPF de 11 dígitos é formatado");
        cpf.appendText("2");
        verificar(cpf.getText().equals("123.456.789-09"),"CPF não aceita um décimo segundo dígito digitado");
        cpf.selectAll(); cpf.replaceSelection("123456789092");
        verificar(cpf.getText().equals("123.456.789-09")
                && !MascarasFormulario.cpfValido("123456789092"),"colagem de CPF longo é recusada por inteiro");
        verificar(!MascarasFormulario.cpfValido("12345678901") && !MascarasFormulario.cpfValido("00000000000"),
                "CPF com verificador incorreto ou com dígitos repetidos é inválido");
        cpf.selectAll(); cpf.replaceSelection("52998224725");
        verificar(cpf.getText().equals("529.982.247-25") && MascarasFormulario.cpfValido(cpf.getText()),
                "substituição de CPF selecionado aceita outro CPF válido");
        cpf.selectAll(); cpf.replaceSelection("12345678901");
        cpf.positionCaret(3); cpf.deleteNextChar();
        verificar(Formatadores.soDigitos(cpf.getText()).equals("1235678901"),"Delete diante de ponto remove o próximo dígito");
        cpf.selectAll(); cpf.replaceSelection("12345678901");
        cpf.positionCaret(4); cpf.deletePreviousChar();
        verificar(Formatadores.soDigitos(cpf.getText()).equals("1245678901"),"Backspace após ponto remove o dígito anterior");

        TextField telefone = new TextField();
        MascarasFormulario.telefone(telefone);
        for (String entrada : List.of("4330332023","43999991234","+554330332023","+55 (43) 99999-1234")) {
            telefone.selectAll(); telefone.replaceSelection(entrada);
            verificar(MascarasFormulario.telefoneValido(telefone.getText())
                    && Formatadores.soDigitos(entrada).equals(Formatadores.soDigitos(telefone.getText()))
                    && telefone.getText().startsWith("+")==entrada.startsWith("+"),
                    "telefone preserva todos os dígitos e o prefixo quando informado");
        }
        telefone.positionCaret(4);
        telefone.deletePreviousChar();
        verificar(telefone.getText().equals("+55 (43) 99999-1234"),
                "Backspace no espaço logo após +55 mantém o prefixo");
        telefone.positionCaret(8);
        telefone.deleteNextChar();
        verificar(Formatadores.soDigitos(telefone.getText()).equals("554399991234"),
                "Delete diante do espaço após o DDD remove o próximo dígito, sem alterar o prefixo");
        String longo = "+55 439999912345678";
        String telefoneAntes = telefone.getText();
        telefone.selectAll(); telefone.replaceSelection(longo);
        verificar(telefone.getText().equals(telefoneAntes) && !MascarasFormulario.telefoneValido(longo),
                "telefone longo é recusado por inteiro");
        telefone.selectAll(); telefone.replaceSelection("+144330332023");
        verificar(telefone.getText().equals(telefoneAntes), "telefone com prefixo diferente de +55 é recusado");
        telefone.selectAll(); telefone.replaceSelection("554330332023");
        verificar(telefone.getText().equals(telefoneAntes), "DDD e prefixo internacional não são inferidos pela quantidade de dígitos");
        telefone.clear();
        for (char caractere : "+5543999991234".toCharArray()) telefone.appendText(String.valueOf(caractere));
        verificar(telefone.getText().equals("+55 (43) 99999-1234"),
                "telefone internacional completo pode ser digitado caractere por caractere");
        verificar(MascarasFormulario.telefoneValidoOuExistente("3033-2023","3033-2023")
                && !MascarasFormulario.telefoneValidoOuExistente("3033-2023",null),
                "apenas contato legado inalterado pode manter formato sem DDD");
        TextField limiteTexto = new TextField();
        ValidacaoFormulario.limitarTexto(limiteTexto,80,"Nome");
        limiteTexto.replaceSelection("á".repeat(80));
        verificar(limiteTexto.getLength()==80,"texto aceita exatamente o limite do campo");
        limiteTexto.appendText("x");
        verificar(limiteTexto.getText().equals("á".repeat(80)),"caractere acima do limite é recusado");
        limiteTexto.selectAll(); limiteTexto.replaceSelection("a".repeat(4000));
        verificar(limiteTexto.getText().equals("á".repeat(80)),"colagem longa rejeita toda a edição");
        limiteTexto.selectAll(); limiteTexto.replaceSelection("Ana");
        verificar(limiteTexto.getText().equals("Ana"),"texto selecionado pode ser substituído");
        TextField limiteNumerico = new TextField();
        ValidacaoFormulario.limitarInteiro(limiteNumerico,24,"Horas");
        limiteNumerico.replaceSelection("24");
        limiteNumerico.appendText("0");
        limiteNumerico.selectAll(); limiteNumerico.replaceSelection("25");
        limiteNumerico.selectAll(); limiteNumerico.replaceSelection("2a");
        verificar(limiteNumerico.getText().equals("24"),"campo numérico rejeita excesso de dígitos, valor acima da faixa e letras");
        limiteNumerico.selectAll(); limiteNumerico.replaceSelection("10");
        verificar(limiteNumerico.getText().equals("10"),"substituição numérica aceita valor dentro da faixa");
        PasswordField senha = new PasswordField();
        ValidacaoFormulario.limitarSenha(senha,Senhas.MAXIMO_NOVA);
        senha.replaceSelection(" *. ".repeat(32));
        senha.appendText("x");
        verificar(senha.getLength()==128 && senha.getText().endsWith(" "),
                "limite de senha preserva espaços e símbolos, sem aceitar o caractere 129");
        String limiteEmail = "a".repeat(84)+"@x.com";
        verificar(ValidacaoFormulario.email(limiteEmail).equals(limiteEmail),"e-mail no limite de 90 caracteres aceito");
        for (String invalido : List.of("sem-arroba","a"+limiteEmail)) {
            try { ValidacaoFormulario.email(invalido); throw new AssertionError("E-mail inválido aceito."); }
            catch (IllegalArgumentException esperado) { verificar(true,"e-mail inválido ou acima do limite é rejeitado"); }
        }
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
