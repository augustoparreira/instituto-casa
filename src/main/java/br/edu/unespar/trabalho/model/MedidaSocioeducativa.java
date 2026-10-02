package br.edu.unespar.trabalho.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Reincidência NÃO é campo: é derivada (adolescente com mais de uma medida).
 * Calcule no service/DAO contando as medidas do adolescente.
 */
public class MedidaSocioeducativa {
    private int idMedida;
    private long cpfAdolescente;
    private TipoMedida tipoMedida;
    private LocalDate dataInicio;
    private String historicoInfracional;
    private Integer duracaoMeses; // somente LA
    private Integer duracaoHoras; // somente PSC

    public int getIdMedida() { return idMedida; }
    public void setIdMedida(int idMedida) { this.idMedida = idMedida; }
    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }
    public TipoMedida getTipoMedida() { return tipoMedida; }
    public void setTipoMedida(TipoMedida tipoMedida) { this.tipoMedida = tipoMedida; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public String getHistoricoInfracional() { return historicoInfracional; }
    public void setHistoricoInfracional(String historicoInfracional) { this.historicoInfracional = historicoInfracional; }
    public Integer getDuracaoMeses() { return duracaoMeses; }
    public void setDuracaoMeses(Integer duracaoMeses) { this.duracaoMeses = duracaoMeses; }
    public Integer getDuracaoHoras() { return duracaoHoras; }
    public void setDuracaoHoras(Integer duracaoHoras) { this.duracaoHoras = duracaoHoras; }

    public boolean isLA() { return tipoMedida == TipoMedida.LA; }
    public boolean isPSC() { return tipoMedida == TipoMedida.PSC; }

    /** Meses corridos desde o início da medida (0 se ainda não começou). */
    public int getMesesCorridos() {
        return getMesesCorridos(LocalDate.now());
    }

    public int getMesesCorridos(LocalDate referencia) {
        if (dataInicio == null || referencia == null) return 0;
        return (int) Math.max(0, ChronoUnit.MONTHS.between(dataInicio, referencia));
    }

    /** Lança IllegalArgumentException se os campos condicionais estiverem inconsistentes. */
    public void validar() {
        if (tipoMedida == null) throw new IllegalArgumentException("Informe o tipo da medida (LA ou PSC).");
        if (dataInicio == null) throw new IllegalArgumentException("Informe a data de início da medida.");
        if (isLA()) {
            if (duracaoMeses == null || duracaoMeses <= 0)
                throw new IllegalArgumentException("LA exige duração em meses maior que zero.");
            if (duracaoHoras != null)
                throw new IllegalArgumentException("LA não possui duração em horas.");
        } else {
            if (duracaoHoras == null || duracaoHoras <= 0)
                throw new IllegalArgumentException("PSC exige duração em horas maior que zero.");
            if (duracaoMeses != null)
                throw new IllegalArgumentException("PSC não possui duração em meses.");
        }
    }
}