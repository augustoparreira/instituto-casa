package br.edu.unespar.trabalho.model;

public class SituacaoSocial {
    private int idSituacaoSocial;
    private double rendaFamiliar;      // coluna: renda
    private String beneficioSocial;    // coluna: beneficios_sociais (opcional)
    private String endereco;           // NOT NULL
    private String bairro;             // NOT NULL
    private String telefone;           // NOT NULL
    private long numeroNis;            // NOT NULL (11 dígitos -> long)
    private Integer crasReferencia;    // INTEGER, opcional
    private String crasNome;
    public String getCrasNome() { return crasNome; }
    public void setCrasNome(String v) { crasNome = v; }
    private long cpfAdolescente;

    public int getIdSituacaoSocial() { return idSituacaoSocial; }
    public void setIdSituacaoSocial(int v) { this.idSituacaoSocial = v; }
    public double getRendaFamiliar() { return rendaFamiliar; }
    public void setRendaFamiliar(double v) { this.rendaFamiliar = v; }
    public String getBeneficioSocial() { return beneficioSocial; }
    public void setBeneficioSocial(String v) { this.beneficioSocial = v; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String v) { this.endereco = v; }
    public String getBairro() { return bairro; }
    public void setBairro(String v) { this.bairro = v; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String v) { this.telefone = v; }
    public long getNumeroNis() { return numeroNis; }
    public void setNumeroNis(long v) { this.numeroNis = v; }
    public Integer getCrasReferencia() { return crasReferencia; }
    public void setCrasReferencia(Integer v) { this.crasReferencia = v; }
    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long v) { this.cpfAdolescente = v; }
}
