package br.edu.unespar.trabalho.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/** Formatação de documentos e telefones nos formulários de cadastro. */
public final class MascarasFormulario {
    private MascarasFormulario() { }

    public static void cpf(TextField campo) {
        aplicar(campo, 11, true);
    }

    public static void telefone(TextField campo) {
        aplicar(campo, 11, false);
    }

    public static boolean telefoneValido(String valor) {
        int tamanho = Formatadores.soDigitos(valor).length();
        return tamanho == 10 || tamanho == 11;
    }

    public static boolean telefoneValidoOuExistente(String valor, String original) {
        return telefoneValido(valor) || (valor!=null && !valor.isBlank()
                && original!=null && valor.strip().equals(original.strip()));
    }

    /** Exibe um contato legado sem modificar seu valor durante a abertura da edição. */
    public static void carregarTelefoneExistente(TextField campo, String valor) {
        TextFormatter<?> formatador = campo.getTextFormatter();
        campo.setTextFormatter(null);
        campo.setText(valor == null ? "" : valor);
        campo.setTextFormatter(formatador);
    }

    private static void aplicar(TextField campo, int limite, boolean cpf) {
        campo.setTextFormatter(new TextFormatter<>(alteracao -> {
            String proposto = alteracao.getControlNewText();
            String digitos = Formatadores.soDigitos(proposto);
            if (digitos.length() > limite) digitos = digitos.substring(0, limite);
            String formatado = cpf ? formatarCpf(digitos) : formatarTelefone(digitos);
            if (formatado.equals(proposto)) return alteracao;

            int cursor = Math.min(alteracao.getCaretPosition(), proposto.length());
            int digitosAntesDoCursor = Formatadores.soDigitos(proposto.substring(0, cursor)).length();
            alteracao.setRange(0, alteracao.getControlText().length());
            alteracao.setText(formatado);
            int novaPosicao = 0;
            while (novaPosicao < formatado.length() && digitosAntesDoCursor > 0) {
                if (Character.isDigit(formatado.charAt(novaPosicao))) digitosAntesDoCursor--;
                novaPosicao++;
            }
            alteracao.setCaretPosition(novaPosicao);
            alteracao.setAnchor(novaPosicao);
            return alteracao;
        }));
    }

    private static String formatarCpf(String digitos) {
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < digitos.length(); i++) {
            if (i == 3 || i == 6) resultado.append('.');
            if (i == 9) resultado.append('-');
            resultado.append(digitos.charAt(i));
        }
        return resultado.toString();
    }

    private static String formatarTelefone(String digitos) {
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < digitos.length(); i++) {
            if (i == 0) resultado.append('(');
            if (i == 2) resultado.append(") ");
            if (i == (digitos.length() == 11 ? 7 : 6)) resultado.append('-');
            resultado.append(digitos.charAt(i));
        }
        return resultado.toString();
    }
}
