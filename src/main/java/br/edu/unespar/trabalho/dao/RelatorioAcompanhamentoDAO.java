package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.RelatorioAcompanhamento;
import br.edu.unespar.trabalho.model.RelatorioAcompanhamento.Campo;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class RelatorioAcompanhamentoDAO {
    public boolean salvar(RelatorioAcompanhamento r) {
        r.validar();
        String colunas=Arrays.stream(Campo.values()).map(Campo::getColuna).collect(Collectors.joining(","));
        String atribuicoes=Arrays.stream(Campo.values()).map(c->c.getColuna()+"=?").collect(Collectors.joining(","));
        String parametros=String.join(",",Collections.nCopies(Campo.values().length,"?"));
        String sql=r.getIdRelatorio()==0 ? "INSERT INTO RelatorioAcompanhamento(cpf_adolescente,competencia,data_emissao,"+colunas+",registro_frequencia,descumprimento) VALUES(?,?,?,"+parametros+",?,?) RETURNING id_relatorio"
                : "UPDATE RelatorioAcompanhamento SET cpf_adolescente=?,competencia=?,data_emissao=?,"+atribuicoes+",registro_frequencia=?,descumprimento=? WHERE id_relatorio=? RETURNING id_relatorio";
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(sql)) {
            int i=1; s.setLong(i++,r.getCpfAdolescente()); s.setObject(i++,r.getCompetencia().atDay(1)); s.setObject(i++,r.getDataEmissao());
            for(Campo campo:Campo.values()) s.setString(i++,r.getCampo(campo));
            s.setString(i++,r.getRegistroFrequencia()); s.setString(i++,r.getDescumprimento());
            if(r.getIdRelatorio()!=0) s.setInt(i,r.getIdRelatorio());
            try(ResultSet rs=s.executeQuery()) {
                if(!rs.next()) throw new IllegalArgumentException("O relatório não existe mais. Atualize a lista.");
                r.setIdRelatorio(rs.getInt(1)); return true;
            }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível salvar o relatório. Confira a configuração do banco.",e); }
    }
    public List<RelatorioAcompanhamento> listar() {
        List<RelatorioAcompanhamento> lista=new ArrayList<>();
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("SELECT * FROM RelatorioAcompanhamento ORDER BY competencia DESC,id_relatorio DESC"); ResultSet rs=s.executeQuery()) {
            while(rs.next()) {
                RelatorioAcompanhamento r=new RelatorioAcompanhamento(); r.setIdRelatorio(rs.getInt("id_relatorio"));
                r.setCpfAdolescente(rs.getLong("cpf_adolescente")); r.setCompetencia(YearMonth.from(rs.getObject("competencia",LocalDate.class)));
                r.setDataEmissao(rs.getObject("data_emissao",LocalDate.class));
                for(Campo campo:Campo.values()) r.setCampo(campo,rs.getString(campo.getColuna()));
                r.setRegistroFrequencia(rs.getString("registro_frequencia")); r.setDescumprimento(rs.getString("descumprimento")); lista.add(r);
            }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível consultar os relatórios. Confira a configuração do banco.",e); }
        return lista;
    }
    public boolean excluir(int id) {
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("DELETE FROM RelatorioAcompanhamento WHERE id_relatorio=?")) {
            s.setInt(1,id); return s.executeUpdate()==1;
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível excluir o relatório.",e); }
    }
}
