package br.edu.unespar.trabalho.model;

public class Adolescente extends Pessoa {
    private String naturalidade;
    private String genero;
    private String corRaca;
    private String bairro;
    private String observacoes;
    private boolean imm;
    private boolean valeTransporte;
    private boolean piaEnviado;
    private boolean medidaProtetiva;

    public boolean isMedidaProtetiva() { return medidaProtetiva; }
    public void setMedidaProtetiva(boolean medidaProtetiva) { this.medidaProtetiva = medidaProtetiva; }

    public boolean isPiaEnviado() { return piaEnviado; }
    public void setPiaEnviado(boolean piaEnviado) { this.piaEnviado = piaEnviado; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String v) { observacoes = v; }
    public boolean isImm() { return imm; }
    public void setImm(boolean v) { imm = v; }
    public boolean isValeTransporte() { return valeTransporte; }
    public void setValeTransporte(boolean v) { valeTransporte = v; }
    private StatusAdolescente status = StatusAdolescente.ATIVO;

    public String getNaturalidade() { return naturalidade; }
    public void setNaturalidade(String naturalidade) { this.naturalidade = naturalidade; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    public String getCorRaca() { return corRaca; }
    public void setCorRaca(String corRaca) { this.corRaca = corRaca; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public StatusAdolescente getStatus() { return status; }
    public void setStatus(StatusAdolescente status) { this.status = status; }
}
