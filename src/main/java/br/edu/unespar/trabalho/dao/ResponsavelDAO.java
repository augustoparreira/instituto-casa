package br.edu.unespar.trabalho.dao;

import static br.edu.unespar.trabalho.service.AutorizacaoService.*;
import static br.edu.unespar.trabalho.model.Operacao.*;
import br.edu.unespar.trabalho.model.Responsavel;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import br.edu.unespar.trabalho.util.ValidacaoFormulario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Persiste responsáveis e gerencia seus vínculos e parentescos com os adolescentes por JDBC.
 */
public class ResponsavelDAO {

    public boolean inserir(Responsavel r,long cpf) {
        return salvar(r,cpf,false); }
    public boolean atualizar(Responsavel r,long cpf) {
        return salvar(r,cpf,true); }
    private boolean salvar(Responsavel r,long cpf,boolean edicao) {
        exigir(FAMILIA_ALTERAR);

        if(r.getCpf()==cpf || r.getCpf()<=0 || r.getNomeCompleto()==null || r.getNomeCompleto().isBlank() || r.getDataNascimento()==null)
            throw new IllegalArgumentException("Confira os dados do responsável.");
        ValidacaoFormulario.texto(r.getParentesco(),80,"Parentesco",true);
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement s=c.prepareStatement("SELECT cpf_adolescente FROM Adolescente WHERE cpf_adolescente=? FOR UPDATE")) {
                    s.setLong(1,cpf); try(ResultSet rs=s.executeQuery()) { if(!rs.next()) throw new IllegalArgumentException("Adolescente não encontrado."); }
                }
                if(edicao) {
                    try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM Responsabiliza WHERE cpf_adolescente=? AND cpf_responsavel=? FOR UPDATE")) {
                        s.setLong(1,cpf); s.setLong(2,r.getCpf());
                        try(ResultSet rs=s.executeQuery()) {
                            if(!rs.next()) throw new IllegalArgumentException("O vínculo do responsável foi removido. Recarregue o perfil antes de editar.");
                        }
                    }
                }
                if (edicao) {
                    if (podeEditarDadosPessoais(c,r.getCpf())) {
                        try (PreparedStatement s=c.prepareStatement("UPDATE Pessoa SET nome_completo=?,data_nascimento=?,contato=?,email=? WHERE cpf=?")) {
                            preencherPessoa(s,r,ContatoPessoaDAO.validar(c,r.getCpf(),r.getContato(),true,false));
                            s.executeUpdate();
                        }
                    }
                } else if (!bloquearResponsavelReutilizavel(c,r.getCpf())) {
                    String contato = ContatoPessoaDAO.validar(c,r.getCpf(),r.getContato(),false,false);
                    int inseridas;
                    try (PreparedStatement s=c.prepareStatement("INSERT INTO Pessoa(nome_completo,data_nascimento,contato,email,cpf) VALUES(?,?,?,?,?) ON CONFLICT(cpf) DO NOTHING")) {
                        preencherPessoa(s,r,contato);
                        inseridas=s.executeUpdate();
                    }
                    if (inseridas==0 && !bloquearResponsavelReutilizavel(c,r.getCpf()))
                        throw new IllegalArgumentException("CPF já utilizado em outro tipo de cadastro.");
                }
                try(PreparedStatement s=c.prepareStatement("INSERT INTO Responsavel(cpf_responsavel,parentesco,contato_principal) VALUES(?,?,?) ON CONFLICT(cpf_responsavel) DO NOTHING")) {
                    s.setLong(1,r.getCpf()); s.setString(2,r.getParentesco()); s.setBoolean(3,r.isContatoPrincipal()); s.executeUpdate();
                }
                if(r.isContatoPrincipal()) try(PreparedStatement s=c.prepareStatement("UPDATE Responsabiliza SET principal=false WHERE cpf_adolescente=?")) { s.setLong(1,cpf); s.executeUpdate(); }
                try(PreparedStatement s=c.prepareStatement("INSERT INTO Responsabiliza(cpf_adolescente,cpf_responsavel,parentesco_vinculo,principal) VALUES(?,?,?,?) ON CONFLICT(cpf_responsavel,cpf_adolescente) DO UPDATE SET parentesco_vinculo=excluded.parentesco_vinculo,principal=excluded.principal")) {
                    s.setLong(1,cpf); s.setLong(2,r.getCpf()); s.setString(3,r.getParentesco()); s.setBoolean(4,r.isContatoPrincipal()); s.executeUpdate();
                }
                c.commit(); return true;
            } catch(SQLException|RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível salvar o responsável.",e); }
    }
    private static final String REUTILIZAVEL = "EXISTS (SELECT 1 FROM Responsavel WHERE cpf_responsavel=p.cpf) "
            + "AND NOT EXISTS (SELECT 1 FROM Adolescente WHERE cpf_adolescente=p.cpf) "
            + "AND NOT EXISTS (SELECT 1 FROM EquipeTecnica WHERE cpf_equipe=p.cpf)";

    private void preencherPessoa(PreparedStatement s,Responsavel r,String contato) throws SQLException {
        ValidacaoFormulario.texto(r.getNomeCompleto(),80,"Nome",true);
        ValidacaoFormulario.texto(r.getEmail(),90,"E-mail",false);
        ValidacaoFormulario.texto(contato,30,"Telefone",false);
        s.setString(1,r.getNomeCompleto());
        s.setObject(2,r.getDataNascimento());
        s.setString(3,contato);
        s.setString(4,r.getEmail());
        s.setLong(5,r.getCpf());
    }

    boolean bloquearResponsavelReutilizavel(Connection c,long cpf) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("SELECT cpf FROM Pessoa WHERE cpf=? FOR UPDATE")) {
            s.setLong(1,cpf);
            try (ResultSet r=s.executeQuery()) { if (!r.next()) return false; }
        }
        try (PreparedStatement s=c.prepareStatement("SELECT "+REUTILIZAVEL+" FROM Pessoa p WHERE cpf=?")) {
            s.setLong(1,cpf);
            try (ResultSet r=s.executeQuery()) { return r.next() && r.getBoolean(1); }
        }
    }

    private boolean podeEditarDadosPessoais(Connection c,long cpf) throws SQLException {
        boolean compartilhada = pode(FAMILIA_PESSOA_COMPARTILHADA_ALTERAR);
        if (compartilhada && pode(USUARIO_GERENCIAR)) return true;
        if (!bloquearResponsavelReutilizavel(c,cpf)) return false;
        if (compartilhada) return true;
        try (PreparedStatement s=c.prepareStatement("SELECT count(*)=1 FROM Responsabiliza WHERE cpf_responsavel=?")) {
            s.setLong(1,cpf);
            try (ResultSet r=s.executeQuery()) { return r.next() && r.getBoolean(1); }
        }
    }

    public boolean podeEditarDadosPessoais(long cpf) {
        exigir(FAMILIA_ALTERAR);
        try (Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try { return podeEditarDadosPessoais(c,cpf); }
            finally { c.rollback(); }
        } catch (SQLException e) { throw new IllegalStateException("Não foi possível verificar os vínculos do responsável.",e); }
    }

    public Responsavel buscarPessoa(long cpf) {
        exigir(FAMILIA_CONSULTAR);

        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("SELECT p.cpf,p.nome_completo,p.data_nascimento,p.contato,p.email,"+REUTILIZAVEL+" AS reutilizavel FROM Pessoa p WHERE cpf=?")) {
            s.setLong(1,cpf); try(ResultSet rs=s.executeQuery()) {
                if(!rs.next()) return null;
                if (!rs.getBoolean("reutilizavel")) throw new IllegalArgumentException("CPF já utilizado em outro tipo de cadastro.");
                Responsavel r=new Responsavel(); r.setCpf(cpf); r.setNomeCompleto(rs.getString("nome_completo")); r.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());
                r.setContato(rs.getString("contato")); r.setEmail(rs.getString("email")); return r;
            }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível consultar o CPF.",e); }
    }
    public boolean desvincular(long cpfAdolescente,long cpfResponsavel) {
        exigir(FAMILIA_REMOVER);

        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("DELETE FROM Responsabiliza WHERE cpf_adolescente=? AND cpf_responsavel=?")) {
            s.setLong(1,cpfAdolescente); s.setLong(2,cpfResponsavel); return s.executeUpdate()==1;
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível remover o vínculo.",e); }
    }

    public List<Responsavel> listarResponsaveis(long cpfAdolescente) {
        exigir(FAMILIA_CONSULTAR);

        List<Responsavel> lista = new ArrayList<>();

        String sql = "SELECT p.cpf, p.nome_completo, p.data_nascimento, p.contato, p.email, " +
                "COALESCE(v.parentesco_vinculo,r.parentesco) AS parentesco, COALESCE(v.principal,r.contato_principal,false) AS contato_principal " +
                "FROM Pessoa p " +
                "INNER JOIN Responsavel r ON p.cpf = r.cpf_responsavel " +
                "INNER JOIN Responsabiliza v ON r.cpf_responsavel = v.cpf_responsavel " +
                "WHERE v.cpf_adolescente = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfAdolescente);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Responsavel resp = new Responsavel();

                    resp.setCpf(rs.getLong("cpf"));
                    resp.setNomeCompleto(rs.getString("nome_completo"));
                    resp.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());
                    resp.setContato(rs.getString("contato"));
                    resp.setEmail(rs.getString("email"));

                    resp.setParentesco(rs.getString("parentesco"));
                    resp.setContatoPrincipal(rs.getBoolean("contato_principal"));

                    lista.add(resp);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível consultar os responsáveis.", e);
        }

        return lista;
    }
}
