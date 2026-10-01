package br.edu.unespar.trabalho.model;

public class SituacaoSocial {
    private int idSituacaoSocial;
    private double rendaFamiliar;
    private String beneficioSocial;
    private String crasReferencia;
    private long cpfAdolescente;

    public int getIdSituacaoSocial() { return idSituacaoSocial; }
    public void setIdSituacaoSocial(int idSituacaoSocial) { this.idSituacaoSocial = idSituacaoSocial; }
    public double getRendaFamiliar() { return rendaFamiliar; }
    public void setRendaFamiliar(double rendaFamiliar) { this.rendaFamiliar = rendaFamiliar; }
    public String getBeneficioSocial() { return beneficioSocial; }
    public void setBeneficioSocial(String beneficioSocial) { this.beneficioSocial = beneficioSocial; }
    public String getCrasReferencia() { return crasReferencia; }
    public void setCrasReferencia(String crasReferencia) { this.crasReferencia = crasReferencia; }
    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }
}