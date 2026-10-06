package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.ComposicaoFamiliar;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import br.edu.unespar.trabalho.util.IdUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ComposicaoFamiliarDAO {

    public boolean inserir(ComposicaoFamiliar f) { return salvar(f,false); }
    public boolean atualizar(ComposicaoFamiliar f) { return salvar(f,true); }
    private boolean salvar(ComposicaoFamiliar f,boolean edicao) {
        if(f.getNome()==null || f.getNome().isBlank()) throw new IllegalArgumentException("Informe o nome do familiar.");
        if(f.getRenda()!=null && (!Double.isFinite(f.getRenda()) || f.getRenda()<0)) throw new IllegalArgumentException("Renda inválida.");
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                if(!edicao) f.setIdComposicaoFamiliar(IdUtil.proximoId(c,"ComposicaoFamiliar"));
                String sql=edicao ? "UPDATE ComposicaoFamiliar SET nome=?,parentesco=?,idade=?,renda=?,escolaridade=?,profissao=? WHERE id_composicaoFamiliar=? AND cpf_adolescente=?"
                        : "INSERT INTO ComposicaoFamiliar(nome,parentesco,idade,renda,escolaridade,profissao,id_composicaoFamiliar,cpf_adolescente) VALUES(?,?,?,?,?,?,?,?)";
                try(PreparedStatement s=c.prepareStatement(sql)) {
                    s.setString(1,f.getNome()); s.setString(2,f.getParentesco()); s.setObject(3,f.getIdade()); s.setObject(4,f.getRenda());
                    s.setString(5,f.getEscolaridade()); s.setString(6,f.getProfissao()); s.setInt(7,f.getIdComposicaoFamiliar()); s.setLong(8,f.getCpfAdolescente());
                    if(s.executeUpdate()!=1) throw new SQLException("Familiar não encontrado.");
                }
                c.commit(); return true;
            } catch(SQLException|RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível salvar o familiar.",e); }
    }
    public boolean excluir(int id,long cpf) {
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("DELETE FROM ComposicaoFamiliar WHERE id_composicaoFamiliar=? AND cpf_adolescente=?")) {
            s.setInt(1,id); s.setLong(2,cpf); return s.executeUpdate()==1;
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível excluir o familiar.",e); }
    }

    public List<ComposicaoFamiliar> listarPorAdolescente(long cpfAdolescente) {
        List<ComposicaoFamiliar> lista = new ArrayList<>();
        String sql = "SELECT id_composicaoFamiliar, nome, parentesco, idade, renda, escolaridade, profissao, cpf_adolescente " +
                "FROM ComposicaoFamiliar WHERE cpf_adolescente = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfAdolescente);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ComposicaoFamiliar cf = new ComposicaoFamiliar();
                    cf.setIdComposicaoFamiliar(rs.getInt("id_composicaoFamiliar"));
                    cf.setNome(rs.getString("nome"));
                    cf.setParentesco(rs.getString("parentesco"));

                    int idade = rs.getInt("idade");
                    cf.setIdade(rs.wasNull() ? null : idade);

                    double renda = rs.getDouble("renda");
                    cf.setRenda(rs.wasNull() ? null : renda);

                    cf.setEscolaridade(rs.getString("escolaridade"));
                    cf.setProfissao(rs.getString("profissao"));
                    cf.setCpfAdolescente(rs.getLong("cpf_adolescente"));
                    lista.add(cf);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar composição familiar: " + e.getMessage());
        }
        return lista;
    }
}