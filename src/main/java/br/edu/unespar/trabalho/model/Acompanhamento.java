package br.edu.unespar.trabalho.model;

public class Acompanhamento {
    private long cpfAdolescente;
    private long cpfEquipe;
    private Boolean tecnicoReferencia;

    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }

    public long getCpfEquipe() { return cpfEquipe; }
    public void setCpfEquipe(long cpfEquipe) { this.cpfEquipe = cpfEquipe; }

    public Boolean getTecnicoReferencia() { return tecnicoReferencia; }
    public void setTecnicoReferencia(Boolean tecnicoReferencia) { this.tecnicoReferencia = tecnicoReferencia; }
}