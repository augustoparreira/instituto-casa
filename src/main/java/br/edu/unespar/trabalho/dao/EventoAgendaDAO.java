package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.EventoAgenda;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EventoAgendaDAO {
    public boolean inserir(EventoAgenda evento) { return salvar(evento,false); }
    public boolean atualizar(EventoAgenda evento) { return salvar(evento,true); }

    private boolean salvar(EventoAgenda e,boolean edicao) {
        e.validar();
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Pessoas compartilhadas serializam a verificação e a gravação de horários.
                try(PreparedStatement s=c.prepareStatement("SELECT cpf FROM Pessoa WHERE cpf IN (?,?) ORDER BY cpf FOR UPDATE")) {
                    s.setObject(1,e.getCpfAdolescente()); s.setObject(2,e.getCpfEquipe()); s.executeQuery().close();
                }
                if(e.getStatus()==EventoAgenda.Status.AGENDADO && (e.getCpfAdolescente()!=null || e.getCpfEquipe()!=null)) {
                    try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM EventoAgenda WHERE status='AGENDADO' AND id_evento<>? "
                            + "AND data_evento=? AND hora_inicio<? AND hora_fim>? AND (cpf_adolescente=? OR cpf_equipe=?)")) {
                        s.setInt(1,edicao?e.getIdEvento():-1); s.setObject(2,e.getData());
                        s.setObject(3,e.getHoraFim()); s.setObject(4,e.getHoraInicio());
                        s.setObject(5,e.getCpfAdolescente()); s.setObject(6,e.getCpfEquipe());
                        try(ResultSet r=s.executeQuery()) { if(r.next()) throw new IllegalArgumentException("O adolescente ou técnico já tem um compromisso agendado nesse horário."); }
                    }
                }
                String sql=edicao ? "UPDATE EventoAgenda SET titulo=?,tipo=?,data_evento=?,hora_inicio=?,hora_fim=?,local_evento=?,observacoes=?,status=?,cpf_adolescente=?,cpf_equipe=? WHERE id_evento=? RETURNING id_evento"
                        : "INSERT INTO EventoAgenda(titulo,tipo,data_evento,hora_inicio,hora_fim,local_evento,observacoes,status,cpf_adolescente,cpf_equipe) VALUES(?,?,?,?,?,?,?,?,?,?) RETURNING id_evento";
                try(PreparedStatement s=c.prepareStatement(sql)) {
                    s.setString(1,e.getTitulo().trim()); s.setString(2,e.getTipo().name()); s.setObject(3,e.getData());
                    s.setObject(4,e.getHoraInicio()); s.setObject(5,e.getHoraFim()); s.setString(6,e.getLocal()==null?"":e.getLocal().trim());
                    s.setString(7,e.getObservacoes()==null?"":e.getObservacoes().trim()); s.setString(8,e.getStatus().name());
                    s.setObject(9,e.getCpfAdolescente()); s.setObject(10,e.getCpfEquipe());
                    if(edicao) s.setInt(11,e.getIdEvento());
                    try(ResultSet r=s.executeQuery()) {
                        if(!r.next()) throw new IllegalArgumentException("O compromisso não existe mais. Atualize a agenda.");
                        e.setIdEvento(r.getInt(1));
                    }
                }
                c.commit(); return true;
            } catch(SQLException|RuntimeException ex) { c.rollback(); throw ex; }
        } catch(SQLException ex) { throw new IllegalStateException("Não foi possível salvar o compromisso. Confira a conexão e a configuração da agenda no banco.",ex); }
    }

    public List<EventoAgenda> listar(LocalDate inicio,LocalDate fim) {
        List<EventoAgenda> eventos=new ArrayList<>();
        String sql="SELECT e.*,a.nome_completo AS nome_adolescente,t.nome_completo AS nome_tecnico FROM EventoAgenda e "
                + "LEFT JOIN Pessoa a ON a.cpf=e.cpf_adolescente LEFT JOIN Pessoa t ON t.cpf=e.cpf_equipe "
                + "WHERE data_evento BETWEEN ? AND ? ORDER BY data_evento,hora_inicio,id_evento";
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setObject(1,inicio); s.setObject(2,fim);
            try(ResultSet r=s.executeQuery()) { while(r.next()) {
                EventoAgenda e=new EventoAgenda(); e.setIdEvento(r.getInt("id_evento")); e.setTitulo(r.getString("titulo"));
                e.setTipo(EventoAgenda.Tipo.valueOf(r.getString("tipo"))); e.setStatus(EventoAgenda.Status.valueOf(r.getString("status")));
                e.setData(r.getObject("data_evento",LocalDate.class)); e.setHoraInicio(r.getObject("hora_inicio",java.time.LocalTime.class));
                e.setHoraFim(r.getObject("hora_fim",java.time.LocalTime.class)); e.setLocal(r.getString("local_evento")); e.setObservacoes(r.getString("observacoes"));
                e.setCpfAdolescente((Long)r.getObject("cpf_adolescente")); e.setCpfEquipe((Long)r.getObject("cpf_equipe"));
                e.setNomeAdolescente(r.getString("nome_adolescente")); e.setNomeTecnico(r.getString("nome_tecnico")); eventos.add(e);
            } }
        } catch(SQLException ex) { throw new IllegalStateException("Não foi possível carregar a agenda. Confira a conexão e a configuração da agenda no banco.",ex); }
        return eventos;
    }

    public boolean excluir(int id) {
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement("DELETE FROM EventoAgenda WHERE id_evento=?")) {
            s.setInt(1,id); return s.executeUpdate()==1;
        } catch(SQLException ex) { throw new IllegalStateException("Não foi possível excluir o compromisso.",ex); }
    }
}
