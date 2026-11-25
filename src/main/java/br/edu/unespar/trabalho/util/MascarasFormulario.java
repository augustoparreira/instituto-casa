package br.edu.unespar.trabalho.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/** Formatação de documentos e telefones nos formulários de cadastro. */
public final class MascarasFormulario {
    public static final String CPF_INVALIDO = "Informe um CPF com 11 dígitos e dígitos verificadores válidos.";
    public static final String TELEFONE_INVALIDO = "Informe um telefone com 10 ou 11 dígitos, incluindo o DDD. O prefixo +55 é opcional.";

    private MascarasFormulario() { }

    public static void cpf(TextField campo) {
        aplicar(campo, true);
    }

    public static void telefone(TextField campo) {
        aplicar(campo, false);
    }

    public static boolean cpfValido(String valor) {
        if (valor==null || !valor.matches("[0-9.\\- ]+")) return false;
        String digitos = Formatadores.soDigitos(valor);
        if (digitos.length()!=11 || digitos.chars().allMatch(d -> d==digitos.charAt(0))) return false;
        return digitos.charAt(9)-'0'==digitoVerificador(digitos,9)
                && digitos.charAt(10)-'0'==digitoVerificador(digitos,10);
    }

    private static int digitoVerificador(String digitos, int posicao) {
        int soma = 0;
        for (int i=0; i<posicao; i++) soma += (digitos.charAt(i)-'0')*(posicao+1-i);
        int resto = soma%11;
        return resto<2 ? 0 : 11-resto;
    }

    public static boolean telefoneValido(String valor) {
        String nacional = telefoneNacional(valor);
        return nacional!=null && (nacional.length()==10 || nacional.length()==11);
    }

    private static String telefoneNacional(String valor) {
        if (valor==null) return null;
        String texto = valor.strip();
        if (texto.startsWith("+55")) texto = texto.substring(3);
        if (!texto.matches("[0-9().\\- ]*")) return null;
        return Formatadores.soDigitos(texto);
    }

    public static boolean telefoneValidoOuExistente(String valor, String original) {
        return telefoneValido(valor) || (valor!=null && !valor.isBlank()
                && original!=null && valor.strip().equals(original.strip()));
    }

    /** Exibe um contato legado sem modificar seu valor durante a abertura da edição. */
    public static void carregarTelefoneExistente(TextField campo, String valor) {
        ValidacaoFormulario.carregarExistente(campo,valor);
    }

    private static void aplicar(TextField campo, boolean cpf) {
        campo.setTextFormatter(new TextFormatter<>(alteracao -> {
            if (!alteracao.isContentChange()) return alteracao;
            ajustarExclusaoDeSeparador(alteracao, cpf);
            String proposto = alteracao.getControlNewText();
            boolean internacional = !cpf && proposto.stripLeading().startsWith("+55");
            String digitos = cpf && proposto.matches("[0-9.\\- ]*")
                    ? Formatadores.soDigitos(proposto) : cpf ? null : telefoneNacional(proposto);

            if (!cpf && (proposto.equals("+") || proposto.equals("+5"))) return alteracao;
            if (digitos==null || digitos.length()>11) {
                ValidacaoFormulario.recusarEdicao(campo,cpf
                        ? "CPF: use somente dígitos e os separadores da máscara, com até 11 dígitos." : TELEFONE_INVALIDO);
                return null;
            }
            if (internacional && digitos.isEmpty() && proposto.equals("+55")) return alteracao;
            String formatado = cpf ? formatarCpf(digitos)
                    : (internacional ? "+55 " : "") + formatarTelefone(digitos);
            if (formatado.equals(proposto)) return alteracao;

            int cursor = Math.max(0, Math.min(alteracao.getCaretPosition(), proposto.length()));
            int digitosAntes = Formatadores.soDigitos(proposto.substring(0, cursor)).length();
            int posicao = 0;
            while (posicao < formatado.length() && digitosAntes > 0) {
                if (Character.isDigit(formatado.charAt(posicao))) digitosAntes--;
                posicao++;
            }
            alteracao.setRange(0, alteracao.getControlText().length());
            alteracao.setText(formatado);
            alteracao.selectRange(posicao, posicao);
            return alteracao;
        }));
    }

    private static void ajustarExclusaoDeSeparador(TextFormatter.Change alteracao, boolean cpf) {
        if (!alteracao.isDeleted() || !alteracao.getText().isEmpty()
                || alteracao.getControlAnchor()!=alteracao.getControlCaretPosition()) return;
        String texto = alteracao.getControlText();
        int inicio = alteracao.getRangeStart(), fim = alteracao.getRangeEnd();
        if (!texto.substring(inicio, fim).matches("[().\\- ]+")) return;

        if (alteracao.getControlCaretPosition()==inicio) {
            while (fim<texto.length() && !Character.isDigit(texto.charAt(fim))) fim++;
            if (fim<texto.length()) alteracao.setRange(inicio,fim+1);
        } else {
            int limite = !cpf && texto.startsWith("+55") ? 3 : 0;
            int digito = inicio-1;
            while (digito>=limite && !Character.isDigit(texto.charAt(digito))) digito--;
            if (digito>=limite) {
                alteracao.setRange(digito,fim);
                alteracao.selectRange(digito,digito);
            }
        }
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
