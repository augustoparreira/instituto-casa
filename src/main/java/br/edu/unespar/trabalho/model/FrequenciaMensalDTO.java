package br.edu.unespar.trabalho.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/** Uma linha da planilha mensal; dias vazios não são faltas. */
public class FrequenciaMensalDTO {
    /** Datas com falta injustificada no mês a partir das quais a situação é "Irregular" (e o adolescente entra em descumprimento). */
    public static final int LIMITE_FALTAS_INJUSTIFICADAS = 2;
    private final Adolescente adolescente;
    private final List<MedidaSocioeducativa> medidas;
    private final List<Frequencia> frequencias;
    private final YearMonth mes;
    private final boolean piaEnviado;

    public FrequenciaMensalDTO(Adolescente a,List<MedidaSocioeducativa> m,List<Frequencia> f,YearMonth mes,boolean pia) {
        adolescente=a; medidas=List.copyOf(m); frequencias=List.copyOf(f); this.mes=mes; piaEnviado=pia;
    }
    public Adolescente getAdolescente() { return adolescente; }
    public boolean isPiaEnviado() { return piaEnviado; }
    public List<MedidaSocioeducativa> getMedidasDoMes() {
        // No mês atual, uma medida já cadastrada conta como associada mesmo que comece nos próximos dias
        // (ou meses). Nos demais meses vale o histórico: só medidas iniciadas até o fim daquele mês.
        LocalDate limiteInicio = mes.equals(YearMonth.now()) ? LocalDate.MAX : mes.atEndOfMonth();
        return medidas.stream()
                .filter(m->!m.getDataInicio().isAfter(limiteInicio)
                        && (m.getDataFim()==null || !m.getDataFim().isBefore(mes.atDay(1))))
                .toList();
    }
    private LocalDate referencia() { return mes.atEndOfMonth().isAfter(LocalDate.now())?LocalDate.now():mes.atEndOfMonth(); }
    public List<Frequencia> getRegistrosDia(int dia) { return frequencias.stream().filter(f->f.getDataPresenca().equals(mes.atDay(dia))).toList(); }
    public String getDia(int dia) {
        return getRegistrosDia(dia).stream().map(f->switch(f.getStatusPresenca()) {
            case PRESENTE -> "P"; case FALTA_JUSTIFICADA -> "J"; case FALTA_INJUSTIFICADA -> "A";
        }).distinct().sorted().collect(Collectors.joining("/"));
    }
    public long getFaltasInjustificadas() {
        return frequencias.stream().filter(f->YearMonth.from(f.getDataPresenca()).equals(mes) && f.getStatusPresenca()==StatusPresenca.FALTA_INJUSTIFICADA)
                .map(Frequencia::getDataPresenca).distinct().count();
    }
    public String getSituacao() { return getFaltasInjustificadas()>=LIMITE_FALTAS_INJUSTIFICADAS ? "Irregular" : "Regular"; }
    public String getMse() { return getMedidasDoMes().stream().map(m->m.getTipoMedida().getCodigo()).distinct().sorted().collect(Collectors.joining("/")); }
    public String getMeses() {
        return getMedidasDoMes().stream().filter(MedidaSocioeducativa::isLA).map(m->m.getMesesCorridos(m.getDataFim()!=null && m.getDataFim().isBefore(referencia())?m.getDataFim():referencia())+"/"+m.getDuracaoMeses()).collect(Collectors.joining("; "));
    }
    public int getHorasPrevistas() { return getMedidasDoMes().stream().filter(MedidaSocioeducativa::isPSC).mapToInt(MedidaSocioeducativa::getDuracaoHoras).sum(); }
    public int getHorasCumpridas() {
        var ids=getMedidasDoMes().stream().filter(MedidaSocioeducativa::isPSC).map(MedidaSocioeducativa::getIdMedida).toList();
        return frequencias.stream().filter(f->f.getIdMedida()!=null && ids.contains(f.getIdMedida()) && !f.getDataPresenca().isAfter(referencia()))
                .mapToInt(Frequencia::getHorasContabilizadas).sum();
    }
    public int getHorasPendentes() {
        return getMedidasDoMes().stream().filter(MedidaSocioeducativa::isPSC).mapToInt(m -> {
            int cumpridas = frequencias.stream()
                    .filter(f -> f.getIdMedida() != null && f.getIdMedida() == m.getIdMedida()
                            && !f.getDataPresenca().isAfter(referencia()))
                    .mapToInt(Frequencia::getHorasContabilizadas).sum();
            return Math.max(0, m.getDuracaoHoras() - cumpridas);
        }).sum();
    }
    public long getSemVinculo() { return frequencias.stream().filter(f->f.getIdMedida()==null && f.getHorasContabilizadas()>0 && YearMonth.from(f.getDataPresenca()).equals(mes)).count(); }
}
