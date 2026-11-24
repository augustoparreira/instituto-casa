package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.service.AutorizacaoService;
import br.edu.unespar.trabalho.service.AcessoNegadoException;
import br.edu.unespar.trabalho.util.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import static br.edu.unespar.trabalho.model.Operacao.*;

/** Persistência de contas, autenticação e preservação dos vínculos profissionais. */
public class EquipeTecnicaDAO {
    private static final String CAMPOS = "p.cpf,p.nome_completo,p.data_nascimento,p.contato,p.email,"
            + "e.login,e.cargo_funcao,e.nivel_acesso,e.ativo,e.versao_sessao";
    private static final String ORIGEM = " FROM Pessoa p JOIN EquipeTecnica e ON p.cpf=e.cpf_equipe ";

    public EquipeTecnica autenticar(String login, String senha) {
        if (login == null || senha == null) return null;
        try (Connection c = ConnectionFactory.getConnection(); PreparedStatement s = c.prepareStatement(
                "SELECT " + CAMPOS + ",e.senha,e.senha_formato" + ORIGEM + "WHERE lower(btrim(e.login))=?")) {
            s.setString(1, normalizarLogin(login));
            try (ResultSet r = s.executeQuery()) {
                if (!r.next() || !r.getBoolean("ativo") || !"PBKDF2".equals(r.getString("senha_formato"))
                        || !Senhas.verificar(senha, r.getString("senha"))) return null;
                return mapear(r);
            }
        } catch (SQLException e) { throw falha(e); }
    }

    /** Consulta restrita à identidade autenticada; não retorna dados nem senha. */
    public boolean sessaoValida(EquipeTecnicaDTO usuario) {
        if (usuario == null || usuario.getPerfil() == null) return false;
        try (Connection c = ConnectionFactory.getConnection()) { return sessaoValida(c, usuario); }
        catch (SQLException e) { throw falha(e); }
    }

    private boolean sessaoValida(Connection c, EquipeTecnicaDTO usuario) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT 1 FROM EquipeTecnica WHERE cpf_equipe=? "
                + "AND ativo AND versao_sessao=? AND nivel_acesso=? AND senha_formato='PBKDF2'")) {
            s.setLong(1, usuario.getCpfEquipe()); s.setLong(2, usuario.getVersaoSessao());
            s.setString(3, usuario.getPerfil().getCodigo());
            try (ResultSet r=s.executeQuery()) { return r.next(); }
        }
    }

    public List<EquipeTecnica> listar() {
        AutorizacaoService.exigir(EQUIPE_CONSULTAR);
        List<EquipeTecnica> lista = new ArrayList<>();
        try (Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(
                "SELECT " + CAMPOS + ORIGEM + "ORDER BY p.nome_completo"); ResultSet r=s.executeQuery()) {
            while(r.next()) lista.add(mapear(r));
            return lista;
        } catch (SQLException e) { throw falha(e); }
    }

    /** Agenda consulta apenas os dados públicos de trabalho necessários ao vínculo. */
    public List<EquipeTecnica> listarParaAgenda() {
        AutorizacaoService.exigir(AGENDA_CONSULTAR);
        List<EquipeTecnica> profissionais = new ArrayList<>();
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement consulta = conexao.prepareStatement(
                     "SELECT p.cpf,p.nome_completo,e.cargo_funcao" + ORIGEM + "ORDER BY p.nome_completo");
             ResultSet resultado = consulta.executeQuery()) {
            while (resultado.next()) {
                EquipeTecnica profissional = new EquipeTecnica();
                profissional.setCpf(resultado.getLong(1));
                profissional.setNomeCompleto(resultado.getString(2));
                profissional.setCargoFuncao(resultado.getString(3));
                profissionais.add(profissional);
            }
            return profissionais;
        } catch (SQLException e) { throw falha(e); }
    }

    public EquipeTecnica buscarPorCpf(long cpf) {
        AutorizacaoService.exigir(EQUIPE_CONSULTAR);
        try (Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(
                "SELECT " + CAMPOS + ORIGEM + "WHERE p.cpf=?")) {
            s.setLong(1,cpf);
            try(ResultSet r=s.executeQuery()) { return r.next() ? mapear(r) : null; }
        } catch(SQLException e) { throw falha(e); }
    }

    public boolean inserir(EquipeTecnica membro) { return salvar(membro,false,false); }
    public boolean atualizar(EquipeTecnica membro) { return salvar(membro,true,false); }

    /** Instalação local: permitida apenas quando nenhuma conta existe. Nunca usada pela tela de login. */
    public boolean criarPrimeiroAdministrador(EquipeTecnica membro) {
        if (membro.getNivelAcesso()!=NivelAcesso.ADMINISTRADOR || !membro.isAtivo())
            throw new IllegalArgumentException("A primeira conta deve ser Administrador ativo.");
        return salvar(membro,false,true);
    }

    private boolean salvar(EquipeTecnica m, boolean edicao, boolean inicial) {
        if (!inicial) AutorizacaoService.exigir(USUARIO_GERENCIAR);
        validar(m);
        boolean troca = m.getSenha()!=null && !m.getSenha().isEmpty();
        if (!edicao || troca) Senhas.validarNova(m.getSenha());
        String hash = (!edicao || troca) ? Senhas.gerarHash(m.getSenha()) : null;
        try (Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                bloquearContas(c);
                if (inicial) {
                    try (Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT count(*) FROM EquipeTecnica")) {
                        r.next(); if(r.getInt(1)!=0) throw new IllegalStateException("Já existem contas; use a administração autenticada.");
                    }
                } else exigirAdministradorAtual(c);
                if(edicao) protegerUltimoAdministrador(c,m.getCpf(),m.getNivelAcesso(),m.isAtivo());
                String pessoa=edicao ? "UPDATE Pessoa SET nome_completo=?,data_nascimento=?,contato=?,email=? WHERE cpf=?"
                        : "INSERT INTO Pessoa(nome_completo,data_nascimento,contato,email,cpf) VALUES(?,?,?,?,?)";
                try(PreparedStatement s=c.prepareStatement(pessoa)) {
                    s.setString(1,m.getNomeCompleto().trim()); s.setDate(2,Date.valueOf(m.getDataNascimento()));
                    s.setString(3,m.getContato()); s.setString(4,m.getEmail()); s.setLong(5,m.getCpf());
                    if(s.executeUpdate()!=1) throw new IllegalArgumentException("Profissional não encontrado.");
                }
                String equipe=edicao ? "UPDATE EquipeTecnica SET login=?,cargo_funcao=?,nivel_acesso=?,ativo=?,"
                        + "senha=COALESCE(?,senha),senha_formato=CASE WHEN ? IS NOT NULL THEN 'PBKDF2' ELSE senha_formato END,"
                        + "versao_sessao=versao_sessao+1 WHERE cpf_equipe=?"
                        : "INSERT INTO EquipeTecnica(login,cargo_funcao,nivel_acesso,ativo,senha,senha_formato,cpf_equipe) VALUES(?,?,?,?,?,'PBKDF2',?)";
                try(PreparedStatement s=c.prepareStatement(equipe)) {
                    s.setString(1,normalizarLogin(m.getLogin())); s.setString(2,m.getCargoFuncao().trim());
                    s.setString(3,m.getNivelAcesso().getCodigo()); s.setBoolean(4,m.isAtivo()); s.setString(5,hash);
                    if(edicao) { s.setString(6,hash); s.setLong(7,m.getCpf()); } else s.setLong(6,m.getCpf());
                    if(s.executeUpdate()!=1) throw new IllegalArgumentException("Conta não encontrada.");
                }
                c.commit(); return true;
            } catch(SQLException | RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw falha(e); }
    }

    public boolean definirAtivo(long cpf, boolean ativo) {
        AutorizacaoService.exigir(USUARIO_GERENCIAR);
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                bloquearContas(c); exigirAdministradorAtual(c);
                // O perfil atual é verificado no bloqueio; desativação nunca apaga a pessoa.
                protegerUltimoAdministrador(c,cpf,null,ativo);
                try(PreparedStatement s=c.prepareStatement("UPDATE EquipeTecnica SET ativo=?,versao_sessao=versao_sessao+1 WHERE cpf_equipe=?")) {
                    s.setBoolean(1,ativo); s.setLong(2,cpf);
                    if(s.executeUpdate()!=1) throw new IllegalArgumentException("Conta não encontrada.");
                }
                c.commit(); return true;
            } catch(SQLException | RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw falha(e); }
    }

    /** Compatibilidade: excluir conta agora desativa sem excluir registros históricos. */
    public boolean excluir(long cpf) { return definirAtivo(cpf,false); }

    public void redefinirSenha(long cpf, String novaSenha) {
        AutorizacaoService.exigir(USUARIO_GERENCIAR);
        alterarSenha(cpf,null,novaSenha,true);
    }

    public void trocarMinhaSenha(String atual, String nova) {
        AutorizacaoService.exigir(SENHA_PROPRIA_ALTERAR);
        alterarSenha(Sessao.getUsuarioLogado().getCpfEquipe(),atual,nova,false);
    }

    private void alterarSenha(long cpf,String atual,String nova,boolean administrativa) {
        Senhas.validarNova(nova); String hash=Senhas.gerarHash(nova);
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                bloquearContas(c);
                if(administrativa) exigirAdministradorAtual(c);
                else if(!sessaoValida(c,Sessao.getUsuarioLogado())) throw new AcessoNegadoException();
                try(PreparedStatement s=c.prepareStatement("SELECT senha FROM EquipeTecnica WHERE cpf_equipe=?")) {
                    s.setLong(1,cpf);
                    try(ResultSet r=s.executeQuery()) {
                        if(!r.next()) throw new IllegalArgumentException("Conta não encontrada.");
                        if(!administrativa && !Senhas.verificar(atual,r.getString(1)))
                            throw new IllegalArgumentException("Senha atual incorreta.");
                    }
                }
                try(PreparedStatement s=c.prepareStatement("UPDATE EquipeTecnica SET senha=?,senha_formato='PBKDF2',versao_sessao=versao_sessao+1 WHERE cpf_equipe=?")) {
                    s.setString(1,hash); s.setLong(2,cpf); s.executeUpdate();
                }
                c.commit();
            } catch(SQLException | RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw falha(e); }
    }

    /** Conversão offline transacional; não existe fallback de senha em texto no login. */
    public int migrarSenhasLegadas() {
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                bloquearContas(c); int total=0;
                try(Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT cpf_equipe,senha FROM EquipeTecnica WHERE senha_formato='LEGADO'");
                    PreparedStatement u=c.prepareStatement("UPDATE EquipeTecnica SET senha=?,senha_formato='PBKDF2',versao_sessao=versao_sessao+1 WHERE cpf_equipe=?")) {
                    while(r.next()) { u.setString(1,Senhas.gerarHash(r.getString(2))); u.setLong(2,r.getLong(1)); u.executeUpdate(); total++; }
                }
                c.commit(); return total;
            } catch(SQLException | RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw falha(e); }
    }

    private void bloquearContas(Connection c) throws SQLException {
        try(Statement s=c.createStatement()) { s.execute("LOCK TABLE EquipeTecnica IN SHARE ROW EXCLUSIVE MODE"); }
    }

    private void exigirAdministradorAtual(Connection c) throws SQLException {
        EquipeTecnicaDTO ator=Sessao.getUsuarioLogado();
        if(ator==null || ator.getPerfil()!=NivelAcesso.ADMINISTRADOR || !sessaoValida(c,ator)) throw new AcessoNegadoException();
    }

    private void protegerUltimoAdministrador(Connection c,long cpf,NivelAcesso novoPerfil,boolean ativo) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT nivel_acesso,ativo FROM EquipeTecnica WHERE cpf_equipe=?")) {
            s.setLong(1,cpf);
            try(ResultSet r=s.executeQuery()) {
                if(!r.next()) throw new IllegalArgumentException("Conta não encontrada.");
                boolean perdeAdmin=!ativo || (novoPerfil!=null && novoPerfil!=NivelAcesso.ADMINISTRADOR);
                if(!perdeAdmin || !r.getBoolean(2) || !"ADMINISTRADOR".equals(r.getString(1))) return;
            }
        }
        try(Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT count(*) FROM EquipeTecnica WHERE ativo AND nivel_acesso='ADMINISTRADOR'")) {
            r.next(); if(r.getInt(1)<=1) throw new IllegalArgumentException("O último Administrador ativo não pode ser desativado ou perder o perfil.");
        }
    }

    private static String normalizarLogin(String login) { return login.trim().toLowerCase(Locale.ROOT); }
    private void validar(EquipeTecnica m) {
        if(m==null || m.getCpf()<=0 || m.getCpf()>99999999999L) throw new IllegalArgumentException("CPF inválido.");
        if(m.getNomeCompleto()==null || m.getNomeCompleto().isBlank() || m.getNomeCompleto().trim().length()>80)
            throw new IllegalArgumentException("Nome obrigatório, com até 80 caracteres.");
        if(m.getDataNascimento()==null || m.getDataNascimento().isAfter(LocalDate.now())) throw new IllegalArgumentException("Nascimento inválido.");
        if(m.getLogin()==null || !normalizarLogin(m.getLogin()).matches("[a-z0-9._-]{3,100}"))
            throw new IllegalArgumentException("Login: 3 a 100 letras sem acento, números, ponto, hífen ou sublinhado.");
        if(m.getCargoFuncao()==null || m.getCargoFuncao().isBlank() || m.getCargoFuncao().trim().length()>60 || m.getNivelAcesso()==null)
            throw new IllegalArgumentException("Informe cargo (até 60 caracteres) e perfil.");
        if((m.getContato()!=null && m.getContato().length()>15) || (m.getEmail()!=null && m.getEmail().length()>90))
            throw new IllegalArgumentException("Contato deve ter até 15 e e-mail até 90 caracteres.");
    }

    private EquipeTecnica mapear(ResultSet r) throws SQLException {
        EquipeTecnica e=new EquipeTecnica();
        e.setCpf(r.getLong("cpf")); e.setNomeCompleto(r.getString("nome_completo"));
        e.setDataNascimento(r.getDate("data_nascimento").toLocalDate()); e.setContato(r.getString("contato")); e.setEmail(r.getString("email"));
        e.setLogin(r.getString("login")); e.setCargoFuncao(r.getString("cargo_funcao"));
        try { e.setNivelAcesso(NivelAcesso.fromCodigo(r.getString("nivel_acesso"))); }
        catch(IllegalArgumentException ex) { throw new IllegalStateException("Perfil da conta inválido; solicite revisão administrativa."); }
        e.setAtivo(r.getBoolean("ativo")); e.setVersaoSessao(r.getLong("versao_sessao")); return e;
    }

    private RuntimeException falha(SQLException e) {
        if("23505".equals(e.getSQLState())) return new IllegalArgumentException("CPF ou login já cadastrado.");
        return new IllegalStateException("Não foi possível acessar as contas. Confira a conexão e a migração de segurança.");
    }
}
