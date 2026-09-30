package br.edu.unespar.trabalho.model;

public class EventoAgendaDTO {
    private int dia;
    private String horario;
    private String titulo;
    private String tipo; // "visita", "atendimento", "reuniao", "pia", "audiencia"

    public EventoAgendaDTO(int dia, String horario, String titulo, String tipo) {
        this.dia = dia;
        this.horario = horario;
        this.titulo = titulo;
        this.tipo = tipo;
    }

    public int getDia() { return dia; }
    public String getHorario() { return horario; }
    public String getTitulo() { return titulo; }
    public String getTipo() { return tipo; }
}