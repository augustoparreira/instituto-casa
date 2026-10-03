package br.edu.unespar.trabalho.model;

import java.time.LocalDate;

/** Uma linha da tabela da aba "Frequência e Horas" (registro + nome da atividade). */
public record FrequenciaLinhaDTO(LocalDate data, StatusPresenca status, Integer horas, String atividade) {
}