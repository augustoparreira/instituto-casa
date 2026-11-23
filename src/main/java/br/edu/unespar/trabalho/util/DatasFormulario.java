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
    private DatasFormulario() {}

    public static void configurar(DatePicker campo) {
        campo.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate data) { return data==null ? "" : FORMATO.format(data); }
            @Override public LocalDate fromString(String texto) {
                return texto==null || texto.isBlank() ? null : LocalDate.parse(texto.trim(),FORMATO);
            }
        });
    }
}
