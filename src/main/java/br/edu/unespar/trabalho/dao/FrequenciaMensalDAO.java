package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.service.AutorizacaoService;
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
        AutorizacaoService.exigir(FREQUENCIA_CONSULTAR);
        if (!AutorizacaoService.pode(MEDIDA_CONSULTAR)) {
            var registros = new FrequenciaDAO().listar(0, mes.atDay(1), mes.atEndOfMonth()).stream()
                    .collect(Collectors.groupingBy(Frequencia::getCpfAdolescente));
            return listarParticipantes().stream()
                    .map(a -> new FrequenciaMensalDTO(a, List.of(), registros.getOrDefault(a.getCpf(), List.of()), mes, false))
                    .toList();
        }
        var adolescentes=new AdolescenteDAO().listarCadastros();
        var medidas=new MedidaSocioeducativaDAO().listarPorAdolescente(0).stream().collect(Collectors.groupingBy(MedidaSocioeducativa::getCpfAdolescente));
        var frequencias=new FrequenciaDAO().listar(0,LocalDate.of(1900,1,1),mes.atEndOfMonth()).stream().collect(Collectors.groupingBy(Frequencia::getCpfAdolescente));
        List<FrequenciaMensalDTO> lista=new ArrayList<>();
        for(Adolescente a:adolescentes) lista.add(new FrequenciaMensalDTO(a,medidas.getOrDefault(a.getCpf(),List.of()),frequencias.getOrDefault(a.getCpf(),List.of()),mes,a.isPiaEnviado()));
        return lista;
    }
    public List<Adolescente> listarParticipantes() {
        AutorizacaoService.exigir(FREQUENCIA_CONSULTAR);
        String sql = "SELECT a.cpf_adolescente, p.nome_completo, a.status FROM Adolescente a "
                + "JOIN Pessoa p ON a.cpf_adolescente = p.cpf ORDER BY p.nome_completo";
        try (var conexao = br.edu.unespar.trabalho.util.ConnectionFactory.getConnection();
             var consulta = conexao.prepareStatement(sql); var resultado = consulta.executeQuery()) {
            List<Adolescente> participantes = new ArrayList<>();
            while (resultado.next()) {
                Adolescente a = new Adolescente();
                a.setCpf(resultado.getLong(1));
                a.setNomeCompleto(resultado.getString(2));
                a.setStatus(StatusAdolescente.fromCodigo(resultado.getString(3)));
                participantes.add(a);
            }
            return participantes;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Não foi possível consultar participantes.", e);
        }
    }
}
