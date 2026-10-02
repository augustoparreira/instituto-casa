package br.edu.unespar.trabalho.model;

// Associativa pura: a data de elaboração pertence ao PIA.
public class ElaborarPIA {
    private long cpfEquipe;
    private int idPia;

    public long getCpfEquipe() { return cpfEquipe; }
    public void setCpfEquipe(long v) { this.cpfEquipe = v; }
    public int getIdPia() { return idPia; }
    public void setIdPia(int v) { this.idPia = v; }
}