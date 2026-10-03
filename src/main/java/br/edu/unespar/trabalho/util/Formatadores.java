package br.edu.unespar.trabalho.util;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatações de texto usadas nas telas do perfil. */
public class Formatadores {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_EXTENSO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PT_BR);
    private static final DateTimeFormatter DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formatadores() {
    }

    /** 1800.0 -> "R$ 1.800,00" */
    public static String moeda(double valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor).replace('\u00A0', ' ');
    }

    /** 2024-04-10 -> "10 de abril de 2024" */
    public static String dataExtenso(LocalDate data) {
        return data == null ? "-" : data.format(DATA_EXTENSO);
    }

    /** 2024-04-10 -> "10/04/2024" */
    public static String dataCurta(LocalDate data) {
        return data == null ? "-" : data.format(DATA_CURTA);
    }

    /** Texto vazio ou nulo vira "-". */
    public static String textoOuTraco(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }

    /** Aceita "1800", "1800,50" ou "1.800,50". Lança NumberFormatException se não for número. */
    public static double lerDecimal(String texto) {
        String t = texto.trim().replace("R$", "").replace(" ", "");
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        }
        double v = Double.parseDouble(t);
        if (v < 0) {
            throw new NumberFormatException("valor negativo");
        }
        return v;
    }

    /** Só os dígitos de um texto ("123.456.789-00" -> "12345678900"). */
    public static String soDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    /** "João Carlos Oliveira" -> "JC" */
    public static String iniciais(String nome) {
        if (nome == null || nome.isBlank()) return "?";
        String[] partes = nome.trim().split("\\s+");
        String a = partes[0].substring(0, 1);
        String b = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
        return (a + b).toUpperCase(PT_BR);
    }
}