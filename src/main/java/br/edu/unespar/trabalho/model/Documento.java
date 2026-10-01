package br.edu.unespar.trabalho.model;

import java.time.LocalDate;

public class Documento {
    private int idDocumento;
    private String tipo;
    private LocalDate dataGeracao;
    private String statusEnvio;
    private long cpfAdolescente;

    public int getIdDocumento() { return idDocumento; }
    public void setIdDocumento(int idDocumento) { this.idDocumento = idDocumento; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public LocalDate getDataGeracao() { return dataGeracao; }
    public void setDataGeracao(LocalDate dataGeracao) { this.dataGeracao = dataGeracao; }

    public String getStatusEnvio() { return statusEnvio; }
    public void setStatusEnvio(String statusEnvio) { this.statusEnvio = statusEnvio; }

    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }
}