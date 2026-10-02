package br.edu.unespar.trabalho.model;

public class Acompanhamento {
    private long cpfAdolescente;
    private long cpfEquipe;
    private boolean tecnicoReferencia;

    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }

    public long getCpfEquipe() { return cpfEquipe; }
    public void setCpfEquipe(long cpfEquipe) { this.cpfEquipe = cpfEquipe; }

    public boolean isTecnicoReferencia() { return tecnicoReferencia; }
    public void setTecnicoReferencia(boolean tecnicoReferencia) { this.tecnicoReferencia = tecnicoReferencia; }
}