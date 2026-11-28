package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.controller.*;
import br.edu.unespar.trabalho.dao.*;
import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.service.AutorizacaoService;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import br.edu.unespar.trabalho.util.ImpressaoRelatorio;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import br.edu.unespar.trabalho.util.Sessao;
import java.io.UncheckedIOException;
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
        var futura=medida(CPF,TipoMedida.PSC,YearMonth.now().plusMonths(2).atDay(1));
        verifica(new FrequenciaMensalDTO(a,List.of(futura),List.of(),YearMonth.now(),false).getMedidasDoMes().isEmpty(),
                "medida de mês posterior não aparece na competência atual");
        verifica(new FrequenciaMensalDTO(a,List.of(futura),List.of(),YearMonth.now().plusMonths(2),false).getMse().equals("PSC"),
                "medida futura aparece na competência de início");
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
                    ddl.execute(Files.readString(Path.of("sql/agenda.sql")));
                    ddl.execute(Files.readString(Path.of("sql/agenda.sql")));
                    ddl.execute(Files.readString(Path.of("sql/relatorios_acompanhamento.sql")));
                    ddl.execute(Files.readString(Path.of("sql/relatorios_acompanhamento.sql")));
                    ddl.execute("INSERT INTO Pessoa(cpf,nome_completo,data_nascimento) VALUES(77777777770,'Técnico de agenda','1980-01-01')");
                    ddl.execute("INSERT INTO EquipeTecnica(cpf_equipe,login,senha,cargo_funcao,nivel_acesso) VALUES(77777777770,'agenda_teste','senha fictícia','Técnico','EQUIPE_TECNICA')");
                }
                VerificacaoContas.executar();
                verifica(new FrequenciaDAO().listar(10000000001L,LocalDate.of(2024,1,1),LocalDate.of(2024,1,31)).getFirst().getIdMedida()==1,"ajuste SQL reexecutável preserva e vincula frequência legada inequívoca");

                AdolescenteDAO adao=new AdolescenteDAO();
                verifica(adao.buscarPorCpf(10000000001L).isPiaEnviado(),"ajuste SQL preserva confirmação de envio do PIA já existente");
                Adolescente a=adolescente(CPF); SituacaoSocial social=social(); Saude saude=new Saude();
                saude.setUbsReferencia("UBS Centro"); saude.setUsoSpa(true); saude.setSubstanciasUtilizadas("Dado fictício");
                EducacaoTrabalho estudo=new EducacaoTrabalho(); estudo.setEstuda(true); estudo.setEscola("Escola de teste"); estudo.setSerie("2º ano");
                a.setMedidaProtetiva(true);
                a.setStatus(StatusAdolescente.INATIVO);
                adao.salvarCadastro(a,social,saude,estudo,false);
                verifica(adao.buscarPorCpf(CPF).getStatus()==StatusAdolescente.ATIVO,
                        "inclusão do administrador grava Ativo mesmo quando recebe outro status");
                adao.salvarCadastro(a,social,saude,estudo,true);
                verifica(adao.buscarPorCpf(CPF).getStatus()==StatusAdolescente.INATIVO,
                        "administrador pode alterar a situação após a inclusão");
                a.setStatus(StatusAdolescente.ATIVO);
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
                SituacaoSocial semRenda=new SituacaoSocial();
                semRenda.setEndereco(social.getEndereco()); semRenda.setBairro(social.getBairro());
                rejeita(IllegalArgumentException.class,()->adao.salvarCadastro(a,semRenda,saude,estudo,true),"renda ausente não é persistida como zero");
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
                pdao.confirmarEnvio(CPF,false);
                verifica(!pdao.consultarEnvio(CPF) && !adao.buscarPorCpf(CPF).isPiaEnviado(),"PIA confirma envio sem editar texto");
                verifica(pdao.buscarPorCpf(CPF).getDiagnostico().equals("Dado fictício"),"confirmação preserva texto histórico do PIA");
                pdao.confirmarEnvio(CPF,true);
                pdao.confirmarEnvio(10000000001L,false);
                verifica(!pdao.consultarEnvio(10000000001L),"confirmação também atualiza PIA legado");

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
                r.setNomeCompleto("Alteração de formulário antigo");
                rejeita(IllegalArgumentException.class,()->rdao.atualizar(r,CPF+1),"edição antiga não recria vínculo removido");
                verifica(rdao.listarResponsaveis(CPF+1).isEmpty()
                        && !rdao.buscarPessoa(r.getCpf()).getNomeCompleto().equals(r.getNomeCompleto()),
                        "vínculo removido e dados pessoais preservados após edição rejeitada");
                Adolescente inexistente=adolescente(r.getCpf());
                rejeita(IllegalArgumentException.class,()->adao.salvarCadastro(inexistente,social,saude,estudo,true),
                        "edição exige registro de adolescente além de Pessoa");
                verifica(!rdao.buscarPessoa(r.getCpf()).getNomeCompleto().equals(inexistente.getNomeCompleto()),
                        "edição de adolescente inexistente reverte alteração de Pessoa");
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
                try(Connection c=ConnectionFactory.getConnection(); PreparedStatement legado=c.prepareStatement(
                        "UPDATE Frequencia SET status_presenca=? WHERE cpf_adolescente=? AND id_atividade=? AND data_presenca=?")) {
                    legado.setLong(2,CPF); legado.setInt(3,at.getIdAtividade()); legado.setObject(4,LocalDate.of(2025,1,7));
                    legado.setString(1,"PRESENTE "); verifica(legado.executeUpdate()==1,"cenário legado fictício preparado");
                    try {
                        verifica(mdao.consultarHorasDaMedida(psc.getIdMedida(),LocalDate.now())==4,
                                "saldo da PSC conta presença legada com espaço final");
                        rejeita(IllegalArgumentException.class,()->freq.registrar(frequencia(at2.getIdAtividade(),psc.getIdMedida(),LocalDate.of(2025,1,7),StatusPresenca.PRESENTE,21)),
                                "limite diário inclui presença legada com espaço final");
                    } finally { legado.setString(1,"PRESENTE"); legado.executeUpdate(); }
                }
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
                var somenteLa=frequencia(at2.getIdAtividade(),la.getIdMedida(),LocalDate.now(),StatusPresenca.PRESENTE,0); freq.registrar(somenteLa);
                verifica(freq.listar(CPF,LocalDate.now(),LocalDate.now()).stream().anyMatch(registro->registro.getIdMedida()!=null && registro.getIdMedida()==la.getIdMedida() && registro.getTipoMedida()==TipoMedida.LA && registro.getHorasContabilizadas()==0),"comparecimento vinculado explicitamente à LA sem horas de PSC");
                somenteLa.setHorasCumpridas(4);
                rejeita(IllegalArgumentException.class,()->freq.atualizar(somenteLa),"LA não pode receber horas mesmo por acesso direto ao DAO");
                somenteLa.setHorasCumpridas(0);
                la.setHistoricoInfracional("Atualização com comparecimento vinculado"); mdao.atualizar(la);
                rejeita(IllegalArgumentException.class,()->freq.registrar(frequencia(at2.getIdAtividade(),null,LocalDate.now().minusDays(1),StatusPresenca.PRESENTE,4)),"horas novas sem PSC rejeitadas");
                for(int dia:new int[]{9,14}) freq.registrar(frequencia(at.getIdAtividade(),psc.getIdMedida(),LocalDate.of(2025,1,dia),StatusPresenca.FALTA_INJUSTIFICADA,0));
                var mensal=new FrequenciaMensalDAO().listar(JANEIRO).stream().filter(l->l.getAdolescente().getCpf()==CPF).findFirst().orElseThrow();
                verifica(mensal.getSituacao().equals("Irregular") && mensal.getMse().equals("LA/PSC"),"planilha combina medidas sem duplicar adolescente e calcula irregularidade mensal");
                var resumos = adao.listarResumoDTO();
                verifica(resumos.stream().filter(l->l.getCpf().equals(a.getCpfFormatado())).count()==1,
                        "listagem de cadastro tem uma linha por adolescente");
                var resumo = resumos.stream().filter(l->l.getCpf().equals(a.getCpfFormatado())).findFirst().orElseThrow();
                var mensalAtual = new FrequenciaMensalDAO().listar(YearMonth.now()).stream()
                        .filter(l->l.getAdolescente().getCpf()==CPF).findFirst().orElseThrow();
                String progressoEsperado = mensalAtual.getHorasPrevistas()>0
                        ? mensalAtual.getHorasCumpridas()+"/"+mensalAtual.getHorasPrevistas()+"h" : "";
                if (!mensalAtual.getMeses().isEmpty())
                    progressoEsperado+=(progressoEsperado.isEmpty()?"":" · ")+mensalAtual.getMeses()+"m";
                verifica(resumo.getMedida().equals(mensalAtual.getMse().isEmpty()?"Sem medida":mensalAtual.getMse())
                                && resumo.getTextoProgresso().equals(progressoEsperado.isEmpty()?"—":progressoEsperado)
                                && adao.buscarResumoDTO(CPF).getTextoProgresso().equals(resumo.getTextoProgresso()),
                        "consulta compacta preserva medidas e progresso da planilha");
                var fevereiro1=frequencia(at.getIdAtividade(),psc.getIdMedida(),LocalDate.of(2025,2,4),StatusPresenca.FALTA_INJUSTIFICADA,0);
                var fevereiro2=frequencia(at.getIdAtividade(),psc.getIdMedida(),LocalDate.of(2025,2,11),StatusPresenca.FALTA_INJUSTIFICADA,0);
                freq.registrar(fevereiro1); freq.registrar(fevereiro2);
                freq.excluir(fevereiro2);
                verifica(adao.buscarPorCpf(CPF).getStatus()==StatusAdolescente.EM_DESCUMPRIMENTO,"excluir falta de fevereiro preserva descumprimento de janeiro");
                freq.registrar(fevereiro2); fevereiro2.setStatusPresenca(StatusPresenca.PRESENTE); freq.atualizar(fevereiro2);
                verifica(adao.buscarPorCpf(CPF).getStatus()==StatusAdolescente.EM_DESCUMPRIMENTO,"corrigir falta de fevereiro preserva descumprimento de janeiro");
                freq.excluir(frequencia(at.getIdAtividade(),psc.getIdMedida(),LocalDate.of(2025,1,14),StatusPresenca.FALTA_INJUSTIFICADA,0));
                verifica(adao.buscarPorCpf(CPF).getStatus()==StatusAdolescente.ATIVO,"retorna a Ativo quando nenhum mês permanece irregular");
                freq.registrar(frequencia(at.getIdAtividade(),psc.getIdMedida(),LocalDate.of(2025,1,14),StatusPresenca.FALTA_INJUSTIFICADA,0));
                freq.excluir(hoje); verifica(mdao.consultarHorasDaMedida(nova.getIdMedida(),LocalDate.now())==0,"exclusão recalcula saldo");
                testarStatusProtegidos();
                testarFalhasConsultaFamilia();
                testarPreservacaoRendaFamiliar();
                testarAgenda();
                testarPermissoesOperacionais();
                if(interfaceFx) {
                    freq.registrar(frequencia(at.getIdAtividade(),psc.getIdMedida(),LocalDate.of(2025,1,6),StatusPresenca.PRESENTE,6));
                    testarInterface();
                }
            } finally {
                br.edu.unespar.trabalho.util.Sessao.limparSessao();
                if(urlAnterior==null) System.clearProperty("casa.db.url"); else System.setProperty("casa.db.url",urlAnterior);
                // Exclusivamente o schema aleatório criado por esta execução. Nunca public.
                if(!schema.matches("casa_teste_[a-f0-9]{32}")) throw new IllegalStateException("Schema de teste inválido.");
                s.execute("DROP SCHEMA "+schema+" CASCADE");
            }
        }
    }

    private static void testarPermissoesOperacionais() throws Exception {
        boolean piaOriginal=new PIADAO().consultarEnvio(CPF);
        String ubsOriginal=new SaudeDAO().buscarPorCpf(CPF).getUbsReferencia();
        var mdao = new MedidaSocioeducativaDAO();
        var encerrada = medida(CPF+1,TipoMedida.LA,LocalDate.of(2023,1,1));
        encerrada.setDataFim(LocalDate.of(2023,8,1));
        encerrada.setHistoricoInfracional("Histórico reservado");
        mdao.inserir(encerrada);
        var agenda = new EventoAgendaDAO();
        var evento = eventoAgenda(LocalDate.of(2023,8,2), "Compromisso reservado de teste");
        evento.setObservacoes("Observação restrita de teste");
        agenda.inserir(evento);
        String notas = new AdolescenteDAO().buscarPorCpf(CPF).getObservacoes();
        try {
            VerificacaoContas.entrar("agenda_teste", "senha fictícia");
            var listaTecnica = new AdolescenteDAO().listarResumoDTO();
            verifica(listaTecnica.stream().anyMatch(jovem -> "Centro".equals(jovem.getBairro())),
                    "consulta compacta da lista técnica inclui o bairro autorizado");
            verifica("Centro".equals(new AdolescenteDAO().buscarPorCpf(CPF).getBairro()),"projeção técnica inclui dados sociais");
            var adolescentes = new AdolescenteDAO();
            var novo = adolescente(CPF+10);
            novo.setNomeCompleto("Novo cadastro técnico");
            novo.setObservacoes("Informação protegida");
            novo.setPiaEnviado(true);
            novo.setStatus(StatusAdolescente.INATIVO);
            adolescentes.salvarDadosOperacionais(novo,new EducacaoTrabalho(),false);
            verifica(adolescentes.buscarPorCpf(novo.getCpf()).getStatus()==StatusAdolescente.ATIVO,
                    "Equipe Técnica cadastra adolescente com situação inicial ativa");
            var responsaveis = new ResponsavelDAO();
            var contato = new Responsavel();
            contato.setCpf(99999999910L); contato.setNomeCompleto("Contato de teste");
            contato.setDataNascimento(LocalDate.of(1980,1,1)); contato.setParentesco("Tia");
            responsaveis.inserir(contato,novo.getCpf());
            contato.setContato("43999991234"); responsaveis.atualizar(contato,novo.getCpf());
            verifica(responsaveis.listarResponsaveis(novo.getCpf()).getFirst().getContato().equals("43999991234"),
                    "Equipe Técnica cadastra e edita contato exclusivo");
            responsaveis.inserir(contato,CPF);
            contato.setContato("43999990001"); contato.setParentesco("Avó");
            responsaveis.atualizar(contato,novo.getCpf());
            verifica(responsaveis.listarResponsaveis(CPF).stream().filter(r->r.getCpf()==contato.getCpf()).findFirst()
                    .orElseThrow().getContato().equals("43999990001")
                    && responsaveis.listarResponsaveis(novo.getCpf()).getFirst().getParentesco().equals("Avó"),
                    "Equipe Técnica edita dados pessoais compartilhados e o parentesco do vínculo");
            rejeita(IllegalArgumentException.class,()->responsaveis.buscarPessoa(77777777770L),"CPF de conta da equipe não pode ser reutilizado");
            contato.setCpf(77777777770L);
            rejeita(IllegalArgumentException.class,()->responsaveis.inserir(contato,novo.getCpf()),"DAO impede vínculo indevido por CPF");
            verifica(responsaveis.desvincular(novo.getCpf(),99999999910L) && responsaveis.listarResponsaveis(novo.getCpf()).isEmpty(),
                    "Equipe Técnica desvincula responsável");
            var familia = new ComposicaoFamiliarDAO();
            var integrante = new ComposicaoFamiliar();
            integrante.setCpfAdolescente(novo.getCpf()); integrante.setNome("Familiar fictício"); integrante.setParentesco("Irmã");
            verifica(familia.inserir(integrante) && familia.excluir(integrante.getIdComposicaoFamiliar(),novo.getCpf()),
                    "Equipe Técnica inclui e remove composição familiar");
            verifica(adolescentes.excluir(novo.getCpf())
                    && adolescentes.buscarPorCpf(novo.getCpf()).getStatus()==StatusAdolescente.INATIVO,
                    "Equipe Técnica inativa pelo fluxo existente");
            var freq = new FrequenciaDAO();
            var protegida=freq.listar(CPF,LocalDate.of(2025,1,7),LocalDate.of(2025,1,7)).getFirst();
            verifica(protegida.getIdMedida()!=null && "Atestado fictício".equals(protegida.getObservacoes()),
                    "Equipe Técnica consulta a justificativa autorizada");
            protegida.setObservacoes("Atestado fictício corrigido");
            verifica(freq.atualizar(protegida), "Equipe Técnica corrige a justificativa autorizada");
            var descartavel = frequencia(1,null,LocalDate.of(2025,1,9),StatusPresenca.PRESENTE,0);
            descartavel.setCpfAdolescente(novo.getCpf());
            verifica(freq.registrar(descartavel) && freq.excluir(descartavel),"Equipe Técnica aceita exclusão de frequência");
            var relatorio = freq.listarParaRelatorioOperacional(CPF,JANEIRO);
            verifica(!relatorio.isEmpty() && relatorio.stream().allMatch(f -> f.getObservacoes()==null),"relatório técnico omite notas e justificativas");
            var operacional=adolescentes.buscarPorCpf(CPF);
            var situacaoOriginal=operacional.getStatus();
            verifica(Objects.equals(notas,operacional.getObservacoes()), "projeção técnica inclui observações gerais");
            var visivel = agenda.listar(evento.getData(),evento.getData()).getFirst();
            verifica(evento.getObservacoes().equals(visivel.getObservacoes()), "projeção da agenda inclui observações autorizadas");
            visivel.setTitulo("Compromisso corrigido"); agenda.atualizar(visivel);
            var medidaTecnica = mdao.listarPorAdolescente(CPF+1).stream()
                    .filter(m -> m.getIdMedida()==encerrada.getIdMedida()).findFirst().orElseThrow();
            verifica("Histórico reservado".equals(medidaTecnica.getHistoricoInfracional()),"Equipe Técnica consulta histórico infracional");
            medidaTecnica.setDataFim(null);
            verifica(mdao.atualizar(medidaTecnica),"Equipe Técnica reabre medida encerrada");
            medidaTecnica.setDataFim(LocalDate.of(2023,8,1));
            verifica(mdao.atualizar(medidaTecnica),"Equipe Técnica encerra a medida novamente");
            operacional.setContato(" ");
            rejeita(IllegalArgumentException.class,()->new AdolescenteDAO().salvarDadosOperacionais(operacional,new EducacaoTrabalhoDAO().buscarPorCpf(CPF)),
                    "edição operacional também exige telefone");
            operacional.setContato("43999990000");
            operacional.setPiaEnviado(!piaOriginal);
            operacional.setStatus(StatusAdolescente.INATIVO);
            new AdolescenteDAO().salvarDadosOperacionais(operacional,new EducacaoTrabalhoDAO().buscarPorCpf(CPF));
            VerificacaoContas.entrar("admin-teste", VerificacaoContas.SENHA);
            verifica(adolescentes.buscarPorCpf(CPF).getStatus()==situacaoOriginal,
                    "salvamento exclusivamente operacional preserva a situação");
            verifica(new SituacaoSocialDAO().buscarPorCpf(novo.getCpf())==null && new SaudeDAO().buscarPorCpf(novo.getCpf())==null
                    && !new PIADAO().consultarEnvio(novo.getCpf()), "caminho exclusivamente operacional preserva a ausência de fichas");
            verifica(freq.listar(CPF,LocalDate.of(2025,1,7),LocalDate.of(2025,1,7)).getFirst().getObservacoes().equals("Atestado fictício corrigido"),
                    "correção técnica gravou a justificativa");
            verifica(agenda.listar(evento.getData(),evento.getData()).getFirst().getObservacoes().equals(evento.getObservacoes()),
                    "edição técnica de compromisso preserva observação reservada");
            verifica(Objects.equals(notas,new AdolescenteDAO().buscarPorCpf(CPF).getObservacoes()),
                    "edição técnica preserva observações gerais");
            verifica(new SituacaoSocialDAO().buscarPorCpf(CPF).getTelefone().equals("43999990000"),
                    "consulta social acompanha telefone atualizado");
            verifica(new PIADAO().consultarEnvio(CPF)==piaOriginal && java.util.Objects.equals(new SaudeDAO().buscarPorCpf(CPF).getUbsReferencia(),ubsOriginal),
                    "edição operacional preserva saúde e PIA");
            VerificacaoContas.entrar("agenda_teste", "senha fictícia");

            verifica(new SaudeDAO().buscarPorCpf(CPF)!=null,"Equipe Técnica consulta saúde");
            verifica(new SituacaoSocialDAO().buscarPorCpf(CPF)!=null,"Equipe Técnica consulta ficha social");
            var pia = new PIADAO();
            pia.confirmarEnvio(CPF,!piaOriginal);
            verifica(pia.consultarEnvio(CPF)!=piaOriginal,"Equipe Técnica confirma o envio do PIA");
            pia.confirmarEnvio(CPF,piaOriginal);
            verifica(new RelatorioAcompanhamentoDAO().listar()!=null,"Equipe Técnica abre consulta de relatórios completos");
            var perfilDAO = new PerfilAdolescenteDAO();
            perfilDAO.definirTecnicoReferencia(CPF,77777777770L);
            verifica(Objects.equals(perfilDAO.buscarCpfTecnicoReferencia(CPF),77777777770L),"Equipe Técnica define a referência");
            var equipe = new EquipeTecnicaDAO();
            verifica(equipe.listarParaReferencia().stream().anyMatch(p -> p.getCpf()==77777777770L && p.getLogin()==null
                    && p.getNivelAcesso()==null && p.getSenha()==null), "seletor recebe apenas dados profissionais operacionais");
            var outraConta = VerificacaoContas.conta(85555555556L,"conta-negada",NivelAcesso.ADMINISTRADOR);
            rejeita(SecurityException.class,equipe::listar,"Equipe Técnica não lista contas pelo DAO");
            rejeita(SecurityException.class,()->equipe.buscarPorCpf(77777777770L),"Equipe Técnica não busca conta pelo DAO");
            rejeita(SecurityException.class,()->equipe.inserir(outraConta),"Equipe Técnica não cria usuário pelo DAO");
            rejeita(SecurityException.class,()->equipe.atualizar(outraConta),"Equipe Técnica não edita conta ou perfil pelo DAO");
            rejeita(SecurityException.class,()->equipe.definirAtivo(77777777770L,false),"Equipe Técnica não desativa contas pelo DAO");
            rejeita(SecurityException.class,()->equipe.excluir(77777777770L),"Equipe Técnica não exclui conta pelo DAO");
            rejeita(SecurityException.class,()->equipe.redefinirSenha(77777777770L,VerificacaoContas.SENHA),"Equipe Técnica não redefine senha de terceiros pelo DAO");
            rejeita(SecurityException.class,equipe::migrarSenhasLegadas,"Equipe Técnica não executa migração administrativa pela sessão");
            var pessoaDeConta=adolescente(77777777770L);
            rejeita(SecurityException.class,()->adolescentes.atualizar(pessoaDeConta),
                    "edição de adolescente não altera a Pessoa de uma conta");
            rejeita(SecurityException.class,()->adolescentes.salvarCadastro(pessoaDeConta,social(),new Saude(),new EducacaoTrabalho(),true,false),
                    "cadastro completo não permite alterar a Pessoa de uma conta");
            try(var c=ConnectionFactory.getConnection();var s=c.createStatement()) {
                s.executeUpdate("INSERT INTO Responsavel(cpf_responsavel,parentesco,contato_principal) VALUES(77777777770,'Legado',false)");
                try {
                    verifica(!responsaveis.podeEditarDadosPessoais(77777777770L),
                            "cadastro familiar legado com CPF de conta não libera edição de dados pessoais");
                } finally { s.executeUpdate("DELETE FROM Responsavel WHERE cpf_responsavel=77777777770"); }
            }
            rejeita(SecurityException.class,()->NavegacaoUtil.exigirTela("/View/EquipeTecnicaView.fxml"),
                    "Equipe Técnica não abre a administração por navegação alternativa");
            verifica(agenda.excluir(visivel.getIdEvento()), "Equipe Técnica exclui compromisso");
            var cadastroAtividade = new AtividadeDAO();
            var atividadeNova = new Atividade();
            atividadeNova.setNomeAtividade("Oficina fictícia");
            atividadeNova.setTipo("Oficina"); atividadeNova.setCargaHoraria(1);
            verifica(cadastroAtividade.inserir(atividadeNova),"Equipe Técnica cadastra atividade");
            atividadeNova.setCargaHoraria(2);
            verifica(cadastroAtividade.atualizar(atividadeNova) && cadastroAtividade.excluir(atividadeNova.getIdAtividade()),
                    "Equipe Técnica edita e remove atividade sem vínculos");
            var documento = new Documento();
            documento.setIdDocumento(701); documento.setCpfAdolescente(CPF);
            documento.setTipo("Relatório"); documento.setDataGeracao(LocalDate.of(2025,1,7));
            var documentos = new DocumentoDAO();
            verifica(documentos.inserir(documento) && documentos.atualizarStatus(701,"Pendente")
                    && documentos.listarPorAdolescente(CPF).stream().anyMatch(d -> d.getIdDocumento()==701),
                    "Equipe Técnica registra, atualiza e consulta metadados de documento");
            equipe.trocarMinhaSenha("senha fictícia",VerificacaoContas.SENHA);
            verifica(!Sessao.validarContaAtual(),"alteração da própria senha invalida a sessão da Equipe Técnica");
            rejeita(SecurityException.class,()->new RelatorioAcompanhamentoDAO().listar(),"sessão técnica revogada não acessa relatório");
            VerificacaoContas.entrar("agenda_teste", VerificacaoContas.SENHA);
            equipe.trocarMinhaSenha(VerificacaoContas.SENHA,"senha fictícia");
            VerificacaoContas.entrar("agenda_teste", "senha fictícia");
            Sessao.configurarInatividade(java.time.Duration.ofNanos(1));
            try {
                rejeita(SecurityException.class,()->new RelatorioAcompanhamentoDAO().listar(),"sessão técnica expirada não acessa relatório");
            } finally { Sessao.configurarInatividade(java.time.Duration.ofMinutes(15)); }
            VerificacaoContas.entrar("agenda_teste", "senha fictícia");
            try(var c=ConnectionFactory.getConnection();var s=c.createStatement()) {
                s.executeUpdate("UPDATE EquipeTecnica SET nivel_acesso='EDUCADOR' WHERE login='agenda_teste'");
                try {
                    rejeita(SecurityException.class,()->new RelatorioAcompanhamentoDAO().listar(),
                            "mudança do perfil invalida a sessão técnica mesmo sem mudar a versão");
                } finally { s.executeUpdate("UPDATE EquipeTecnica SET nivel_acesso='EQUIPE_TECNICA' WHERE login='agenda_teste'"); }
            }
            VerificacaoContas.entrar("educador-teste", VerificacaoContas.SENHA);
            verifica(!new FrequenciaMensalDAO().listar(JANEIRO).isEmpty(),"educador consulta frequência por projeções operacionais");
            var dia = LocalDate.of(2025,1,7);
            var limitada = new FrequenciaDAO().listar(CPF,dia,dia).getFirst();
            verifica(limitada.getObservacoes()==null && limitada.getIdMedida()==null && limitada.getHorasContabilizadas()==0,
                    "Educador vê o status sem justificativa, medida ou horas na consulta direta");
            rejeita(SecurityException.class,()->mdao.listarParaFrequencia(CPF),"Educador não consulta medidas pela planilha");
            rejeita(SecurityException.class,()->new AdolescenteDAO().buscarPorCpf(CPF),"educador não consulta prontuário");
            Frequencia f=frequencia(1,null,LocalDate.now(),StatusPresenca.PRESENTE,2);
            rejeita(SecurityException.class,()->new FrequenciaDAO().registrar(f),"educador não registra horas");
            rejeita(SecurityException.class,()->new FrequenciaDAO().excluir(f),"educador não exclui frequência");
            rejeita(SecurityException.class,()->freq.atualizar(protegida),"educador não corrige frequência");
            rejeita(SecurityException.class,()->freq.listarParaRelatorioOperacional(CPF,JANEIRO),"educador não gera relatório");
            var faltaEducador = frequencia(1,null,LocalDate.of(2025,1,7),StatusPresenca.FALTA_JUSTIFICADA,0);
            faltaEducador.setCpfAdolescente(novo.getCpf()); faltaEducador.setObservacoes("Motivo informado");
            verifica(freq.registrar(faltaEducador) && freq.listar(novo.getCpf(),faltaEducador.getDataPresenca(),faltaEducador.getDataPresenca())
                    .getFirst().getObservacoes()==null, "educador registra falta justificada e a consulta omite o texto");
        } finally { VerificacaoContas.entrar("admin-teste", VerificacaoContas.SENHA); }
    }

    private static void testarStatusProtegidos() throws SQLException {
        var dao=new FrequenciaDAO();
        var anterior=new AdolescenteDAO().buscarPorCpf(CPF).getStatus();
        var atividade=new Atividade(); atividade.setNomeAtividade("Verificação de status"); atividade.setCargaHoraria(1);
        new AtividadeDAO().inserir(atividade);
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement update=c.prepareStatement("UPDATE Adolescente SET status=? WHERE cpf_adolescente=?")) {
            update.setLong(2,CPF);
            try {
                for(var status:List.of(StatusAdolescente.INATIVO,StatusAdolescente.EM_ANALISE_EXTINCAO)) {
                    update.setString(1,status.getCodigo()); update.executeUpdate();
                    var primeira=frequencia(atividade.getIdAtividade(),null,LocalDate.of(2025,2,20),StatusPresenca.FALTA_INJUSTIFICADA,0);
                    var segunda=frequencia(atividade.getIdAtividade(),null,LocalDate.of(2025,2,21),StatusPresenca.FALTA_INJUSTIFICADA,0);
                    dao.registrar(primeira); dao.registrar(segunda);
                    verifica(new AdolescenteDAO().buscarPorCpf(CPF).getStatus()==status,"faltas preservam situação manual "+status);
                    primeira.setStatusPresenca(StatusPresenca.PRESENTE); dao.atualizar(primeira); dao.excluir(segunda);
                    verifica(new AdolescenteDAO().buscarPorCpf(CPF).getStatus()==status,"correção e exclusão preservam situação manual "+status);
                    dao.excluir(primeira);
                }
            } finally { update.setString(1,anterior.getCodigo()); update.executeUpdate(); }
        }
    }

    private static void testarFalhasConsultaFamilia() throws SQLException {
        // Executado exclusivamente dentro do schema aleatório criado por testarBanco.
        try(Connection c=ConnectionFactory.getConnection(); Statement sql=c.createStatement()) {
            sql.execute("ALTER TABLE Responsabiliza RENAME TO vinculo_teste_indisponivel");
            try {
                rejeita(IllegalStateException.class,()->new ResponsavelDAO().listarResponsaveis(CPF),
                        "falha SQL de responsáveis não é apresentada como lista vazia");
            } finally { sql.execute("ALTER TABLE vinculo_teste_indisponivel RENAME TO Responsabiliza"); }
            sql.execute("ALTER TABLE ComposicaoFamiliar RENAME TO familia_teste_indisponivel");
            try {
                rejeita(IllegalStateException.class,()->new ComposicaoFamiliarDAO().listarPorAdolescente(CPF),
                        "falha SQL de familiares não é apresentada como lista vazia");
            } finally { sql.execute("ALTER TABLE familia_teste_indisponivel RENAME TO ComposicaoFamiliar"); }
        }
        verifica(new ComposicaoFamiliarDAO().listarPorAdolescente(CPF).isEmpty(),
                "consulta familiar válida ainda admite lista vazia");
    }

    private static void testarPreservacaoRendaFamiliar() throws Exception {
        var dao=new ComposicaoFamiliarDAO();
        var familiar=new ComposicaoFamiliar(); familiar.setCpfAdolescente(CPF);
        familiar.setNome("Familiar para teste"); familiar.setParentesco("Irmão"); familiar.setRenda(1500.0);
        dao.inserir(familiar);
        try {
            VerificacaoContas.entrar("agenda_teste", "senha fictícia");
            var tecnico=dao.listarPorAdolescente(CPF).getFirst();
            verifica(tecnico.getRenda().equals(1500.0),"consulta técnica inclui renda familiar autorizada");
            tecnico.setNome("Familiar com nome corrigido"); dao.atualizar(tecnico);
            VerificacaoContas.entrar("admin-teste", VerificacaoContas.SENHA);
            var salvo=dao.listarPorAdolescente(CPF).getFirst();
            verifica(salvo.getRenda().equals(1500.0) && salvo.getNome().equals("Familiar com nome corrigido"),
                    "edição técnica de familiar preserva renda protegida");
        } finally {
            VerificacaoContas.entrar("admin-teste", VerificacaoContas.SENHA);
            dao.excluir(familiar.getIdComposicaoFamiliar(),CPF);
        }
    }

    private static void testarAgenda() throws Exception {
        EventoAgendaDAO dao=new EventoAgendaDAO();
        EventoAgenda evento=eventoAgenda(LocalDate.of(2026,8,31),"Evento de teste");
        evento.setCpfAdolescente(CPF); evento.setCpfEquipe(77777777770L); dao.inserir(evento);
        verifica(dao.listar(evento.getData(),evento.getData()).getFirst().getNomeTecnico().equals("Técnico de agenda"),"agenda persiste vínculos e nomes dos participantes");
        EventoAgenda conflito=eventoAgenda(evento.getData(),"Sobreposição"); conflito.setCpfAdolescente(CPF);
        rejeita(IllegalArgumentException.class,()->dao.inserir(conflito),"agenda impede sobreposição do adolescente");
        conflito.setCpfAdolescente(null); conflito.setCpfEquipe(77777777770L);
        rejeita(IllegalArgumentException.class,()->dao.inserir(conflito),"agenda impede sobreposição do técnico");
        evento.setTitulo("Evento atualizado"); dao.atualizar(evento);
        verifica(dao.listar(evento.getData(),evento.getData()).getFirst().getTitulo().equals("Evento atualizado"),"agenda edita sem criar duplicata");
        evento.setStatus(EventoAgenda.Status.CANCELADO); dao.atualizar(evento); dao.inserir(conflito);
        verifica(dao.listar(evento.getData(),evento.getData()).size()==2,"cancelado preserva histórico e libera horário");
        conflito.setStatus(EventoAgenda.Status.CONCLUIDO); dao.atualizar(conflito);
        verifica(dao.excluir(conflito.getIdEvento()),"agenda conclui e exclui compromisso");
        EventoAgenda futuro=eventoAgenda(LocalDate.now().plusDays(1),"Futuro"); futuro.setStatus(EventoAgenda.Status.CONCLUIDO);
        rejeita(IllegalArgumentException.class,()->dao.inserir(futuro),"compromisso futuro não pode ser concluído");
        EventoAgenda invalido=eventoAgenda(LocalDate.now(),"Horário inválido"); invalido.setHoraFim(invalido.getHoraInicio());
        rejeita(IllegalArgumentException.class,()->dao.inserir(invalido),"agenda rejeita horário final igual ao inicial");
        EventoAgenda legado=eventoAgenda(LocalDate.of(2025,5,15),"Compromisso legado fictício"); dao.inserir(legado);
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(
                "UPDATE EventoAgenda SET hora_inicio=?,hora_fim=? WHERE id_evento=?")) {
            s.setObject(1,LocalTime.of(9,0,30)); s.setObject(2,LocalTime.of(10,0,45)); s.setInt(3,legado.getIdEvento());
            verifica(s.executeUpdate()==1,"horário legado fictício preparado");
        }
        legado=dao.listar(legado.getData(),legado.getData()).getFirst();
        legado.setTitulo("Título legado corrigido"); dao.atualizar(legado);
        EventoAgenda relido=dao.listar(legado.getData(),legado.getData()).getFirst();
        verifica(relido.getTitulo().equals("Título legado corrigido") && relido.getHoraInicio().equals(LocalTime.of(9,0,30))
                && relido.getHoraFim().equals(LocalTime.of(10,0,45)),"edição de título preserva segundos legados");
        relido.setHoraInicio(LocalTime.of(9,0,31));
        rejeita(IllegalArgumentException.class,()->dao.atualizar(relido),"mudança de horário continua exigindo HH:mm");
        relido.setHoraInicio(LocalTime.of(9,30)); dao.atualizar(relido);
        verifica(dao.listar(relido.getData(),relido.getData()).getFirst().getHoraFim().equals(LocalTime.of(10,0,45)),
                "alterar um horário preserva o outro horário legado");
        dao.inserir(eventoAgenda(LocalDate.now(),"Agenda real da semana"));
    }

    private static EventoAgenda eventoAgenda(LocalDate data,String titulo) {
        EventoAgenda e=new EventoAgenda(); e.setTitulo(titulo); e.setData(data); e.setTipo(EventoAgenda.Tipo.ATENDIMENTO);
        e.setHoraInicio(LocalTime.of(9,0)); e.setHoraFim(LocalTime.of(10,0)); return e;
    }

    private static void testarInterface() throws Exception {
        CompletableFuture<Void> fim=new CompletableFuture<>();
        Platform.startup(() -> Platform.setImplicitExit(false));
        Platform.runLater(() -> {
            try {
                Files.createDirectories(Path.of("target/verificacao"));
                verificarLoginELogout();
                for(String nome:List.of("Dashboard","CadastroAdolescenteView","AdolescentesView","DetalhesAdolescenteView","FrequenciaView","RegistroFrequenciaView","AgendaView","RelatoriosView","EquipeTecnicaView")) {
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
                    if (controller instanceof EquipeTecnicaController) {
                        ListView<?> lista = (ListView<?>) root.lookup("#listMembros");
                        verifica(lista.getItems().stream().anyMatch(item -> item instanceof EquipeTecnica e
                                && "admin-teste".equals(e.getLogin())), "tela da equipe lista conta real do PostgreSQL");
                        ((TextField)root.lookup("#txtPesquisa")).setText("educador-teste");
                        ((EquipeTecnicaController)controller).filtrarMembros();
                        verifica(lista.getItems().size()==1,"tela da equipe filtra profissional por login");
                    }
                    if(controller instanceof DashboardController) {
                        verifica(((javafx.scene.layout.VBox)root.lookup("#vboxAgenda")).lookupAll(".label").stream()
                                .anyMatch(n->n instanceof Label l && l.getText().equals("Agenda real da semana")),"painel mostra compromisso real da semana");
                        verifica(!((Label)root.lookup("#lblDataAlerta")).getText().contains("Nascimento:"),
                                "painel usa previs?o de medida em lugar de nascimento");
                        int pscBase=Integer.parseInt(((Label)root.lookup("#lblTotalPsc")).getText());
                        int laBase=Integer.parseInt(((Label)root.lookup("#lblTotalLa")).getText());
                        String statusOriginal=new AdolescenteDAO().buscarPorCpf(CPF).getStatus().getCodigo();
                        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("UPDATE Adolescente SET status=? WHERE cpf_adolescente=?")) {
                            s.setLong(2,CPF);
                            try {
                                s.setString(1,StatusAdolescente.ATIVO.getCodigo()); s.executeUpdate();
                                Parent ativo=FXMLLoader.load(VerificacaoCadastroFrequencia.class.getResource("/View/Dashboard.fxml"));
                                verifica(Integer.parseInt(((Label)ativo.lookup("#lblTotalPsc")).getText())==pscBase+1
                                        && Integer.parseInt(((Label)ativo.lookup("#lblTotalLa")).getText())==laBase+1,"painel conta adolescente ativo com LA/PSC em ambos os cartões");
                                s.setString(1,StatusAdolescente.INATIVO.getCodigo()); s.executeUpdate();
                                Parent inativo=FXMLLoader.load(VerificacaoCadastroFrequencia.class.getResource("/View/Dashboard.fxml"));
                                verifica(Integer.parseInt(((Label)inativo.lookup("#lblTotalPsc")).getText())==pscBase
                                        && Integer.parseInt(((Label)inativo.lookup("#lblTotalLa")).getText())==laBase,"painel exclui inativos dos contadores de medidas ativas");
                            } finally { s.setString(1,statusOriginal); s.executeUpdate(); }
                        }
                    }
                    if(controller instanceof CadastroAdolescenteController cadastro) {
                        verifica(((TextField)root.lookup("#txtCras")).getText().equals("CRAS Centro"),"FXML de edição carrega CRAS textual");
                        verifica(root.lookup("#txtCpf").isDisabled() && ((Label)root.lookup("#lblCpf")).getText().equals("CPF"),
                                "CPF fica protegido e sem asterisco na edição");
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
                    if (controller instanceof DetalhesAdolescenteController perfil) {
                        DatePicker encerramento = (DatePicker)root.lookup("#dpFimMedida");
                        verifica(encerramento.getParent() instanceof javafx.scene.layout.VBox grupo
                                && grupo.getChildren().getFirst() instanceof Label rotulo
                                && rotulo.getText().contains("ENCERRAMENTO"),"calendário de encerramento tem rótulo acima do campo");
                        verifica(((ListView<?>)root.lookup("#listaFrequenciaPerfil")).getItems().isEmpty()
                                        && ((ComboBox<?>)root.lookup("#cmbMedidaPerfil")).getItems().isEmpty(),
                                "perfil abre sem carregar frequência ou medidas de abas ocultas");
                        ((Button)root.lookup("#btnAbaFrequencia")).fire();
                        var conteudoFrequencia = root.lookup("#conteudoFrequencia");
                        VerificacaoPreparacaoEntrega.aguardarInterface(conteudoFrequencia::isVisible);
                        javafx.scene.layout.VBox geral=(javafx.scene.layout.VBox)root.lookup("#boxFrequenciaGeral");
                        verifica(geral.getChildren().size()==2,"resumo geral mostra LA e PSC vigentes juntas");
                        javafx.scene.layout.VBox pscAtual=(javafx.scene.layout.VBox)geral.getChildren().stream()
                                .filter(n->((Label)((javafx.scene.layout.VBox)n).getChildren().getFirst()).getText().startsWith("PSC")).findFirst().orElseThrow();
                        javafx.scene.layout.HBox totaisPsc=(javafx.scene.layout.HBox)pscAtual.getChildren().get(1);
                        verifica(((Label)((javafx.scene.layout.VBox)totaisPsc.getChildren().get(1)).getChildren().getFirst()).getText().equals("0h"),"PSC vigente não herda horas da PSC anterior no resumo geral");
                        @SuppressWarnings("unchecked")
                        ComboBox<MedidaSocioeducativa> historico=(ComboBox<MedidaSocioeducativa>)root.lookup("#cmbMedidaFrequenciaGeral");
                        historico.setValue(historico.getItems().stream().filter(m->m.isPSC() && m.getDataFim()!=null).findFirst().orElseThrow());
                        javafx.scene.layout.HBox totaisAntigos=(javafx.scene.layout.HBox)((javafx.scene.layout.VBox)geral.getChildren().getFirst()).getChildren().get(1);
                        verifica(geral.getChildren().size()==1 && ((Label)((javafx.scene.layout.VBox)totaisAntigos.getChildren().get(1)).getChildren().getFirst()).getText().equals("6h"),"selecionar PSC encerrada mostra suas próprias horas históricas");
                        DatePicker mesPerfil=(DatePicker)root.lookup("#dpMesFrequencia");
                        mesPerfil.setValue(JANEIRO.atDay(1)); perfil.atualizarFrequenciaMes();
                        totaisAntigos=(javafx.scene.layout.HBox)((javafx.scene.layout.VBox)geral.getChildren().getFirst()).getChildren().get(1);
                        verifica(historico.getValue()!=null && ((Label)((javafx.scene.layout.VBox)totaisAntigos.getChildren().get(1)).getChildren().getFirst()).getText().equals("6h"),"consulta mensal preserva medida selecionada e totais gerais");
                        perfil.mostrarMedidasVigentesFrequencia();
                        mesPerfil.setValue(LocalDate.now().withDayOfMonth(1)); perfil.atualizarFrequenciaMes();
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
                        verifica(tabela.getItems().size()==3 && tabela.getItems().stream().noneMatch(
                                item -> item instanceof FrequenciaMensalDTO linha && linha.getAdolescente().getCpf()==CPF+10),
                                "planilha omite o cadastro inativo sem lançamentos por padrão");
                        CheckBox inativos = (CheckBox)root.lookup("#chkInativos");
                        inativos.setSelected(true);
                        verifica(tabela.getItems().size()==4 && tabela.getItems().stream().anyMatch(
                                item -> item instanceof FrequenciaMensalDTO linha && linha.getAdolescente().getCpf()==CPF+10),
                                "planilha inclui o cadastro inativado pela Equipe Técnica quando a opção é marcada");
                        inativos.setSelected(false);
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
                        @SuppressWarnings("unchecked")
                        ComboBox<MedidaSocioeducativa> medida=(ComboBox<MedidaSocioeducativa>)root.lookup("#cmbMedida");
                        verifica(medida.getItems().stream().anyMatch(MedidaSocioeducativa::isLA) && medida.getItems().stream().anyMatch(MedidaSocioeducativa::isPSC),"editor oferece LA e PSC vigentes");
                        medida.setValue(medida.getItems().stream().filter(MedidaSocioeducativa::isPSC).findFirst().orElseThrow());
                        ((TextField)root.lookup("#txtHoras")).setText("3");
                        registro.salvar();
                        var salvas = new FrequenciaDAO().listar(CPF,data.getValue(),data.getValue());
                        verifica(salvas.size()==1 && salvas.getFirst().getHorasContabilizadas()==3,
                                "formulário JavaFX registra frequência no JDBC");
                        ((ListView<?>)root.lookup("#listaRegistros")).getSelectionModel().selectFirst();
                        medida.setValue(medida.getItems().stream().filter(MedidaSocioeducativa::isLA).findFirst().orElseThrow());
                        verifica(((TextField)root.lookup("#txtHoras")).isDisabled() && ((TextField)root.lookup("#txtHoras")).getText().equals("0"),"selecionar LA desabilita e zera horas no editor");
                        registro.salvar();
                        salvas=new FrequenciaDAO().listar(CPF,data.getValue(),data.getValue());
                        verifica(salvas.getFirst().getTipoMedida()==TipoMedida.LA && salvas.getFirst().getHorasContabilizadas()==0,"editor corrige vínculo de PSC para LA sem duplicar presença");
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
                    if(controller instanceof AgendaController agenda) {
                        agenda.exibirData(LocalDate.of(2026,8,31));
                        javafx.scene.layout.GridPane calendario=(javafx.scene.layout.GridPane)root.lookup("#gridCalendario");
                        verifica(calendario.getChildren().stream().filter(n->n.getUserData() instanceof LocalDate).count()==31
                                && calendario.getRowConstraints().size()==7,"agenda renderiza todos os dias de mês com seis semanas");
                        agenda.exibirData(LocalDate.of(2024,2,29));
                        verifica(calendario.getChildren().stream().filter(n->n.getUserData() instanceof LocalDate).count()==29,"agenda navega para fevereiro bissexto");
                        agenda.exibirData(LocalDate.of(2026,8,31));
                        fecharProximoDialogo(ButtonType.OK,painel-> {
                            ((TextField)painel.lookup("#txtEventoTitulo")).setText("Compromisso pela tela");
                            ((TextField)painel.lookup("#txtEventoInicio")).setText("14:00");
                            ((TextField)painel.lookup("#txtEventoFim")).setText("15:00");
                            ((ComboBox<?>)painel.lookup("#cmbEventoAdolescente")).getSelectionModel().selectFirst();
                        });
                        agenda.abrirModalNovoEvento(new ActionEvent(root,root));
                        EventoAgendaDAO eventos=new EventoAgendaDAO();
                        verifica(eventos.listar(LocalDate.of(2026,8,31),LocalDate.of(2026,8,31)).stream()
                                .anyMatch(e->e.getTitulo().equals("Compromisso pela tela") && e.getCpfAdolescente()!=null),"formulário JavaFX grava compromisso e vínculo no JDBC");
                        EventoAgenda legadoTela=eventos.listar(LocalDate.of(2026,8,31),LocalDate.of(2026,8,31)).stream()
                                .filter(e->e.getTitulo().equals("Compromisso pela tela")).findFirst().orElseThrow();
                        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(
                                "UPDATE EventoAgenda SET hora_inicio=?,hora_fim=? WHERE id_evento=?")) {
                            s.setObject(1,LocalTime.of(14,0,30)); s.setObject(2,LocalTime.of(15,0,45));
                            s.setInt(3,legadoTela.getIdEvento()); s.executeUpdate();
                        }
                        agenda.exibirData(LocalDate.of(2026,8,31));
                        javafx.scene.layout.VBox painel=(javafx.scene.layout.VBox)root.lookup("#listaEventosPainel");
                        javafx.scene.layout.VBox card=(javafx.scene.layout.VBox)painel.getChildren().stream()
                                .filter(n->n.getUserData() instanceof EventoAgenda e && e.getTitulo().equals("Compromisso pela tela")).findFirst().orElseThrow();
                        fecharProximoDialogo(ButtonType.OK,p->((TextField)p.lookup("#txtEventoTitulo")).setText("Compromisso editado pela tela"));
                        ((Button)card.lookupAll(".button").stream().filter(n->n instanceof Button b && b.getText().equals("Editar")).findFirst().orElseThrow()).fire();
                        verifica(eventos.listar(LocalDate.of(2026,8,31),LocalDate.of(2026,8,31)).stream()
                                .anyMatch(e->e.getTitulo().equals("Compromisso editado pela tela")
                                        && e.getHoraInicio().equals(LocalTime.of(14,0,30))
                                        && e.getHoraFim().equals(LocalTime.of(15,0,45))),
                                "agenda edita título pela tela sem alterar segundos legados");
                        card=(javafx.scene.layout.VBox)painel.getChildren().stream().filter(n->n.getUserData() instanceof EventoAgenda e && e.getTitulo().equals("Compromisso editado pela tela")).findFirst().orElseThrow();
                        fecharProximoDialogo(ButtonType.YES);
                        ((Button)card.lookupAll(".button").stream().filter(n->n instanceof Button b && b.getText().equals("Cancelar")).findFirst().orElseThrow()).fire();
                        verifica(eventos.listar(LocalDate.of(2026,8,31),LocalDate.of(2026,8,31)).stream().anyMatch(e->e.getStatus()==EventoAgenda.Status.CANCELADO && e.getTitulo().equals("Compromisso editado pela tela")),"cancelamento preserva compromisso no histórico");
                        ((CheckBox)root.lookup("#chkCancelados")).setSelected(true);
                        card=(javafx.scene.layout.VBox)painel.getChildren().stream().filter(n->n.getUserData() instanceof EventoAgenda e && e.getTitulo().equals("Compromisso editado pela tela")).findFirst().orElseThrow();
                        fecharProximoDialogo(ButtonType.YES);
                        ((Button)card.lookupAll(".button").stream().filter(n->n instanceof Button b && b.getText().equals("Excluir")).findFirst().orElseThrow()).fire();
                        verifica(eventos.listar(LocalDate.of(2026,8,31),LocalDate.of(2026,8,31)).stream().noneMatch(e->e.getTitulo().equals("Compromisso editado pela tela")),"agenda exclui compromisso após confirmação");
                        fecharProximoDialogo(ButtonType.OK,p-> {
                            ((TextField)p.lookup("#txtEventoTitulo")).setText("Visita domiciliar de acompanhamento");
                            ((TextField)p.lookup("#txtEventoLocal")).setText("Instituto C.A.S.A.");
                        });
                        agenda.abrirModalNovoEvento(new ActionEvent(root,root));
                        root.applyCss(); root.layout();
                    }
                    if(controller instanceof RelatoriosController relatorios) {
                        @SuppressWarnings("unchecked")
                        ComboBox<Adolescente> adolescente=(ComboBox<Adolescente>)root.lookup("#cmbAdolescente");
                        adolescente.setValue(adolescente.getItems().stream().filter(a->a.getCpf()==CPF).findFirst().orElseThrow());
                        ((DatePicker)root.lookup("#dpCompetencia")).setValue(JANEIRO.atDay(1)); relatorios.gerarNovoRelatorio();
                        verifica(((TextField)root.lookup("#campoNOME")).getText().equals("Cadastro validado pela tela")
                                && ((TextArea)root.lookup("#txtRegistroFrequencia")).getText().contains("06/01/2025"),"relatório prepara identificação e frequência da competência correta");
                        ((TextField)root.lookup("#campoPROCESSO")).setText("0000000-00.2025.8.16.0000");
                        ((TextArea)root.lookup("#txtDescumprimento")).setText("Contato com a família registrado pela equipe em 15/01/2025. Acordo de comparecimento às atividades, conforme avaliação técnica.");
                        relatorios.salvarAtualizacaoRelatorio();
                        RelatorioAcompanhamentoDAO rdao=new RelatorioAcompanhamentoDAO();
                        var salvo=rdao.listar().getFirst();
                        verifica(salvo.getCpfAdolescente()==CPF && salvo.getCompetencia().equals(JANEIRO)
                                && salvo.getCampo(RelatorioAcompanhamento.Campo.PROCESSO).contains("2025"),"rascunho de relatório persiste dados revisados e competência");
                        ((TextField)root.lookup("#campoFILIACAO")).setText("Filiação revisada pela equipe"); relatorios.salvarAtualizacaoRelatorio();
                        verifica(rdao.listar().size()==1 && rdao.listar().getFirst().getCampo(RelatorioAcompanhamento.Campo.FILIACAO).equals("Filiação revisada pela equipe"),"editar rascunho atualiza o banco sem duplicar relatório");
                        relatorios.exportarPara(Path.of("target/verificacao/relatorio-acompanhamento.txt"));
                        String textoExportado=Files.readString(Path.of("target/verificacao/relatorio-acompanhamento.txt"));
                        verifica(textoExportado.contains("Filiação revisada pela equipe") && textoExportado.contains("0000000-00.2025.8.16.0000")
                                && textoExportado.contains("06/01/2025") && textoExportado.contains("Acordo de comparecimento"),"exportação TXT preserva os dados revisados e as duas seções em UTF-8");
                        fecharProximoDialogo(ButtonType.CLOSE, painel -> {
                            ScrollPane scroll=(ScrollPane)painel.getContent();
                            var paginas=(javafx.scene.layout.VBox)scroll.getContent();
                            var papel=(javafx.scene.layout.VBox)paginas.getChildren().getFirst();
                            verifica(Math.abs(papel.getPrefWidth()-javafx.print.Paper.A4.getWidth())<1,
                                    "prévia apresenta folha A4 com margens");
                            var img=painel.snapshot(null,null);
                            var png=new BufferedImage((int)img.getWidth(),(int)img.getHeight(),BufferedImage.TYPE_INT_ARGB);
                            for(int y=0;y<png.getHeight();y++) for(int x=0;x<png.getWidth();x++) png.setRGB(x,y,img.getPixelReader().getArgb(x,y));
                            try { ImageIO.write(png,"png",Path.of("target/verificacao/PreviaPaginas.png").toFile()); }
                            catch(java.io.IOException e) { throw new IllegalStateException(e); }
                        }); relatorios.visualizarRelatorio();
                        salvo=rdao.listar().getFirst();
                        verifica(rdao.excluir(salvo.getIdRelatorio()) && rdao.listar().isEmpty(),"exclusão de rascunho remove o registro do banco");
                        root.applyCss(); root.layout();
                    }
                    if(controller instanceof DetalhesAdolescenteController perfilPia) {
                        ((Button)root.lookup("#btnAbaPia")).fire();
                        CheckBox enviado=(CheckBox)root.lookup("#chkEnvioPia");
                        verifica(enviado!=null && root.lookup("#txtPiaDiagnostico")==null,"interface PIA contém apenas confirmação");
                        enviado.setSelected(false);
                        fecharProximoDialogo(ButtonType.OK);
                        perfilPia.salvarConfirmacaoPia();
                        verifica(!new PIADAO().consultarEnvio(CPF),"interface grava confirmação de envio");
                    }
                    WritableImage imagem=root.snapshot(null,null);
                    BufferedImage png=new BufferedImage((int)imagem.getWidth(),(int)imagem.getHeight(),BufferedImage.TYPE_INT_ARGB);
                    for(int y=0;y<png.getHeight();y++) for(int x=0;x<png.getWidth();x++) png.setRGB(x,y,imagem.getPixelReader().getArgb(x,y));
                    ImageIO.write(png,"png",Path.of("target/verificacao/"+nome+".png").toFile());
                    verifica(true,"FXML carregado e renderizado: "+nome);
                    if (controller instanceof RegistroFrequenciaController registro) {
                        var lista=(ListView<Frequencia>)root.lookup("#listaRegistros");
                        new FrequenciaDAO().excluir(lista.getSelectionModel().getSelectedItem());
                        fecharProximoDialogo(ButtonType.YES);
                        registro.excluir();
                        verifica(((Label)root.lookup("#lblMensagem")).getText().contains("já foi removido"),
                                "interface informa exclusão anterior sem confirmar nova remoção");
                        LocalDate data = ((DatePicker)root.lookup("#dpData")).getValue();
                        verifica(new FrequenciaDAO().listar(CPF,data,data).isEmpty(),
                                "botão JavaFX exclui a frequência após confirmação");
                    }
                }
                verificarInterfacePermissoesOperacionais();
                VerificacaoImpressao.executar();
                fim.complete(null);
            } catch(Throwable t) { fim.completeExceptionally(t); }
        });
        try { fim.get(180,TimeUnit.SECONDS); } finally { Platform.exit(); }
    }

    private static void verificarInterfacePermissoesOperacionais() throws Exception {
        Stage janela = new Stage();
        try {
            VerificacaoContas.entrar("agenda_teste", "senha fictícia");
            Parent lista = FXMLLoader.load(VerificacaoCadastroFrequencia.class.getResource("/View/AdolescentesView.fxml"));
            janela.setScene(new Scene(lista,1280,850));
            ((Button)lista.lookup("#btnNovoAdolescente")).fire();
            Parent cadastro = janela.getScene().getRoot();
            cadastro.applyCss(); cadastro.layout();
            var protegida = (Parent)cadastro.lookup("#secaoProtegida");
            verifica(cadastro!=lista && protegida!=null && protegida.isVisible() && protegida.isManaged()
                    && !protegida.isDisabled() && !cadastro.lookup("#lblAvisoOperacional").isVisible(),
                    "cadastro técnico expõe os campos do fluxo completo");
            verifica(!cadastro.lookup("#cmbStatus").isVisible() && ((Label)cadastro.lookup("#lblSituacao")).getText().equals("Ativo"),
                    "tela técnica apresenta situação inicial fixa");
            var obrigatorios = cadastro.lookupAll(".form-label-mini").stream().filter(Label.class::isInstance)
                    .map(Label.class::cast).filter(l -> l.getText().endsWith("*"))
                    .filter(l -> {
                        for (javafx.scene.Node no=l; no!=null; no=no.getParent())
                            if (!no.isVisible()) return false;
                        return true;
                    }).map(Label::getText).collect(java.util.stream.Collectors.toSet());
            verifica(obrigatorios.containsAll(Set.of("NOME COMPLETO *","CPF *","DATA DE NASCIMENTO *","TELEFONE/CONTATO *",
                    "ENDEREÇO *","BAIRRO / COMUNIDADE *","RENDA FAMILIAR (SALÁRIOS MÍNIMOS) *")),
                    "cadastro técnico mantém as exigências do formulário completo");
            ((TextField)cadastro.lookup("#txtCpf")).setText("52998224725");
            ((TextField)cadastro.lookup("#txtNome")).setText("Cadastro técnico pela interface");
            ((TextField)cadastro.lookup("#txtContato")).setText("43999995555");
            ((DatePicker)cadastro.lookup("#dpNascimento")).setValue(LocalDate.of(2009,1,15));
            ((TextField)cadastro.lookup("#txtEndereco")).setText("Rua fictícia da equipe");
            ((TextField)cadastro.lookup("#txtBairro")).setText("Bairro fictício");
            ((TextField)cadastro.lookup("#txtRenda")).setText("1,5");
            ((TextField)cadastro.lookup("#txtUbs")).setText("UBS fictícia");
            fecharProximoDialogo(ButtonType.OK);
            ((Button)cadastro.lookupAll(".button").stream()
                    .filter(n -> n instanceof Button b && "Salvar cadastro".equalsIgnoreCase(b.getText()))
                    .findFirst().orElseThrow()).fire();
            verifica(new AdolescenteDAO().buscarPorCpf(52998224725L).getStatus()==StatusAdolescente.ATIVO,
                    "cadastro técnico pela interface grava adolescente ativo");
            var socialTecnica = new SituacaoSocialDAO().buscarPorCpf(52998224725L);
            verifica(socialTecnica.getEndereco().equals("Rua fictícia da equipe") && socialTecnica.getRendaFamiliar()==1.5
                    && "UBS fictícia".equals(new SaudeDAO().buscarPorCpf(52998224725L).getUbsReferencia()),
                    "cadastro técnico grava endereço, renda e saúde");
            socialTecnica.setEndereco("Rua fictícia atualizada");
            var daoCadastro = new AdolescenteDAO();
            verifica(daoCadastro.salvarCadastro(daoCadastro.buscarPorCpf(52998224725L), socialTecnica,
                    new SaudeDAO().buscarPorCpf(52998224725L),
                    new EducacaoTrabalhoDAO().buscarPorCpf(52998224725L),true,false)
                    && new SituacaoSocialDAO().buscarPorCpf(52998224725L).getEndereco().equals("Rua fictícia atualizada")
                    && new SituacaoSocialDAO().buscarPorCpf(52998224725L).getRendaFamiliar()==1.5
                    && "UBS fictícia".equals(new SaudeDAO().buscarPorCpf(52998224725L).getUbsReferencia()),
                    "edição técnica do endereço preserva renda e saúde");

            FXMLLoader detalhes = new FXMLLoader(VerificacaoCadastroFrequencia.class.getResource("/View/DetalhesAdolescenteView.fxml"));
            Parent perfil = detalhes.load(); janela.getScene().setRoot(perfil);
            perfil.applyCss(); perfil.layout();
            ((DetalhesAdolescenteController)detalhes.getController()).carregarDados(new AdolescenteDAO().listarResumoDTO().stream()
                    .filter(a -> a.getCpf().replaceAll("[^0-9]","").equals(Long.toString(CPF))).findFirst().orElseThrow());
            ((Button)perfil.lookup("#btnAbaMedida")).fire();
            @SuppressWarnings("unchecked")
            ComboBox<MedidaSocioeducativa> medidas=(ComboBox<MedidaSocioeducativa>)perfil.lookup("#cmbMedidaPerfil");
            medidas.setValue(medidas.getItems().stream().filter(m -> m.getDataFim()!=null).findFirst().orElseThrow());
            verifica(perfil.lookup("#btnEditarMedida").isVisible() && perfil.lookup("#btnDefinirReferencia").isVisible()
                    && ((ComboBox<?>)perfil.lookup("#cmbTecnico")).getItems().stream().anyMatch(it ->
                            it instanceof EquipeTecnica e && e.getCpf()==77777777770L && e.getLogin()==null),
                    "perfil técnico oferece edição da medida encerrada e seletor reduzido de referência");
            ((Button)perfil.lookup("#btnAbaFamilia")).fire();
            verifica(perfil.lookup("#btnAbaPia").isVisible() && AutorizacaoService.pode(Operacao.FAMILIA_REMOVER),
                    "perfil técnico oferece PIA e tem permissão de desvincular");

            Parent planilha = FXMLLoader.load(VerificacaoCadastroFrequencia.class.getResource("/View/FrequenciaView.fxml"));
            janela.getScene().setRoot(planilha);
            planilha.applyCss(); planilha.layout();
            var tabela = (TableView<?>)planilha.lookup("#tabela");
            tabela.getSelectionModel().selectFirst();
            var relatorio=(Button)planilha.lookup("#btnRelatorioOperacional");
            verifica(relatorio.isVisible() && !relatorio.isDisabled(),"planilha técnica oferece relatório operacional para a seleção");
            fecharProximoDialogo(ButtonType.CLOSE, painel -> {
                var texto=(TextArea)painel.getContent();
                verifica(!texto.isEditable() && texto.getText().contains("uso interno"),"relatório operacional abre prévia interna");
            });
            relatorio.fire();

            NavegacaoUtil.exigirTela("/View/RelatoriosView.fxml");
            FXMLLoader editorRelatorio = new FXMLLoader(VerificacaoCadastroFrequencia.class.getResource("/View/RelatoriosView.fxml"));
            Parent telaRelatorio = editorRelatorio.load();
            janela.getScene().setRoot(telaRelatorio);
            telaRelatorio.applyCss(); telaRelatorio.layout();
            @SuppressWarnings("unchecked")
            var seleciona = (ComboBox<Adolescente>)telaRelatorio.lookup("#cmbAdolescente");
            seleciona.setValue(seleciona.getItems().stream().filter(jovem -> jovem.getCpf()==52998224725L).findFirst().orElseThrow());
            RelatoriosController controlador = editorRelatorio.getController();
            controlador.gerarNovoRelatorio();
            verifica(((TextField)telaRelatorio.lookup("#campoENDERECO")).getText().contains("Rua fictícia atualizada"),
                    "relatório completo da Equipe Técnica recebe endereço");
            ((TextField)telaRelatorio.lookup("#campoPROCESSO")).setText("Processo fictício");
            ((TextArea)telaRelatorio.lookup("#txtDescumprimento")).setText("Registro fictício de acompanhamento.");
            controlador.salvarAtualizacaoRelatorio();
            verifica(new RelatorioAcompanhamentoDAO().listar().stream().anyMatch(r ->
                    r.getCpfAdolescente()==52998224725L && r.getCampo(RelatorioAcompanhamento.Campo.PROCESSO).equals("Processo fictício")),
                    "Equipe Técnica persiste o relatório completo");
            fecharProximoDialogo(ButtonType.CLOSE);
            controlador.visualizarRelatorio();
            Path exportacao = Files.createTempFile("casa-relatorio-tecnico-", ".txt");
            try {
                controlador.exportarPara(exportacao);
                verifica(Files.readString(exportacao).contains("Rua fictícia atualizada"),
                        "Equipe Técnica exporta relatório completo");
                Sessao.limparSessao();
                rejeita(SecurityException.class,()-> {
                    try { controlador.exportarPara(exportacao); }
                    catch (java.io.IOException erro) { throw new UncheckedIOException(erro); }
                },"exportação técnica exige sessão válida");
                rejeita(SecurityException.class,()->ImpressaoRelatorio.imprimirModelo(janela,new RelatorioAcompanhamento()),
                        "impressão técnica exige sessão válida");
            } finally { Files.deleteIfExists(exportacao); }

            VerificacaoContas.entrar("educador-teste", VerificacaoContas.SENHA);
            Parent frequencia = FXMLLoader.load(VerificacaoCadastroFrequencia.class.getResource("/View/FrequenciaView.fxml"));
            verifica(!frequencia.lookup("#btnRelatorioOperacional").isVisible()
                    && ((Button)frequencia.lookup("#btnRegistrar")).getText().equals("Registrar frequência"),
                    "planilha do Educador oferece registro sem correção ou relatório");
            FXMLLoader editor = new FXMLLoader(VerificacaoCadastroFrequencia.class.getResource("/View/RegistroFrequenciaView.fxml"));
            Parent formulario = editor.load();
            ((RegistroFrequenciaController)editor.getController()).carregar(CPF,LocalDate.now(),()->{});
            verifica(!formulario.lookup("#btnExcluirLancamento").isVisible() && !formulario.lookup("#btnNovaAtividade").isVisible()
                    && !formulario.lookup("#txtHoras").isVisible() && !formulario.lookup("#cmbMedida").isVisible(),
                    "editor do Educador omite ações e campos vedados");
        } finally {
            janela.close();
            VerificacaoContas.entrar("admin-teste", VerificacaoContas.SENHA);
        }
    }

    private static void verificarLoginELogout() throws Exception {
        Stage janela = new Stage();
        try {
            var url = VerificacaoCadastroFrequencia.class.getResource("/View/Login.fxml");
            for (var perfil : NivelAcesso.values()) {
                String loginTeste = switch (perfil) {
                    case ADMINISTRADOR -> "admin-teste";
                    case EQUIPE_TECNICA -> "agenda_teste";
                    case EDUCADOR -> "educador-teste";
                };
                Parent login = FXMLLoader.load(url);
                janela.setScene(new Scene(login));
                ((TextField)login.lookup("#txtLogin")).setText(loginTeste);
                ((PasswordField)login.lookup("#txtSenha")).setText(perfil == NivelAcesso.EQUIPE_TECNICA
                        ? "senha fictícia" : VerificacaoContas.SENHA);
                ((Button)login.lookup("#btnEntrar")).fire();
                Parent destino = janela.getScene().getRoot();
                verifica(destino != login && br.edu.unespar.trabalho.util.Sessao.validarContaAtual()
                        && br.edu.unespar.trabalho.util.Sessao.getUsuarioLogado().getPerfil() == perfil,
                        "login pela tela com conta válida de " + perfil);
                verifica(destino.lookup(perfil == NivelAcesso.EDUCADOR ? "#tabela" : "#vboxAdolescentes") != null,
                        "navegação inicial de " + perfil);
                Button equipe = (Button) destino.lookupAll(".button").stream()
                        .filter(no -> no instanceof Button b && "Equipe Técnica".equals(b.getText()))
                        .findFirst().orElseThrow();
                verifica(equipe.isVisible() == (perfil == NivelAcesso.ADMINISTRADOR)
                        && equipe.isManaged() == equipe.isVisible(), "menu da equipe para " + perfil);
                ((Button)destino.lookupAll(".logout-button").stream()
                        .filter(no -> no instanceof Button b && "Sair do sistema".equals(b.getText()))
                        .findFirst().orElseThrow()).fire();
                verifica(br.edu.unespar.trabalho.util.Sessao.getUsuarioLogado() == null
                        && janela.getScene().getRoot().lookup("#btnEntrar") != null, "logout de " + perfil);
            }
        } finally {
            janela.close();
            VerificacaoContas.entrar("admin-teste", VerificacaoContas.SENHA);
        }
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
