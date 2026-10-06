package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.controller.*;
import br.edu.unespar.trabalho.dao.*;
import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Testes executáveis apenas com o JDK, JavaFX e JDBC já usados pelo projeto. */
public class VerificacaoCadastroFrequencia {
    private static int verificacoes;
    private static final long CPF=12345678901L;
    private static final YearMonth JANEIRO=YearMonth.of(2025,1);

    public static void main(String[] args) throws Exception {
        testarRegras();
        if(Arrays.asList(args).contains("--integracao")) testarBanco(Arrays.asList(args).contains("--interface"));
        System.out.println("OK: "+verificacoes+" verificações concluídas.");
    }

    private static void testarRegras() {
        Adolescente a=adolescente(CPF); MedidaSocioeducativa psc=medida(CPF,TipoMedida.PSC,LocalDate.of(2025,1,1)); psc.setIdMedida(1);
        MedidaSocioeducativa anterior=medida(CPF,TipoMedida.PSC,LocalDate.of(2024,1,1)); anterior.setIdMedida(2); anterior.setDataFim(LocalDate.of(2024,12,31));
        var f=frequencia(1,1,LocalDate.of(2025,1,7),StatusPresenca.PRESENTE,4);
        var f2=frequencia(2,1,LocalDate.of(2025,1,9),StatusPresenca.FALTA_INJUSTIFICADA,0);
        var f3=frequencia(3,1,LocalDate.of(2025,1,9),StatusPresenca.FALTA_INJUSTIFICADA,0);
        var j=frequencia(1,1,LocalDate.of(2025,1,11),StatusPresenca.FALTA_JUSTIFICADA,0); j.setObservacoes("Atestado");
        var antigo=frequencia(1,2,LocalDate.of(2024,12,10),StatusPresenca.PRESENTE,20);
        var proximo=frequencia(1,1,LocalDate.of(2025,2,1),StatusPresenca.PRESENTE,8);
        var linha=new FrequenciaMensalDTO(a,List.of(psc,anterior),List.of(f,f2,f3,j,antigo,proximo),JANEIRO,false);
        verifica(linha.getHorasCumpridas()==4,"horas antigas e do mês seguinte não contaminam a PSC/mês selecionados");
        verifica(linha.getHorasPendentes()==44,"saldo de PSC");
        var encerrada = medida(CPF, TipoMedida.PSC, LocalDate.of(2024,12,1));
        encerrada.setIdMedida(3);
        encerrada.setDataFim(LocalDate.of(2025,1,15));
        var seguinte = medida(CPF, TipoMedida.PSC, LocalDate.of(2025,1,16));
        seguinte.setIdMedida(4);
        var excesso = frequencia(1,3,LocalDate.of(2025,1,10),StatusPresenca.PRESENTE,60);
        verifica(new FrequenciaMensalDTO(a,List.of(encerrada,seguinte),List.of(excesso),JANEIRO,false)
                .getHorasPendentes()==48,"excesso de horas em uma PSC não quita outra no mesmo mês");
        verifica(linha.getFaltasInjustificadas()==1 && linha.getSituacao().equals("Regular"),"duas atividades na mesma data contam uma ausência");
        verifica(linha.getDia(8).isEmpty() && linha.getDia(11).equals("J"),"dia vazio não vira falta e justificativa tem legenda própria");
        var falta=frequencia(1,1,LocalDate.of(2025,1,14),StatusPresenca.FALTA_INJUSTIFICADA,0);
        verifica(new FrequenciaMensalDTO(a,List.of(psc),List.of(f2,falta,j),JANEIRO,false).getSituacao().equals("Irregular"),"duas datas injustificadas tornam o mês irregular");
        rejeita(IllegalArgumentException.class,()->frequencia(1,1,LocalDate.now().plusDays(1),StatusPresenca.PRESENTE,4).validar(),"data futura rejeitada");
        rejeita(IllegalArgumentException.class,()->frequencia(1,1,LocalDate.of(2025,1,1),StatusPresenca.FALTA_INJUSTIFICADA,4).validar(),"falta não recebe horas");
        rejeita(IllegalArgumentException.class,()->frequencia(1,1,LocalDate.of(2025,1,1),StatusPresenca.FALTA_JUSTIFICADA,0).validar(),"justificativa exige motivo");
        var la=medida(CPF,TipoMedida.LA,LocalDate.of(2024,2,29));
        verifica(la.getMesesCorridos(LocalDate.of(2024,3,28))==0 && la.getMesesCorridos(LocalDate.of(2024,3,29))==1,"meses completos de LA e ano bissexto");
        verifica(YearMonth.of(2024,2).lengthOfMonth()==29,"competência bissexta");
    }

    private static void testarBanco(boolean interfaceFx) throws Exception {
        String schema="casa_teste_"+UUID.randomUUID().toString().replace("-","");
        String urlAnterior=System.getProperty("casa.db.url");
        try(Connection admin=ConnectionFactory.getConnection(); Statement s=admin.createStatement()) {
            String url=admin.getMetaData().getURL();
            if(url.toLowerCase().contains("currentschema=")) throw new IllegalStateException("Execute os testes sem currentSchema na URL de entrada.");
            s.execute("CREATE SCHEMA "+schema);
            try {
                System.setProperty("casa.db.url",url+(url.contains("?")?"&":"?")+"currentSchema="+schema);
                try(Connection c=ConnectionFactory.getConnection(); Statement ddl=c.createStatement()) {
                    ddl.execute(Files.readString(Path.of("src/test/resources/schema_original.sql")));
                    ddl.execute("INSERT INTO Pessoa(cpf,nome_completo,data_nascimento) VALUES(10000000001,'Legado de teste','2008-01-01')");
                    ddl.execute("INSERT INTO Adolescente(cpf_adolescente,naturalidade,cor_raca,status) VALUES(10000000001,'Apucarana','Parda','Ativo')");
                    ddl.execute("INSERT INTO PIA(id_pia,data_elaboracao,documento_enviado,cpf_adolescente) VALUES(1,'2024-01-01',true,10000000001)");
                    ddl.execute("INSERT INTO MedidaSocioeducativa(id_medida,reincidencia,tipo_medida,data_inicio,duracao_horas,cpf_adolescente) VALUES(1,false,'PSC','2024-01-01',48,10000000001)");
                    ddl.execute("INSERT INTO Atividade VALUES(1,'Atividade legada',NULL,4)");
                    ddl.execute("INSERT INTO Frequencia VALUES(10000000001,1,'2024-01-02','Presente',4)");
                    ddl.execute(Files.readString(Path.of("sql/ajustes_cadastro_frequencia.sql")));
                    ddl.execute(Files.readString(Path.of("sql/ajustes_cadastro_frequencia.sql")));
                }
                verifica(new FrequenciaDAO().listar(10000000001L,LocalDate.of(2024,1,1),LocalDate.of(2024,1,31)).getFirst().getIdMedida()==1,"ajuste SQL reexecutável preserva e vincula frequência legada inequívoca");

                AdolescenteDAO adao=new AdolescenteDAO();
                verifica(adao.buscarPorCpf(10000000001L).isPiaEnviado(),"ajuste SQL preserva confirmação de envio do PIA já existente");
                Adolescente a=adolescente(CPF); SituacaoSocial social=social(); Saude saude=new Saude();
                saude.setUbsReferencia("UBS Centro"); saude.setUsoSpa(true); saude.setSubstanciasUtilizadas("Dado fictício");
                EducacaoTrabalho estudo=new EducacaoTrabalho(); estudo.setEstuda(true); estudo.setEscola("Escola de teste"); estudo.setSerie("2º ano");
                a.setMedidaProtetiva(true);
                adao.salvarCadastro(a,social,saude,estudo,false);
                verifica(adao.buscarPorCpf(CPF).isMedidaProtetiva(),"cadastro persiste medida protetiva Sim");
                verifica(adao.buscarPorCpf(CPF).getNomeCompleto().equals(a.getNomeCompleto()),"cadastro persiste e relê identificação");
                verifica(new SituacaoSocialDAO().buscarPorCpf(CPF).getNumeroNis()==12345678901L,"NIS de 11 dígitos e CRAS textual");
                verifica(new SaudeDAO().buscarPorCpf(CPF).isUsoSpa() && new EducacaoTrabalhoDAO().buscarPorCpf(CPF).isEstuda(),"saúde e educação persistidas");
                a.setNomeCompleto("Adolescente atualizado"); a.setObservacoes("Somente às terças"); estudo.setEstuda(false); estudo.setEscola(null); estudo.setSerie(null);
                a.setPiaEnviado(true);
                a.setMedidaProtetiva(false);
                adao.salvarCadastro(a,social,saude,estudo,true);
                verifica(!adao.buscarPorCpf(CPF).isMedidaProtetiva(),"edição persiste medida protetiva Não");
                verifica(adao.buscarPorCpf(CPF).isPiaEnviado() && new PIADAO().buscarPorCpf(CPF)==null,
                        "cadastro confirma envio de PIA sem elaborar documento no sistema");
                verifica(new FrequenciaMensalDAO().listar(JANEIRO).stream().filter(l->l.getAdolescente().getCpf()==CPF)
                        .findFirst().orElseThrow().isPiaEnviado(),"planilha mensal lê a confirmação de envio do cadastro");
                SituacaoSocial incompleta=social(); incompleta.setEndereco(" ");
                rejeita(IllegalArgumentException.class,()->adao.salvarCadastro(a,incompleta,saude,estudo,true),"endereço obrigatório");
                incompleta.setEndereco("Rua fictícia"); incompleta.setBairro("");
                rejeita(IllegalArgumentException.class,()->adao.salvarCadastro(a,incompleta,saude,estudo,true),"bairro obrigatório");
                String telefone=a.getContato(); a.setContato(" ");
                rejeita(IllegalArgumentException.class,()->adao.salvarCadastro(a,social,saude,estudo,true),"telefone obrigatório");
                a.setContato(telefone);
                PIADAO pdao=new PIADAO(); PIA pia=new PIA(); pia.setIdPia(2); pia.setCpfAdolescente(CPF);
                pia.setDataElaboracao(LocalDate.of(2025,1,1)); pia.setDiagnostico("Dado fictício");
                verifica(pdao.inserir(pia) && pdao.buscarPorCpf(CPF).isDocumentoEnviado(),
                        "elaboração de PIA não apaga confirmação de envio já feita no cadastro");
                pia=pdao.buscarPorCpf(CPF); pia.setDocumentoEnviado(false); pdao.atualizar(pia);
                verifica(!adao.buscarPorCpf(CPF).isPiaEnviado(),"edição de envio no PIA sincroniza confirmação cadastral");
                pia.setDocumentoEnviado(true); pdao.atualizar(pia);
                verifica(adao.buscarPorCpf(CPF).getObservacoes().equals("Somente às terças") && !new EducacaoTrabalhoDAO().buscarPorCpf(CPF).isEstuda(),"edição do cadastro e limpeza de campos condicionais");
                rejeita(IllegalArgumentException.class,()->adao.salvarCadastro(a,social,saude,estudo,false),"CPF duplicado rejeitado");
                Adolescente outro=adolescente(CPF+1); EducacaoTrabalho grande=new EducacaoTrabalho(); grande.setEstuda(true); grande.setEscola("x".repeat(121));
                rejeita(IllegalStateException.class,()->adao.salvarCadastro(outro,social,saude,grande,false),"falha de tabela filha reverte toda a transação");
                verifica(adao.buscarPorCpf(CPF+1)==null,"nenhum adolescente parcial após rollback");

                Responsavel r=new Responsavel(); r.setCpf(99999999901L); r.setNomeCompleto("Responsável fictício"); r.setDataNascimento(LocalDate.of(1980,1,1)); r.setParentesco("Mãe"); r.setContatoPrincipal(true);
                ResponsavelDAO rdao=new ResponsavelDAO(); rdao.inserir(r,CPF);
                adao.salvarCadastro(outro,social,saude,estudo,false); r.setParentesco("Tia"); rdao.inserir(r,CPF+1);
                verifica(rdao.listarResponsaveis(CPF).getFirst().getParentesco().equals("Mãe") && rdao.listarResponsaveis(CPF+1).getFirst().getParentesco().equals("Tia"),"responsável reutilizado com parentescos independentes");
                r.setContato("(43) 99999-0000"); rdao.atualizar(r,CPF+1);
                verifica(rdao.buscarPessoa(r.getCpf()).getContato().equals(r.getContato()),"edição de responsável");
                rdao.desvincular(CPF+1,r.getCpf()); verifica(rdao.listarResponsaveis(CPF).size()==1,"desvincular não apaga responsável do outro adolescente");
                ComposicaoFamiliar f=new ComposicaoFamiliar(); f.setCpfAdolescente(CPF); f.setNome("Familiar fictício"); f.setParentesco("Irmão");
                ComposicaoFamiliarDAO cdao=new ComposicaoFamiliarDAO(); cdao.inserir(f); f.setIdade(15); cdao.atualizar(f);
                verifica(cdao.listarPorAdolescente(CPF).getFirst().getIdade()==15,"inclusão e edição de composição familiar");
                cdao.excluir(f.getIdComposicaoFamiliar(),CPF); verifica(cdao.listarPorAdolescente(CPF).isEmpty(),"exclusão de familiar");

                MedidaSocioeducativaDAO mdao=new MedidaSocioeducativaDAO();
                var psc=medida(CPF,TipoMedida.PSC,LocalDate.of(2025,1,1)); psc.setDataFim(LocalDate.of(2025,2,28)); mdao.inserir(psc);
                var la=medida(CPF,TipoMedida.LA,LocalDate.of(2025,1,1)); mdao.inserir(la);
                var nova=medida(CPF,TipoMedida.PSC,LocalDate.of(2025,3,1)); mdao.inserir(nova);
                rejeita(IllegalArgumentException.class,()->mdao.inserir(medida(CPF,TipoMedida.PSC,LocalDate.of(2025,3,2))),"sobreposição de PSC rejeitada");
                Atividade at=new Atividade(); at.setNomeAtividade("Oficina de teste"); at.setCargaHoraria(4); new AtividadeDAO().inserir(at);
                Atividade at2=new Atividade(); at2.setNomeAtividade("Outra oficina"); at2.setCargaHoraria(4); new AtividadeDAO().inserir(at2);
                FrequenciaDAO freq=new FrequenciaDAO();
                var presenca=frequencia(at.getIdAtividade(),psc.getIdMedida(),LocalDate.of(2025,1,7),StatusPresenca.PRESENTE,4); freq.registrar(presenca);
                rejeita(IllegalArgumentException.class,()->freq.registrar(presenca),"duplicidade atividade/data rejeitada");
                verifica(mdao.consultarHorasDaMedida(psc.getIdMedida(),LocalDate.now())==4,"presença credita horas à PSC");
                psc.setDataFim(LocalDate.of(2025,1,6));
                rejeita(IllegalArgumentException.class,()->mdao.atualizar(psc),"encerramento não pode excluir frequência já vinculada");
                psc.setDataFim(LocalDate.of(2025,2,28));
                psc.setTipoMedida(TipoMedida.LA);
                rejeita(IllegalArgumentException.class,()->mdao.atualizar(psc),"PSC vinculada não vira LA");
                psc.setTipoMedida(TipoMedida.PSC);
                presenca.setHorasCumpridas(2); freq.atualizar(presenca); verifica(mdao.consultarHorasDaMedida(psc.getIdMedida(),LocalDate.now())==2,"correção recalcula horas sem duplicar");
                presenca.setStatusPresenca(StatusPresenca.FALTA_JUSTIFICADA); presenca.setHorasCumpridas(0); presenca.setObservacoes("Atestado fictício"); freq.atualizar(presenca);
                verifica(mdao.consultarHorasDaMedida(psc.getIdMedida(),LocalDate.now())==0,"troca de presença por falta remove horas");
                rejeita(IllegalArgumentException.class,()->freq.registrar(frequencia(at2.getIdAtividade(),psc.getIdMedida(),LocalDate.of(2025,3,1),StatusPresenca.PRESENTE,4)),"registro fora da vigência rejeitado");
                var errado=frequencia(at2.getIdAtividade(),psc.getIdMedida(),LocalDate.of(2025,1,7),StatusPresenca.PRESENTE,4); errado.setCpfAdolescente(CPF+1);
                rejeita(IllegalArgumentException.class,()->freq.registrar(errado),"medida de outro adolescente rejeitada");
                var hoje=frequencia(at.getIdAtividade(),nova.getIdMedida(),LocalDate.now(),StatusPresenca.PRESENTE,20); freq.registrar(hoje);
                rejeita(IllegalArgumentException.class,()->freq.registrar(frequencia(at2.getIdAtividade(),nova.getIdMedida(),LocalDate.now(),StatusPresenca.PRESENTE,5)),"limite diário de 24 horas entre atividades");
                verifica(mdao.consultarHorasDaMedida(psc.getIdMedida(),LocalDate.now())==0 && mdao.consultarHorasDaMedida(nova.getIdMedida(),LocalDate.now())==20,"nova PSC não herda horas da medida anterior");
                var somenteLa=frequencia(at2.getIdAtividade(),null,LocalDate.now(),StatusPresenca.PRESENTE,0); freq.registrar(somenteLa);
                verifica(freq.listar(CPF,LocalDate.now(),LocalDate.now()).size()==2,"acompanhamento de LA sem horas de PSC");
                rejeita(IllegalArgumentException.class,()->freq.registrar(frequencia(at2.getIdAtividade(),null,LocalDate.now().minusDays(1),StatusPresenca.PRESENTE,4)),"horas novas sem PSC rejeitadas");
                for(int dia:new int[]{9,14}) freq.registrar(frequencia(at.getIdAtividade(),psc.getIdMedida(),LocalDate.of(2025,1,dia),StatusPresenca.FALTA_INJUSTIFICADA,0));
                var mensal=new FrequenciaMensalDAO().listar(JANEIRO).stream().filter(l->l.getAdolescente().getCpf()==CPF).findFirst().orElseThrow();
                verifica(mensal.getSituacao().equals("Irregular") && mensal.getMse().equals("LA/PSC"),"planilha combina medidas sem duplicar adolescente e calcula irregularidade mensal");
                verifica(adao.listarResumoDTO().stream().filter(l->l.getCpf().equals(a.getCpfFormatado())).count()==1,"listagem de cadastro tem uma linha por adolescente");
                freq.excluir(hoje); verifica(mdao.consultarHorasDaMedida(nova.getIdMedida(),LocalDate.now())==0,"exclusão recalcula saldo");
                if(interfaceFx) testarInterface();
            } finally {
                if(urlAnterior==null) System.clearProperty("casa.db.url"); else System.setProperty("casa.db.url",urlAnterior);
                // Exclusivamente o schema aleatório criado por esta execução. Nunca public.
                if(!schema.matches("casa_teste_[a-f0-9]{32}")) throw new IllegalStateException("Schema de teste inválido.");
                s.execute("DROP SCHEMA "+schema+" CASCADE");
            }
        }
    }

    private static void testarInterface() throws Exception {
        CompletableFuture<Void> fim=new CompletableFuture<>();
        Platform.startup(()-> {
            Platform.setImplicitExit(false);
            try {
                Files.createDirectories(Path.of("target/verificacao"));
                for(String nome:List.of("CadastroAdolescenteView","AdolescentesView","DetalhesAdolescenteView","FrequenciaView","RegistroFrequenciaView")) {
                    FXMLLoader loader=new FXMLLoader(VerificacaoCadastroFrequencia.class.getResource("/View/"+nome+".fxml"));
                    Parent root=loader.load(); Object controller=loader.getController();
                    if(controller instanceof CadastroAdolescenteController cadastro) cadastro.carregarParaEdicao(CPF);
                    if(controller instanceof DetalhesAdolescenteController perfil) perfil.carregarDados(new AdolescenteDAO().listarResumoDTO().stream().filter(l->l.getCpf().replaceAll("\\D","").equals(Long.toString(CPF))).findFirst().orElseThrow());
                    if(controller instanceof RegistroFrequenciaController registro) registro.carregar(CPF,LocalDate.now(),()->{});
                    // Reproduz cores claras herdadas do tema sem deixar textos invisíveis nos cartões brancos.
                    root.setStyle("-fx-text-background-color: white; -fx-text-base-color: white;");
                    Scene scene=new Scene(root,controller instanceof RegistroFrequenciaController?720:1280,
                            controller instanceof RegistroFrequenciaController?650:850);
                    root.applyCss(); root.layout();
                    if(controller instanceof CadastroAdolescenteController cadastro) {
                        verifica(((TextField)root.lookup("#txtCras")).getText().equals("CRAS Centro"),"FXML de edição carrega CRAS textual");
                        verifica(((TextField)root.lookup("#txtCpf")).isDisabled(),"edição protege a chave CPF");
                        ComboBox<String> protetiva=(ComboBox<String>)root.lookup("#cmbMedidaProtetiva");
                        verifica("Não".equals(protetiva.getValue()) && protetiva.getItems().equals(List.of("Não","Sim")),"tela carrega medida protetiva e oferece Sim/Não");
                        protetiva.setValue("Sim");
                        DatePicker nascimento=(DatePicker)root.lookup("#dpNascimento");
                        Label aviso=(Label)root.lookup("#lblAvisoIdade");
                        for(int idade:List.of(11,12,21,22)) {
                            nascimento.setValue(LocalDate.now().minusYears(idade));
                            verifica(aviso.isVisible()==(idade<12 || idade>21),"aviso de faixa etária aos "+idade+" anos");
                        }
                        for (String campo : List.of("chkSpa", "chkEstuda", "chkTrabalha", "chkImm", "chkValeTransporte", "chkPiaEnviado")) {
                            CheckBox caixa = (CheckBox)root.lookup("#"+campo);
                            verifica(!caixa.getText().isBlank() && caixa.getTextFill().equals(javafx.scene.paint.Color.web("#333333")),
                                    "texto visível da caixa " + campo + " mesmo com tema herdado claro");
                        }
                        Stage janela = new Stage();
                        janela.setScene(scene);
                        ((TextField)root.lookup("#txtNome")).setText("Cadastro validado pela tela");
                        ((TextField)root.lookup("#txtRenda")).setText("1,75");
                        ((CheckBox)root.lookup("#chkEstuda")).setSelected(true);
                        ((TextField)root.lookup("#txtEscola")).setText("Escola de teste da tela");
                        ((TextField)root.lookup("#txtSerie")).setText("3º ano");
                        fecharProximoDialogo(ButtonType.OK);
                        cadastro.salvarAdolescente(new ActionEvent(root, root));
                        verifica(new AdolescenteDAO().buscarPorCpf(CPF).getNomeCompleto().equals("Cadastro validado pela tela")
                                && new SituacaoSocialDAO().buscarPorCpf(CPF).getRendaFamiliar()==1.75
                                && new EducacaoTrabalhoDAO().buscarPorCpf(CPF).isEstuda(),
                                "formulário JavaFX salva cadastro completo com renda decimal e escolaridade");
                        Adolescente salvo=new AdolescenteDAO().buscarPorCpf(CPF);
                        verifica(salvo.isMedidaProtetiva() && salvo.getDataNascimento().equals(LocalDate.now().minusYears(22)),"tela salva medida protetiva e permite cadastro fora da faixa etária");
                        verifica(janela.getScene().getRoot().lookup("#lblNome") instanceof Label nomePerfil
                                && nomePerfil.getText().equals("Cadastro validado pela tela"),
                                "salvar cadastro abre o perfil com os dados persistidos");
                        janela.close();
                        scene.setRoot(root);
                        ((ScrollPane)root.lookup(".scroll-pane")).setVvalue(1);
                        root.applyCss(); root.layout();
                    }
                    if (controller instanceof DetalhesAdolescenteController) {
                        DatePicker encerramento = (DatePicker)root.lookup("#dpFimMedida");
                        verifica(encerramento.getParent() instanceof javafx.scene.layout.VBox grupo
                                && grupo.getChildren().getFirst() instanceof Label rotulo
                                && rotulo.getText().contains("ENCERRAMENTO"),"calendário de encerramento tem rótulo acima do campo");
                        ((Button)root.lookup("#btnAbaFrequencia")).fire();
                        ListView<?> lista = (ListView<?>)root.lookup("#listaFrequenciaPerfil");
                        Button corrigir = (Button)root.lookup("#btnCorrigirFrequencia");
                        verifica(corrigir.isDisabled(),"correção exige selecionar uma frequência");
                        lista.getSelectionModel().selectFirst();
                        root.applyCss(); root.layout();
                        verifica(!corrigir.isDisabled() && root.lookupAll(".button").stream()
                                .filter(n -> n instanceof Button b && b.getText().startsWith("Corrigir")).count()==1,
                                "aba de frequência tem um único botão Corrigir selecionado");
                    }
                    if (controller instanceof FrequenciaController mensal) {
                        TableView<?> tabela = (TableView<?>) root.lookup("#tabela");
                        verifica(tabela.getItems().size()==3,"planilha JavaFX carrega as linhas do banco na abertura");
                        DatePicker competencia = (DatePicker) root.lookup("#dpCompetencia");
                        competencia.getEditor().setText(competencia.getConverter().toString(LocalDate.of(2024,2,1)));
                        mensal.atualizar();
                        verifica(tabela.getColumns().size()==39,"planilha JavaFX monta os 29 dias de fevereiro bissexto");
                        competencia.getEditor().setText(competencia.getConverter().toString(JANEIRO.atDay(1)));
                        mensal.atualizar();
                    }
                    if (controller instanceof RegistroFrequenciaController registro) {
                        DatePicker data = (DatePicker) root.lookup("#dpData");
                        data.setValue(LocalDate.of(2025,1,8));
                        ComboBox<?> atividade = (ComboBox<?>) root.lookup("#cmbAtividade");
                        atividade.getSelectionModel().selectFirst();
                        ((TextField)root.lookup("#txtHoras")).setText("3");
                        registro.salvar();
                        var salvas = new FrequenciaDAO().listar(CPF,data.getValue(),data.getValue());
                        verifica(salvas.size()==1 && salvas.getFirst().getHorasContabilizadas()==3,
                                "formulário JavaFX registra frequência no JDBC");
                        ((ListView<?>)root.lookup("#listaRegistros")).getSelectionModel().selectFirst();
                        @SuppressWarnings("unchecked")
                        ComboBox<StatusPresenca> status = (ComboBox<StatusPresenca>)root.lookup("#cmbStatus");
                        status.setValue(StatusPresenca.FALTA_JUSTIFICADA);
                        ((TextArea)root.lookup("#txtObservacoes")).setText("Justificativa fictícia");
                        registro.salvar();
                        salvas = new FrequenciaDAO().listar(CPF,data.getValue(),data.getValue());
                        verifica(salvas.size()==1 && salvas.getFirst().getHorasContabilizadas()==0
                                && salvas.getFirst().getStatusPresenca()==StatusPresenca.FALTA_JUSTIFICADA,
                                "formulário JavaFX corrige presença para falta sem duplicar ou manter horas");
                        ((ListView<?>)root.lookup("#listaRegistros")).getSelectionModel().selectFirst();
                        root.applyCss(); root.layout();
                        ListView<?> lista = (ListView<?>)root.lookup("#listaRegistros");
                        verifica(lista.lookup(".list-cell:selected") instanceof ListCell<?> celula
                                && !celula.getText().isBlank()
                                && celula.getBackground().getFills().getFirst().getFill().equals(javafx.scene.paint.Color.web("#19523e"))
                                && celula.getTextFill().equals(javafx.scene.paint.Color.WHITE),
                                "oficina selecionada mantém texto legível e fundo verde");
                        verifica(root.lookupAll(".label").stream().filter(n -> n instanceof Label)
                                .map(n -> (Label)n).noneMatch(l -> javafx.scene.paint.Color.WHITE.equals(l.getTextFill())),
                                "rótulos do editor permanecem visíveis no cartão branco");
                        boolean[] abriuSelecionada = { false };
                        Frequencia falta = salvas.getFirst();
                        fecharProximoDialogo(ButtonType.CLOSE, painel -> {
                            var registros = (ListView<?>)painel.lookup("#listaRegistros");
                            var situacao = (ComboBox<?>)painel.lookup("#cmbStatus");
                            abriuSelecionada[0] = registros.getSelectionModel().getSelectedItem() instanceof Frequencia f
                                    && f.getIdAtividade()==falta.getIdAtividade()
                                    && situacao.getValue()==StatusPresenca.FALTA_JUSTIFICADA;
                        });
                        RegistroFrequenciaController.abrir(falta, () -> {});
                        verifica(abriuSelecionada[0],"correção abre a oficina selecionada preservando a falta justificada");
                    }
                    WritableImage imagem=root.snapshot(null,null);
                    BufferedImage png=new BufferedImage((int)imagem.getWidth(),(int)imagem.getHeight(),BufferedImage.TYPE_INT_ARGB);
                    for(int y=0;y<png.getHeight();y++) for(int x=0;x<png.getWidth();x++) png.setRGB(x,y,imagem.getPixelReader().getArgb(x,y));
                    ImageIO.write(png,"png",Path.of("target/verificacao/"+nome+".png").toFile());
                    verifica(true,"FXML carregado e renderizado: "+nome);
                    if (controller instanceof RegistroFrequenciaController registro) {
                        fecharProximoDialogo(ButtonType.YES);
                        registro.excluir();
                        LocalDate data = ((DatePicker)root.lookup("#dpData")).getValue();
                        verifica(new FrequenciaDAO().listar(CPF,data,data).isEmpty(),
                                "botão JavaFX exclui a frequência após confirmação");
                    }
                }
                fim.complete(null);
            } catch(Throwable t) { fim.completeExceptionally(t); }
        });
        try { fim.get(45,TimeUnit.SECONDS); } finally { Platform.exit(); }
    }

    private static void fecharProximoDialogo(ButtonType resposta) {
        fecharProximoDialogo(resposta, painel -> {});
    }

    private static void fecharProximoDialogo(ButtonType resposta, java.util.function.Consumer<DialogPane> verificar) {
        Platform.runLater(() -> {
            for (Window janela : List.copyOf(Window.getWindows())) {
                if (janela.getScene().getRoot().lookup(".dialog-pane") instanceof DialogPane painel
                        && painel.lookupButton(resposta) instanceof Button botao) {
                    try { verificar.accept(painel); } finally { botao.fire(); }
                    return;
                }
            }
        });
    }

    private static Adolescente adolescente(long cpf) {
        Adolescente a=new Adolescente(); a.setCpf(cpf); a.setNomeCompleto("Adolescente fictício"); a.setDataNascimento(LocalDate.of(2008,5,15));
        a.setNaturalidade("Apucarana / PR"); a.setCorRaca("Não informada"); a.setGenero("Não informado"); a.setStatus(StatusAdolescente.ATIVO);
        a.setContato("(43) 99999-1234"); a.setObservacoes(""); return a;
    }
    private static SituacaoSocial social() {
        SituacaoSocial s=new SituacaoSocial(); s.setRendaFamiliar(1.5); s.setEndereco("Rua fictícia, 100"); s.setBairro("Centro");
        s.setNumeroNis(12345678901L); s.setCrasNome("CRAS Centro"); return s;
    }
    private static MedidaSocioeducativa medida(long cpf,TipoMedida tipo,LocalDate inicio) {
        MedidaSocioeducativa m=new MedidaSocioeducativa(); m.setCpfAdolescente(cpf); m.setTipoMedida(tipo); m.setDataInicio(inicio);
        if(tipo==TipoMedida.PSC) m.setDuracaoHoras(48); else m.setDuracaoMeses(6); return m;
    }
    private static Frequencia frequencia(int atividade,Integer medida,LocalDate data,StatusPresenca status,int horas) {
        Frequencia f=new Frequencia(); f.setCpfAdolescente(CPF); f.setIdAtividade(atividade); f.setIdMedida(medida);
        f.setDataPresenca(data); f.setStatusPresenca(status); f.setHorasCumpridas(horas); return f;
    }
    private static void verifica(boolean condicao,String descricao) {
        if(!condicao) throw new AssertionError(descricao);
        verificacoes++; System.out.println("OK: "+descricao);
    }
    private static void rejeita(Class<? extends Throwable> tipo,Runnable acao,String descricao) {
        try { acao.run(); } catch(Throwable e) { if(tipo.isInstance(e)) { verifica(true,descricao); return; } throw e; }
        throw new AssertionError("Deveria rejeitar: "+descricao);
    }
}
