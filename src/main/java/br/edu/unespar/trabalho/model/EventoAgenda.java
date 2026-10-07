package br.edu.unespar.trabalho.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class EventoAgenda {
    public enum Tipo {
        ATENDIMENTO("Atendimento"), VISITA("Visita domiciliar"), REUNIAO("Reunião"),
        PIA("PIA"), AUDIENCIA("Audiência judicial"), RELATORIO("Relatório"), OUTRO("Outro");
        private final String descricao;
        Tipo(String descricao) { this.descricao=descricao; }
        @Override public String toString() { return descricao; }
    }
    public enum Status {
        AGENDADO("Agendado"), CONCLUIDO("Concluído"), CANCELADO("Cancelado");
        private final String descricao;
        Status(String descricao) { this.descricao=descricao; }
        @Override public String toString() { return descricao; }
    }
    private int idEvento;
    private String titulo, local="", observacoes="", nomeAdolescente, nomeTecnico;
    private Tipo tipo;
    private Status status=Status.AGENDADO;
    private LocalDate data;
    private LocalTime horaInicio, horaFim;
    private Long cpfAdolescente, cpfEquipe;

    public void validar() {
        if(titulo==null || titulo.isBlank() || titulo.trim().length()>120)
            throw new IllegalArgumentException("Informe o título (até 120 caracteres).");
        if(tipo==null || data==null || horaInicio==null || horaFim==null || status==null)
            throw new IllegalArgumentException("Informe tipo, data, horários e situação.");
        if(!horaFim.isAfter(horaInicio)) throw new IllegalArgumentException("O horário final deve ser posterior ao inicial, no mesmo dia.");
        if(horaInicio.getSecond()!=0 || horaFim.getSecond()!=0 || horaInicio.getNano()!=0 || horaFim.getNano()!=0)
            throw new IllegalArgumentException("Informe os horários no formato HH:mm.");
        if(local!=null && local.length()>160) throw new IllegalArgumentException("Local: máximo de 160 caracteres.");
        if(status==Status.CONCLUIDO && data.atTime(horaFim).isAfter(java.time.LocalDateTime.now()))
            throw new IllegalArgumentException("Um compromisso futuro não pode ser marcado como concluído.");
    }
    public int getIdEvento() { return idEvento; }
    public void setIdEvento(int v) { idEvento=v; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String v) { titulo=v; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo v) { tipo=v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { status=v; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate v) { data=v; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime v) { horaInicio=v; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime v) { horaFim=v; }
    public String getLocal() { return local; }
    public void setLocal(String v) { local=v; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String v) { observacoes=v; }
    public Long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(Long v) { cpfAdolescente=v; }
    public Long getCpfEquipe() { return cpfEquipe; }
    public void setCpfEquipe(Long v) { cpfEquipe=v; }
    public String getNomeAdolescente() { return nomeAdolescente; }
    public void setNomeAdolescente(String v) { nomeAdolescente=v; }
    public String getNomeTecnico() { return nomeTecnico; }
    public void setNomeTecnico(String v) { nomeTecnico=v; }
}
