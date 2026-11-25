package br.edu.unespar.trabalho.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

/** Mantém o texto digitado e a data confirmada no formato utilizado pelos formulários. */
public final class DatasFormulario {
    private static final DateTimeFormatter FORMATO=DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    public static final String DATA_INVALIDA = "Informe uma data existente no formato DD/MM/AAAA.";

    private DatasFormulario() {}

    public static void configurarDigitacao(DatePicker campo) {
        configurarConversor(campo);
        var editor = campo.getEditor();
        editor.setTextFormatter(new javafx.scene.control.TextFormatter<>(mudanca -> {
            if (!mudanca.isContentChange()) return mudanca;
            String novo = mudanca.getControlNewText();
            String digitos = Formatadores.soDigitos(novo);
            if (!novo.matches("[0-9/]*") || digitos.length()>8 || novo.length()>10) {
                ValidacaoFormulario.recusarEdicao(editor,DATA_INVALIDA);
                return null;
            }
            StringBuilder formatado = new StringBuilder();
            for (int i=0; i<digitos.length(); i++) {
                if (i==2 || i==4) formatado.append('/');
                formatado.append(digitos.charAt(i));
            }
            if (!novo.contentEquals(formatado)) {
                int cursor = Math.min(mudanca.getCaretPosition(),novo.length());
                int antes = Formatadores.soDigitos(novo.substring(0,cursor)).length();
                int posicao = antes + (antes>2 ? 1 : 0) + (antes>4 ? 1 : 0);
                mudanca.setRange(0,mudanca.getControlText().length());
                mudanca.setText(formatado.toString());
                mudanca.selectRange(posicao,posicao);
            }
            return mudanca;
        }));
    }

    public static void configurar(DatePicker campo) {
        configurarConversor(campo);
        ValidacaoFormulario.filtrar(campo.getEditor(),
                texto -> texto.matches("[0-9]{0,2}(/[0-9]{0,2}(/[0-9]{0,4})?)?"), DATA_INVALIDA);
    }

    private static void configurarConversor(DatePicker campo) {
        campo.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate data) { return data==null ? "" : FORMATO.format(data); }
            @Override public LocalDate fromString(String texto) {
                return texto==null || texto.isBlank() ? null : LocalDate.parse(texto.trim(),FORMATO);
            }
        });
    }
}
