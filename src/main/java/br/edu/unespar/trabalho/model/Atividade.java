package br.edu.unespar.trabalho.model;

public class Atividade {
    private int idAtividade;
    private String nomeAtividade;
    private String tipo;
    private int cargaHoraria;

    public int getIdAtividade() { return idAtividade; }
    public void setIdAtividade(int v) { this.idAtividade = v; }
    public String getNomeAtividade() { return nomeAtividade; }
    public void setNomeAtividade(String v) { this.nomeAtividade = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { this.tipo = v; }
    public int getCargaHoraria() { return cargaHoraria; }
    public void setCargaHoraria(int v) { this.cargaHoraria = v; }
}