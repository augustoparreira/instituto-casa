package br.edu.unespar.trabalho.model;

import java.time.LocalDate;

public class Frequencia {
    private long cpfAdolescente;
    private int idAtividade;
    private LocalDate dataPresenca;
    private StatusPresenca statusPresenca;
    private Integer horasCumpridas;
    private Integer idMedida;
    private String observacoes = "";
    private String nomeAtividade;
    public Integer getIdMedida() { return idMedida; }
    public void setIdMedida(Integer v) { idMedida = v; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String v) { observacoes = v; }
    public String getNomeAtividade() { return nomeAtividade; }
    public void setNomeAtividade(String v) { nomeAtividade = v; }
    @Override public String toString() { return nomeAtividade + " · " + statusPresenca + " · " + getHorasContabilizadas() + "h"; }

    public void validar() {
        if (cpfAdolescente <= 0 || idAtividade <= 0 || dataPresenca == null || statusPresenca == null)
            throw new IllegalArgumentException("Informe adolescente, atividade, data e situação.");
        if (dataPresenca.isAfter(LocalDate.now())) throw new IllegalArgumentException("Não registre frequência futura.");
        if (isPresente() && (horasCumpridas == null || horasCumpridas < 0 || horasCumpridas > 24))
            throw new IllegalArgumentException("Informe horas inteiras entre 0 e 24.");
        if (!isPresente() && horasCumpridas != null && horasCumpridas != 0)
            throw new IllegalArgumentException("Faltas não contabilizam horas.");
        if (statusPresenca == StatusPresenca.FALTA_JUSTIFICADA && (observacoes == null || observacoes.isBlank()))
            throw new IllegalArgumentException("Descreva o motivo da falta justificada.");
    }

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
