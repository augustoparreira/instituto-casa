package br.edu.unespar.trabalho.model;
import java.time.LocalDate;

public class Frequencia {
    private long cpfAdolescente;
    private int idAtividade;
    private LocalDate dataPresenca;
    private String statusPresenca; // "Presente", "Falta", "Justificada"
    private Integer horasCumpridas;

    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }
    public int getIdAtividade() { return idAtividade; }
    public void setIdAtividade(int idAtividade) { this.idAtividade = idAtividade; }
    public LocalDate getDataPresenca() { return dataPresenca; }
    public void setDataPresenca(LocalDate dataPresenca) { this.dataPresenca = dataPresenca; }
    public String getStatusPresenca() { return statusPresenca; }
    public void setStatusPresenca(String statusPresenca) { this.statusPresenca = statusPresenca; }
    public Integer getHorasCumpridas() { return horasCumpridas; }
    public void setHorasCumpridas(Integer horasCumpridas) { this.horasCumpridas = horasCumpridas; }
}