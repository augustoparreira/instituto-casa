package br.edu.unespar.trabalho.model;

public class Adolescente extends Pessoa {
    private String naturalidade;
    private String genero;
    private String corRaca;
    private StatusAdolescente status = StatusAdolescente.ATIVO;

    public String getNaturalidade() { return naturalidade; }
    public void setNaturalidade(String naturalidade) { this.naturalidade = naturalidade; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    public String getCorRaca() { return corRaca; }
    public void setCorRaca(String corRaca) { this.corRaca = corRaca; }
    public StatusAdolescente getStatus() { return status; }
    public void setStatus(StatusAdolescente status) { this.status = status; }
}