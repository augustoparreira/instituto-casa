package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import br.edu.unespar.trabalho.util.IdUtil;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MedidaSocioeducativaDAO {
    public boolean inserir(MedidaSocioeducativa m) { return salvar(m,false); }
    public boolean atualizar(MedidaSocioeducativa m) { return salvar(m,true); }

    private boolean salvar(MedidaSocioeducativa m,boolean edicao) {
        m.validar();
        if(m.getDataFim()!=null && m.getDataFim().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("O encerramento deve ser uma data já ocorrida.");
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Serializa alterações de medidas e frequências do mesmo adolescente.
                try(PreparedStatement lock=c.prepareStatement("SELECT cpf_adolescente FROM Adolescente WHERE cpf_adolescente=? FOR UPDATE")) {
                    lock.setLong(1,m.getCpfAdolescente());
                    try(ResultSet r=lock.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("Adolescente não encontrado."); }
                }
                try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM MedidaSocioeducativa WHERE cpf_adolescente=? AND upper(tipo_medida)=? AND id_medida<>? "
                        + "AND data_inicio<=COALESCE(?::date,'infinity'::date) AND COALESCE(data_fim,'infinity'::date)>=?")) {
                    s.setLong(1,m.getCpfAdolescente()); s.setString(2,m.getTipoMedida().getCodigo()); s.setInt(3,edicao?m.getIdMedida():-1);
                    s.setObject(4,m.getDataFim()); s.setObject(5,m.getDataInicio());
                    try(ResultSet r=s.executeQuery()) { if(r.next()) throw new IllegalArgumentException("Já há uma medida desse tipo neste período. Edite ou encerre a anterior antes de cadastrar outra."); }
                }
                if(edicao) {
                    try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM Frequencia WHERE id_medida=? AND (data_presenca<? OR data_presenca>COALESCE(?::date,'infinity'::date))")) {
                        s.setInt(1,m.getIdMedida()); s.setObject(2,m.getDataInicio()); s.setObject(3,m.getDataFim());
                        try(ResultSet r=s.executeQuery()) { if(r.next()) throw new IllegalArgumentException("Há frequências fora do período informado. Corrija os lançamentos antes de mudar as datas."); }
                    }
                    try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM Frequencia WHERE id_medida=? AND upper(?)<>'PSC'")) {
                        s.setInt(1,m.getIdMedida()); s.setString(2,m.getTipoMedida().getCodigo());
                        try(ResultSet r=s.executeQuery()) { if(r.next()) throw new IllegalArgumentException("Uma PSC com frequências vinculadas não pode ser convertida em LA."); }
                    }
                } else m.setIdMedida(IdUtil.proximoId(c,"MedidaSocioeducativa"));
                String sql=edicao ? "UPDATE MedidaSocioeducativa SET reincidencia=?,tipo_medida=?,data_inicio=?,historico_infracional=?,duracao_meses=?,duracao_horas=?,data_fim=? WHERE id_medida=? AND cpf_adolescente=?"
                        : "INSERT INTO MedidaSocioeducativa(reincidencia,tipo_medida,data_inicio,historico_infracional,duracao_meses,duracao_horas,data_fim,id_medida,cpf_adolescente) VALUES(?,?,?,?,?,?,?,?,?)";
                try(PreparedStatement s=c.prepareStatement(sql)) {
                    s.setBoolean(1,m.isReincidencia()); s.setString(2,m.getTipoMedida().getCodigo()); s.setObject(3,m.getDataInicio());
                    s.setString(4,m.getHistoricoInfracional()); s.setObject(5,m.getDuracaoMeses()); s.setObject(6,m.getDuracaoHoras());
                    s.setObject(7,m.getDataFim()); s.setInt(8,m.getIdMedida()); s.setLong(9,m.getCpfAdolescente());
                    if(s.executeUpdate()!=1) throw new SQLException("Medida não encontrada.");
                }
                c.commit(); return true;
            } catch(SQLException|RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível salvar a medida.",e); }
    }

    public List<MedidaSocioeducativa> listarPorAdolescente(long cpf) {
        List<MedidaSocioeducativa> lista=new ArrayList<>();
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("SELECT * FROM MedidaSocioeducativa WHERE (?=0 OR cpf_adolescente=?) ORDER BY data_inicio DESC,id_medida DESC")) {
            s.setLong(1,cpf); s.setLong(2,cpf);
            try(ResultSet r=s.executeQuery()) { while(r.next()) {
                MedidaSocioeducativa m=new MedidaSocioeducativa();
                m.setIdMedida(r.getInt("id_medida")); m.setCpfAdolescente(r.getLong("cpf_adolescente")); m.setTipoMedida(TipoMedida.fromCodigo(r.getString("tipo_medida")));
                m.setDataInicio(r.getDate("data_inicio").toLocalDate()); Date fim=r.getDate("data_fim"); m.setDataFim(fim==null?null:fim.toLocalDate());
                m.setReincidencia(r.getBoolean("reincidencia")); m.setHistoricoInfracional(r.getString("historico_infracional"));
                m.setDuracaoMeses((Integer)r.getObject("duracao_meses")); m.setDuracaoHoras((Integer)r.getObject("duracao_horas")); lista.add(m);
            } }
            return lista;
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível consultar as medidas.",e); }
    }

    public int consultarHorasDaMedida(int id,LocalDate ate) {
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("SELECT COALESCE(sum(horas_cumpridas),0) FROM Frequencia WHERE id_medida=? AND upper(status_presenca)='PRESENTE' AND data_presenca<=?")) {
            s.setInt(1,id); s.setObject(2,ate);
            try(ResultSet r=s.executeQuery()) { r.next(); return r.getInt(1); }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível calcular as horas.",e); }
    }

    /** Compatibilidade com consultas antigas: soma apenas PSC vigentes, vinculadas por ID. */
    public int consultarHorasCumpridas(long cpf) {
        return listarPorAdolescente(cpf).stream().filter(m->m.isPSC() && m.vigenteEm(LocalDate.now()))
                .mapToInt(m->consultarHorasDaMedida(m.getIdMedida(),LocalDate.now())).sum();
    }
}
