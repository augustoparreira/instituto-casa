package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Responsavel;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResponsavelDAO {

    public boolean inserir(Responsavel r,long cpf) { return salvar(r,cpf,false); }
    public boolean atualizar(Responsavel r,long cpf) { return salvar(r,cpf,true); }
    private boolean salvar(Responsavel r,long cpf,boolean edicao) {
        if(r.getCpf()==cpf || r.getCpf()<=0 || r.getNomeCompleto()==null || r.getNomeCompleto().isBlank() || r.getDataNascimento()==null)
            throw new IllegalArgumentException("Confira os dados do responsável.");
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement s=c.prepareStatement("SELECT cpf_adolescente FROM Adolescente WHERE cpf_adolescente=? FOR UPDATE")) {
                    s.setLong(1,cpf); try(ResultSet rs=s.executeQuery()) { if(!rs.next()) throw new IllegalArgumentException("Adolescente não encontrado."); }
                }
                String pessoa=edicao ? "UPDATE Pessoa SET nome_completo=?,data_nascimento=?,contato=?,email=? WHERE cpf=?"
                        : "INSERT INTO Pessoa(nome_completo,data_nascimento,contato,email,cpf) VALUES(?,?,?,?,?) ON CONFLICT(cpf) DO NOTHING";
                try(PreparedStatement s=c.prepareStatement(pessoa)) {
                    s.setString(1,r.getNomeCompleto()); s.setObject(2,r.getDataNascimento()); s.setString(3,r.getContato()); s.setString(4,r.getEmail()); s.setLong(5,r.getCpf()); s.executeUpdate();
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
    public Responsavel buscarPessoa(long cpf) {
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("SELECT * FROM Pessoa WHERE cpf=?")) {
            s.setLong(1,cpf); try(ResultSet rs=s.executeQuery()) {
                if(!rs.next()) return null;
                Responsavel r=new Responsavel(); r.setCpf(cpf); r.setNomeCompleto(rs.getString("nome_completo")); r.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());
                r.setContato(rs.getString("contato")); r.setEmail(rs.getString("email")); return r;
            }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível consultar o CPF.",e); }
    }
    public boolean desvincular(long cpfAdolescente,long cpfResponsavel) {
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("DELETE FROM Responsabiliza WHERE cpf_adolescente=? AND cpf_responsavel=?")) {
            s.setLong(1,cpfAdolescente); s.setLong(2,cpfResponsavel); return s.executeUpdate()==1;
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível remover o vínculo.",e); }
    }

    public List<Responsavel> listarResponsaveis(long cpfAdolescente) {
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

                    // Dados da tabela Pessoa
                    resp.setCpf(rs.getLong("cpf"));
                    resp.setNomeCompleto(rs.getString("nome_completo"));
                    resp.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());
                    resp.setContato(rs.getString("contato"));
                    resp.setEmail(rs.getString("email"));

                    // Dados da tabela Responsavel
                    resp.setParentesco(rs.getString("parentesco"));
                    resp.setContatoPrincipal(rs.getBoolean("contato_principal"));

                    lista.add(resp);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar responsáveis: " + e.getMessage());
        }

        return lista;
    }
}