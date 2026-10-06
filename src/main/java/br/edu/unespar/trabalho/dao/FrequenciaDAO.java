package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class FrequenciaDAO {
    public boolean registrar(Frequencia f) { return salvar(f,false); }
    public boolean atualizar(Frequencia f) { return salvar(f,true); }

    private boolean salvar(Frequencia f,boolean edicao) {
        f.validar();
        if(f.getHorasContabilizadas()>0 && f.getIdMedida()==null)
            throw new IllegalArgumentException("Selecione a PSC para contabilizar horas. Para acompanhamento apenas de LA, informe zero horas.");
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement s=c.prepareStatement("SELECT cpf_adolescente FROM Adolescente WHERE cpf_adolescente=? FOR UPDATE")) {
                    s.setLong(1,f.getCpfAdolescente());
                    try(ResultSet r=s.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("Adolescente não encontrado."); }
                }
                if(f.getIdMedida()!=null) {
                    try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM MedidaSocioeducativa WHERE id_medida=? AND cpf_adolescente=? AND upper(tipo_medida)='PSC' AND data_inicio<=? AND (data_fim IS NULL OR data_fim>=?)")) {
                        s.setInt(1,f.getIdMedida()); s.setLong(2,f.getCpfAdolescente()); s.setObject(3,f.getDataPresenca()); s.setObject(4,f.getDataPresenca());
                        try(ResultSet r=s.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("A PSC selecionada não pertence a este adolescente ou não abrange a data."); }
                    }
                }
                try(PreparedStatement s=c.prepareStatement("SELECT COALESCE(sum(horas_cumpridas),0) FROM Frequencia WHERE cpf_adolescente=? AND data_presenca=? AND upper(status_presenca)='PRESENTE' AND id_atividade<>?")) {
                    s.setLong(1,f.getCpfAdolescente()); s.setObject(2,f.getDataPresenca()); s.setInt(3,edicao?f.getIdAtividade():-1);
                    try(ResultSet r=s.executeQuery()) { r.next(); if(r.getInt(1)+f.getHorasContabilizadas()>24) throw new IllegalArgumentException("A soma de horas do dia não pode ultrapassar 24."); }
                }
                String sql=edicao ? "UPDATE Frequencia SET status_presenca=?,horas_cumpridas=?,id_medida=?,observacoes=? WHERE cpf_adolescente=? AND id_atividade=? AND data_presenca=?"
                        : "INSERT INTO Frequencia(status_presenca,horas_cumpridas,id_medida,observacoes,cpf_adolescente,id_atividade,data_presenca) VALUES(?,?,?,?,?,?,?)";
                try(PreparedStatement s=c.prepareStatement(sql)) {
                    s.setString(1,f.getStatusPresenca().getCodigo()); s.setInt(2,f.getHorasContabilizadas()); s.setObject(3,f.getIdMedida());
                    s.setString(4,f.getObservacoes()==null?"":f.getObservacoes().trim()); s.setLong(5,f.getCpfAdolescente());
                    s.setInt(6,f.getIdAtividade()); s.setObject(7,f.getDataPresenca());
                    if(s.executeUpdate()!=1) throw new IllegalArgumentException("O lançamento não existe mais. Atualize a tela.");
                }
                c.commit(); return true;
            } catch(SQLException|RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) {
            if("23505".equals(e.getSQLState())) throw new IllegalArgumentException("Já existe lançamento nesta atividade e data. Selecione-o para editar.",e);
            throw new IllegalStateException("Não foi possível salvar a frequência.",e);
        }
    }

    public boolean excluir(Frequencia f) {
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("DELETE FROM Frequencia WHERE cpf_adolescente=? AND id_atividade=? AND data_presenca=?")) {
            s.setLong(1,f.getCpfAdolescente()); s.setInt(2,f.getIdAtividade()); s.setObject(3,f.getDataPresenca()); return s.executeUpdate()==1;
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível excluir o lançamento.",e); }
    }

    public List<Frequencia> listar(long cpf,LocalDate inicio,LocalDate fim) {
        List<Frequencia> lista=new ArrayList<>();
        String sql="SELECT f.*,a.nome_atividade FROM Frequencia f JOIN Atividade a ON a.id_atividade=f.id_atividade "
                + "WHERE (?=0 OR f.cpf_adolescente=?) AND f.data_presenca BETWEEN ? AND ? ORDER BY f.data_presenca DESC,a.nome_atividade";
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setLong(1,cpf); s.setLong(2,cpf); s.setObject(3,inicio); s.setObject(4,fim);
            try(ResultSet r=s.executeQuery()) { while(r.next()) {
                Frequencia f=new Frequencia(); f.setCpfAdolescente(r.getLong("cpf_adolescente")); f.setIdAtividade(r.getInt("id_atividade"));
                f.setNomeAtividade(r.getString("nome_atividade")); f.setDataPresenca(r.getDate("data_presenca").toLocalDate());
                f.setStatusPresenca(StatusPresenca.fromCodigo(r.getString("status_presenca"))); f.setHorasCumpridas((Integer)r.getObject("horas_cumpridas"));
                f.setIdMedida((Integer)r.getObject("id_medida")); f.setObservacoes(r.getString("observacoes")); lista.add(f);
            } }
            return lista;
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível consultar as frequências.",e); }
    }

    public int consultarFaltas(long cpf) { return consultarFaltas(cpf,YearMonth.now()); }
    public int consultarFaltas(long cpf,YearMonth mes) {
        return (int)listar(cpf,mes.atDay(1),mes.atEndOfMonth()).stream()
                .filter(f->f.getStatusPresenca()==StatusPresenca.FALTA_INJUSTIFICADA)
                .map(Frequencia::getDataPresenca).distinct().count();
    }
}
