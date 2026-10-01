package br.edu.unespar.trabalho.model;

import java.time.LocalDate;

public class ElaborarPIA {
    private long cpfEquipe;
    private int idPia;
    private LocalDate dataElaboracao;

    public long getCpfEquipe() { return cpfEquipe; }
    public void setCpfEquipe(long cpfEquipe) { this.cpfEquipe = cpfEquipe; }
    public int getIdPia() { return idPia; }
    public void setIdPia(int idPia) { this.idPia = idPia; }
    public LocalDate getDataElaboracao() { return dataElaboracao; }
    public void setDataElaboracao(LocalDate dataElaboracao) { this.dataElaboracao = dataElaboracao; }
}