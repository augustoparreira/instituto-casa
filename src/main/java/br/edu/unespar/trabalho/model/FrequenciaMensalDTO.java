package br.edu.unespar.trabalho.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Uma linha da planilha mensal; dias vazios não são faltas. */
public class FrequenciaMensalDTO {
    /** Datas com falta injustificada no mês a partir das quais a situação é "Irregular" (e o adolescente entra em descumprimento). */
    public static final int LIMITE_FALTAS_INJUSTIFICADAS = 2;
    private final Adolescente adolescente;
    private final List<MedidaSocioeducativa> medidasDoMes;
    private final Map<LocalDate,List<Frequencia>> frequenciasPorDia;
    private final Map<Integer,Integer> horasPorMedida;
    private final YearMonth mes;
    private final LocalDate referencia;
    private final long faltasInjustificadas;
    private final long semVinculo;
    private final boolean piaEnviado;

    public FrequenciaMensalDTO(Adolescente a,List<MedidaSocioeducativa> m,List<Frequencia> f,YearMonth mes,boolean pia) {
        adolescente=a;
        this.mes=mes;
        piaEnviado=pia;
        LocalDate inicio = mes.atDay(1), fim = mes.atEndOfMonth(), hoje = LocalDate.now();
        referencia = fim.isAfter(hoje) ? hoje : fim;
        medidasDoMes = m.stream().filter(medida->!medida.getDataInicio().isAfter(fim)
                && (medida.getDataFim()==null || !medida.getDataFim().isBefore(inicio))).toList();
        frequenciasPorDia = f.stream().filter(registro -> YearMonth.from(registro.getDataPresenca()).equals(mes))
                .collect(Collectors.groupingBy(Frequencia::getDataPresenca, Collectors.toUnmodifiableList()));
        horasPorMedida = f.stream().filter(registro -> registro.getIdMedida()!=null && !registro.getDataPresenca().isAfter(referencia))
                .collect(Collectors.groupingBy(Frequencia::getIdMedida, Collectors.summingInt(Frequencia::getHorasContabilizadas)));
        faltasInjustificadas = frequenciasPorDia.values().stream().filter(dia -> dia.stream()
                .anyMatch(registro -> registro.getStatusPresenca()==StatusPresenca.FALTA_INJUSTIFICADA)).count();
        semVinculo = frequenciasPorDia.values().stream().flatMap(List::stream)
                .filter(registro -> registro.getIdMedida()==null && registro.getHorasContabilizadas()>0).count();
    }
    public Adolescente getAdolescente() { return adolescente; }
    public boolean isPiaEnviado() { return piaEnviado; }
    public List<MedidaSocioeducativa> getMedidasDoMes() { return medidasDoMes; }
    public List<Frequencia> getRegistrosDia(int dia) { return frequenciasPorDia.getOrDefault(mes.atDay(dia),List.of()); }
    public String getDia(int dia) {
        return getRegistrosDia(dia).stream().map(f->switch(f.getStatusPresenca()) {
            case PRESENTE -> "P"; case FALTA_JUSTIFICADA -> "J"; case FALTA_INJUSTIFICADA -> "A";
        }).distinct().sorted().collect(Collectors.joining("/"));
    }
    public long getFaltasInjustificadas() { return faltasInjustificadas; }
    public String getSituacao() { return getFaltasInjustificadas()>=LIMITE_FALTAS_INJUSTIFICADAS ? "Irregular" : "Regular"; }
    public String getMse() { return getMedidasDoMes().stream().map(m->m.getTipoMedida().getCodigo()).distinct().sorted().collect(Collectors.joining("/")); }
    public String getMeses() {
        return getMedidasDoMes().stream().filter(MedidaSocioeducativa::isLA).map(m->m.getMesesCorridos(m.getDataFim()!=null && m.getDataFim().isBefore(referencia)?m.getDataFim():referencia)+"/"+m.getDuracaoMeses()).collect(Collectors.joining("; "));
    }
    public int getHorasPrevistas() { return getMedidasDoMes().stream().filter(MedidaSocioeducativa::isPSC).mapToInt(MedidaSocioeducativa::getDuracaoHoras).sum(); }
    public int getHorasCumpridas() {
        return medidasDoMes.stream().filter(MedidaSocioeducativa::isPSC).map(MedidaSocioeducativa::getIdMedida)
                .distinct().mapToInt(id -> horasPorMedida.getOrDefault(id,0)).sum();
    }
    public int getHorasPendentes() {
        return medidasDoMes.stream().filter(MedidaSocioeducativa::isPSC)
                .mapToInt(m -> Math.max(0,m.getDuracaoHoras()-horasPorMedida.getOrDefault(m.getIdMedida(),0))).sum();
    }
    public long getSemVinculo() { return semVinculo; }
}
