package br.edu.unespar.trabalho.model;

import java.time.LocalDate;

public class PIA {
    private int idPia;
    private LocalDate dataElaboracao;
    private String diagnostico;
    private String vulnerabilidades;
    private String potencialidades;
    private String estrategias;
    private boolean documentoEnviado;
    private long cpfAdolescente;

    public int getIdPia() { return idPia; }
    public void setIdPia(int idPia) { this.idPia = idPia; }
    public LocalDate getDataElaboracao() { return dataElaboracao; }
    public void setDataElaboracao(LocalDate dataElaboracao) { this.dataElaboracao = dataElaboracao; }
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
    public String getVulnerabilidades() { return vulnerabilidades; }
    public void setVulnerabilidades(String vulnerabilidades) { this.vulnerabilidades = vulnerabilidades; }
    public String getPotencialidades() { return potencialidades; }
    public void setPotencialidades(String potencialidades) { this.potencialidades = potencialidades; }
    public String getEstrategias() { return estrategias; }
    public void setEstrategias(String estrategias) { this.estrategias = estrategias; }
    public boolean isDocumentoEnviado() { return documentoEnviado; }
    public void setDocumentoEnviado(boolean documentoEnviado) { this.documentoEnviado = documentoEnviado; }
    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }
}