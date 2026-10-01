package br.edu.unespar.trabalho.model;

import java.time.LocalDate;

public class MedidaSocioeducativa {
    private int idMedida;
    private long cpfAdolescente;
    private boolean reincidencia;
    private String tipoMedida; // "LA" ou "PSC"
    private LocalDate dataInicio;
    private String historicoInfracional;
    private Integer duracaoMeses; // Para LA
    private Integer duracaoHoras; // Para PSC

    // Getters e Setters
    public int getIdMedida() { return idMedida; }
    public void setIdMedida(int idMedida) { this.idMedida = idMedida; }
    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }
    public boolean isReincidencia() { return reincidencia; }
    public void setReincidencia(boolean reincidencia) { this.reincidencia = reincidencia; }
    public String getTipoMedida() { return tipoMedida; }
    public void setTipoMedida(String tipoMedida) { this.tipoMedida = tipoMedida; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public String getHistoricoInfracional() { return historicoInfracional; }
    public void setHistoricoInfracional(String historicoInfracional) { this.historicoInfracional = historicoInfracional; }
    public Integer getDuracaoMeses() { return duracaoMeses; }
    public void setDuracaoMeses(Integer duracaoMeses) { this.duracaoMeses = duracaoMeses; }
    public Integer getDuracaoHoras() { return duracaoHoras; }
    public void setDuracaoHoras(Integer duracaoHoras) { this.duracaoHoras = duracaoHoras; }
}