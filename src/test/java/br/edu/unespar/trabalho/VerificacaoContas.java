package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.dao.*;
import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.service.*;
import br.edu.unespar.trabalho.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;

/** Testes de contas no schema descartável criado por VerificacaoCadastroFrequencia. */
final class VerificacaoContas {
    static final String SENHA = "senha exclusivamente fictícia";
    private static final String NOVA = "outra senha exclusivamente fictícia";

    static void executar() throws Exception {
        var dao = new EquipeTecnicaDAO();
        var auth = new AuthServiceImpl();
        EquipeTecnica admin = conta(85555555551L, "admin-teste", NivelAcesso.ADMINISTRADOR);
        EquipeTecnica educador = conta(85555555552L, "educador-teste", NivelAcesso.EDUCADOR);
        assert !Senhas.gerarHash(SENHA).equals(Senhas.gerarHash(SENHA));
        String migracao = Files.readString(Path.of("sql/usuarios_seguranca.sql"));
        try (var c = ConnectionFactory.getConnection(); var s = c.createStatement()) {
            s.execute(migracao);
            s.execute(migracao);
            // Guarda o registro legado da agenda e testa o bootstrap com a tabela vazia.
            s.execute("CREATE TABLE conta_legada_teste AS SELECT * FROM EquipeTecnica");
            s.execute("DELETE FROM EquipeTecnica");
        }
        assert dao.criarPrimeiroAdministrador(admin);
        rejeitar(() -> dao.criarPrimeiroAdministrador(admin), IllegalStateException.class);
        try (var c = ConnectionFactory.getConnection(); var s = c.createStatement()) {
            s.execute("INSERT INTO EquipeTecnica SELECT * FROM conta_legada_teste");
            s.execute("INSERT INTO Acompanhamento(cpf_adolescente,cpf_equipe,tecnico_referencia) VALUES(10000000001,77777777770,true)");
        }
        assert auth.autenticar("agenda_teste", "senha fictícia") == null;
        assert dao.migrarSenhasLegadas() == 1;
        assert dao.migrarSenhasLegadas() == 0;
        entrar("agenda_teste", "senha fictícia");
        VerificacaoAutorizacao.verificarPerfil(NivelAcesso.EQUIPE_TECNICA);
        assert new PerfilAdolescenteDAO().buscarCpfTecnicoReferencia(10000000001L) == 77777777770L;
        assert auth.autenticar(admin.getLogin(), "incorreta") == null;
        var sessaoAdmin = auth.autenticar(" ADMIN-TESTE ", SENHA);
        assert sessaoAdmin != null && sessaoAdmin.getNomeCompleto().equals(admin.getNomeCompleto());
        entrar(admin.getLogin(), SENHA);
        VerificacaoAutorizacao.verificarPerfil(NivelAcesso.ADMINISTRADOR);
        rejeitar(() -> dao.definirAtivo(admin.getCpf(),false), IllegalArgumentException.class);
        admin.setNivelAcesso(NivelAcesso.EDUCADOR);
        rejeitar(() -> dao.atualizar(admin), IllegalArgumentException.class);
        assert dao.inserir(educador);
        rejeitar(() -> dao.inserir(conta(85555555553L," EDUCAdor-teste ",NivelAcesso.EDUCADOR)),
                IllegalArgumentException.class);
        assert dao.buscarPorCpf(85555555553L) == null;
        try (var c = ConnectionFactory.getConnection(); var s = c.createStatement();
             var r = s.executeQuery("SELECT senha FROM EquipeTecnica WHERE login='educador-teste'")) {
            assert r.next() && !r.getString(1).contains(SENHA) && Senhas.verificar(SENHA,r.getString(1));
        }
        var sessaoEducador = auth.autenticar(educador.getLogin(),SENHA);
        educador.setNomeCompleto("Profissional fictício editado");
        educador.setSenha(null);
        assert dao.atualizar(educador) && !dao.sessaoValida(sessaoEducador);
        assert auth.autenticar(educador.getLogin(),SENHA).getNomeCompleto().equals(educador.getNomeCompleto());
        assert dao.definirAtivo(educador.getCpf(),false);
        assert auth.autenticar(educador.getLogin(),SENHA) == null;
        assert dao.definirAtivo(educador.getCpf(),true);
        dao.redefinirSenha(educador.getCpf(),NOVA);
        assert auth.autenticar(educador.getLogin(),SENHA) == null;
        entrar(educador.getLogin(), NOVA);
        VerificacaoAutorizacao.verificarPerfil(NivelAcesso.EDUCADOR);
        assert !new FrequenciaMensalDAO().listar(YearMonth.of(2024,1)).isEmpty();
        rejeitar(dao::listar,AcessoNegadoException.class);
        rejeitar(() -> new SaudeDAO().buscarPorCpf(10000000001L),AcessoNegadoException.class);
        var frequencia = new Frequencia();
        frequencia.setCpfAdolescente(10000000001L);
        frequencia.setIdAtividade(1);
        frequencia.setDataPresenca(LocalDate.of(2024,1,3));
        frequencia.setStatusPresenca(StatusPresenca.PRESENTE);
        frequencia.setHorasCumpridas(1);
        rejeitar(() -> new FrequenciaDAO().registrar(frequencia), AcessoNegadoException.class);
        frequencia.setHorasCumpridas(0);
        assert new FrequenciaDAO().registrar(frequencia);
        rejeitar(() -> new FrequenciaDAO().excluir(frequencia), AcessoNegadoException.class);
        rejeitar(() -> dao.trocarMinhaSenha("incorreta",SENHA), IllegalArgumentException.class);
        dao.trocarMinhaSenha(NOVA,SENHA);
        assert !Sessao.validarContaAtual() && Sessao.getUsuarioLogado() == null;
        assert auth.autenticar(educador.getLogin(),SENHA) != null;
        Sessao.configurarInatividade(Duration.ofNanos(1));
        entrar(admin.getLogin(), SENHA);
        assert !AutorizacaoService.pode(Operacao.PAINEL_CONSULTAR);
        Sessao.configurarInatividade(Duration.ofMinutes(15));
        entrar(admin.getLogin(), SENHA);
        assert new FrequenciaDAO().excluir(frequencia);
        System.out.println("OK: migração, contas, senhas, sessões e acesso por perfil.");
    }

    static void entrar(String login, String senha) throws Exception {
        var usuario = Sessao.entrar(login, senha);
        if (usuario == null) throw new AssertionError("Falha no login da conta fictícia.");
    }

    static EquipeTecnica conta(long cpf,String login,NivelAcesso perfil) {
        var membro = new EquipeTecnica();
        membro.setCpf(cpf); membro.setLogin(login); membro.setSenha(SENHA);
        membro.setNomeCompleto("Profissional fictício"); membro.setCargoFuncao("Teste");
        membro.setDataNascimento(LocalDate.of(1990,1,1)); membro.setNivelAcesso(perfil);
        return membro;
    }

    private static void rejeitar(Runnable acao,Class<? extends RuntimeException> tipo) {
        try { acao.run(); }
        catch (RuntimeException e) {
            if (tipo.isInstance(e)) return;
            throw e;
        }
        throw new AssertionError("Operação deveria ser rejeitada.");
    }
}
