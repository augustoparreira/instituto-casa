package br.edu.unespar.trabalho.dao;

import static br.edu.unespar.trabalho.service.AutorizacaoService.*;
import static br.edu.unespar.trabalho.model.Operacao.*;
import br.edu.unespar.trabalho.model.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Consulta por JDBC os dados de adolescentes, medidas e frequências para a planilha mensal.
 */
public class FrequenciaMensalDAO {
    public List<FrequenciaMensalDTO> listar(YearMonth mes) {
        exigir(FREQUENCIA_CONSULTAR);
        if (!pode(MEDIDA_CONSULTAR)) {
            var registros = new FrequenciaDAO().listar(0, mes.atDay(1), mes.atEndOfMonth()).stream()
                    .collect(Collectors.groupingBy(Frequencia::getCpfAdolescente));
            return listarParticipantes().stream()
                    .map(a -> new FrequenciaMensalDTO(a, List.of(), registros.getOrDefault(a.getCpf(), List.of()), mes, false))
                    .toList();
        }
        var adolescentes=new AdolescenteDAO().listarCadastros();
        var medidas=new MedidaSocioeducativaDAO().listarParaFrequencia(0).stream().collect(Collectors.groupingBy(MedidaSocioeducativa::getCpfAdolescente));
        var frequencias=new FrequenciaDAO().listar(0,LocalDate.of(1900,1,1),mes.atEndOfMonth()).stream().collect(Collectors.groupingBy(Frequencia::getCpfAdolescente));
        List<FrequenciaMensalDTO> lista=new ArrayList<>();
        for(Adolescente a:adolescentes) lista.add(new FrequenciaMensalDTO(a,medidas.getOrDefault(a.getCpf(),List.of()),frequencias.getOrDefault(a.getCpf(),List.of()),mes,a.isPiaEnviado()));
        return lista;
    }
    public List<Adolescente> listarParticipantes() {
        return new AdolescenteDAO().listarParaFrequencia();
    }
}
