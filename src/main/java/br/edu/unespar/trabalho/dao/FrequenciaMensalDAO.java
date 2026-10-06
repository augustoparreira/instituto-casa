package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class FrequenciaMensalDAO {
    public List<FrequenciaMensalDTO> listar(YearMonth mes) {
        var adolescentes=new AdolescenteDAO().listarCadastros();
        var medidas=new MedidaSocioeducativaDAO().listarPorAdolescente(0).stream().collect(Collectors.groupingBy(MedidaSocioeducativa::getCpfAdolescente));
        var frequencias=new FrequenciaDAO().listar(0,LocalDate.of(1900,1,1),mes.atEndOfMonth()).stream().collect(Collectors.groupingBy(Frequencia::getCpfAdolescente));
        List<FrequenciaMensalDTO> lista=new ArrayList<>();
        for(Adolescente a:adolescentes) lista.add(new FrequenciaMensalDTO(a,medidas.getOrDefault(a.getCpf(),List.of()),frequencias.getOrDefault(a.getCpf(),List.of()),mes,a.isPiaEnviado()));
        return lista;
    }
}
