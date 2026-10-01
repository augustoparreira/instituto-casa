package br.edu.unespar.trabalho.model;

public class Saude {
    private int idFichaSaude;
    private String ubsReferencia;
    private boolean usoSpa;
    private String observacoes;
    private String substanciasUtilizadas;
    private long cpfAdolescente;

    public int getIdFichaSaude() { return idFichaSaude; }
    public void setIdFichaSaude(int idFichaSaude) { this.idFichaSaude = idFichaSaude; }
    public String getUbsReferencia() { return ubsReferencia; }
    public void setUbsReferencia(String ubsReferencia) { this.ubsReferencia = ubsReferencia; }
    public boolean isUsoSpa() { return usoSpa; }
    public void setUsoSpa(boolean usoSpa) { this.usoSpa = usoSpa; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getSubstanciasUtilizadas() { return substanciasUtilizadas; }
    public void setSubstanciasUtilizadas(String substanciasUtilizadas) { this.substanciasUtilizadas = substanciasUtilizadas; }
    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }
}