package br.edu.unespar.trabalho.model;

import java.time.LocalDate;

public class Frequencia {
    private long cpfAdolescente;
    private int idAtividade;
    private LocalDate dataPresenca;
    private StatusPresenca statusPresenca;
    private Integer horasCumpridas;

    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }
    public int getIdAtividade() { return idAtividade; }
    public void setIdAtividade(int idAtividade) { this.idAtividade = idAtividade; }
    public LocalDate getDataPresenca() { return dataPresenca; }
    public void setDataPresenca(LocalDate dataPresenca) { this.dataPresenca = dataPresenca; }
    public StatusPresenca getStatusPresenca() { return statusPresenca; }
    public void setStatusPresenca(StatusPresenca statusPresenca) { this.statusPresenca = statusPresenca; }
    public Integer getHorasCumpridas() { return horasCumpridas; }
    public void setHorasCumpridas(Integer horasCumpridas) { this.horasCumpridas = horasCumpridas; }

    public boolean isPresente() { return statusPresenca == StatusPresenca.PRESENTE; }

    /** Horas que contam para o saldo da medida: só presença conta. */
    public int getHorasContabilizadas() {
        return (isPresente() && horasCumpridas != null) ? horasCumpridas : 0;
    }
}