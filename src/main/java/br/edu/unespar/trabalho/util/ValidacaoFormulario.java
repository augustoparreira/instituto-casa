package br.edu.unespar.trabalho.util;

import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import java.util.function.Predicate;

/** Regras compartilhadas dos campos de cadastro. */
public final class ValidacaoFormulario {
    private ValidacaoFormulario() { }

    public static void limitarTexto(TextInputControl campo, int limite, String rotulo) {
        filtrar(campo, texto -> tamanho(texto)<=limite, mensagemLimite(rotulo,limite));
    }

    public static void limitarSenha(TextInputControl campo, int limite) {
        filtrar(campo, texto -> texto.length()<=limite, mensagemLimite("Senha",limite));
    }

    public static void limitarDigitos(TextInputControl campo, int limite, String rotulo) {
        filtrar(campo, texto -> texto.length()<=limite && texto.matches("[0-9]*"),
                rotulo + ": use até " + limite + " dígitos, sem letras ou sinais.");
    }

    public static void limitarInteiro(TextInputControl campo, int maximo, String rotulo) {
        int digitos = Integer.toString(maximo).length();
        filtrar(campo, texto -> texto.isEmpty() || (texto.length()<=digitos
                        && texto.matches("[0-9]+") && Long.parseLong(texto)<=maximo),
                rotulo + ": informe um número inteiro entre 0 e " + maximo + ".");
    }

    public static void limitarDecimal(TextInputControl campo, boolean agrupamento) {
        String formato = agrupamento ? "-?(?:[0-9]*(?:[.,][0-9]*)?|[0-9]{1,3}(?:\\.[0-9]{3})+(?:,[0-9]*)?)"
                : "-?[0-9]*(?:[.,][0-9]*)?";
        filtrar(campo, texto -> texto.matches(formato), "Informe um valor decimal, como 1,50.");
    }

    public static void carregarExistente(TextInputControl campo, String valor) {
        TextFormatter<?> formatador = campo.getTextFormatter();
        campo.setTextFormatter(null);
        try {
            campo.setText(valor==null ? "" : valor);
        } finally {
            campo.setTextFormatter(formatador);
        }
    }

    public static void filtrar(TextInputControl campo, Predicate<String> aceita, String mensagem) {
        campo.setTextFormatter(new TextFormatter<>(mudanca -> {
            if (!mudanca.isContentChange() || aceita.test(mudanca.getControlNewText())) return mudanca;
            recusarEdicao(campo,mensagem);
            return null;
        }));
    }

    static void recusarEdicao(TextInputControl campo, String mensagem) {
        if (!campo.isFocused() || campo.getScene()==null || campo.getScene().getWindow()==null
                || !campo.getScene().getWindow().isShowing()) return;
        Bounds posicao = campo.localToScreen(campo.getBoundsInLocal());
        if (posicao==null) return;
        String chave = ValidacaoFormulario.class.getName();
        if (campo.getProperties().containsKey(chave)) return;
        Tooltip aviso = new Tooltip(mensagem + " A edição foi recusada.");
        aviso.setWrapText(true);
        aviso.setMaxWidth(340);
        campo.getProperties().put(chave,aviso);
        aviso.setOnHidden(evento -> campo.getProperties().remove(chave));
        aviso.show(campo,posicao.getMinX(),posicao.getMaxY()+4);
        PauseTransition duracao = new PauseTransition(Duration.seconds(3));
        duracao.setOnFinished(evento -> aviso.hide());
        duracao.play();
    }

    public static int tamanho(String texto) {
        return texto.codePointCount(0,texto.length());
    }

    private static String mensagemLimite(String rotulo, int limite) {
        return rotulo+": no máximo "+limite+" caracteres.";
    }

    public static String texto(String valor, int limite, String rotulo, boolean obrigatorio) {
        String texto = valor==null ? "" : valor.strip();
        if (obrigatorio && texto.isEmpty()) throw new IllegalArgumentException("Informe "+rotulo+".");
        if (tamanho(texto)>limite) throw new IllegalArgumentException(mensagemLimite(rotulo,limite));
        return texto;
    }

    public static String email(String valor) {
        return email(valor, null);
    }

    public static String email(String valor, String original) {
        String email = texto(valor,90,"E-mail",false);
        if (!email.isEmpty() && (original==null || !email.equals(original.strip()))
                && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Informe um e-mail válido.");
        return email;
    }
}
