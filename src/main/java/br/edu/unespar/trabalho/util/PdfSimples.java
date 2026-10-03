package br.edu.unespar.trabalho.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gerador de PDF mínimo (só texto, A4, fontes Helvetica) sem dependências externas.
 * Suporta acentuação em português (WinAnsi), quebra automática de linha e várias páginas.
 */
public class PdfSimples {

    private static final float LARGURA = 595.28f;
    private static final float ALTURA = 841.89f;
    private static final float MARGEM = 56f;
    private static final Charset WIN_1252 = Charset.forName("windows-1252");

    // Largura dos caracteres ASCII 32..126 da Helvetica (unidades de 1/1000 do corpo da fonte)
    private static final int[] LARGURAS = {
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
            1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
            333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
            556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584
    };

    private final List<StringBuilder> paginas = new ArrayList<>();
    private StringBuilder atual;
    private float y;

    public PdfSimples() {
        novaPagina();
    }

    // ---------- API de conteúdo ----------

    public void titulo(String texto) {
        escrever(texto, 20, true, 0.098f, 0.322f, 0.243f, 26);
    }

    public void subtitulo(String texto) {
        escrever(texto, 11, false, 0.4f, 0.4f, 0.4f, 16);
    }

    /** Rótulo pequeno em maiúsculas (ex.: DIAGNÓSTICO INICIAL). */
    public void rotulo(String texto) {
        espaco(8);
        escrever(texto.toUpperCase(Locale.ROOT), 8, true, 0.45f, 0.45f, 0.45f, 12);
    }

    public void paragrafo(String texto) {
        escrever(texto, 11, false, 0.12f, 0.12f, 0.12f, 15);
    }

    public void espaco(float pontos) {
        y -= pontos;
    }

    public void linha() {
        garantirEspaco(10);
        atual.append("0.8 G 0.8 w ").append(fmt(MARGEM)).append(' ').append(fmt(y)).append(" m ")
                .append(fmt(LARGURA - MARGEM)).append(' ').append(fmt(y)).append(" l S\n");
        y -= 10;
    }

    public void salvar(Path destino) throws IOException {
        Files.write(destino, gerarBytes());
    }

    // ---------- Texto ----------

    private void escrever(String texto, float tamanho, boolean negrito, float r, float g, float b, float entrelinha) {
        String base = texto == null ? "" : texto;
        for (String paragrafo : base.split("\\R", -1)) {
            for (String linha : quebrar(paragrafo, tamanho, negrito, LARGURA - 2 * MARGEM)) {
                garantirEspaco(entrelinha);
                y -= entrelinha;
                atual.append("BT /").append(negrito ? "F2" : "F1").append(' ').append(fmt(tamanho)).append(" Tf ")
                        .append(fmt(r)).append(' ').append(fmt(g)).append(' ').append(fmt(b)).append(" rg ")
                        .append(fmt(MARGEM)).append(' ').append(fmt(y)).append(" Td (")
                        .append(escapar(linha)).append(") Tj ET\n");
            }
        }
    }

    private List<String> quebrar(String texto, float tamanho, boolean negrito, float larguraMax) {
        List<String> linhas = new ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            String tentativa = linha.length() == 0 ? palavra : linha + " " + palavra;
            if (largura(tentativa, tamanho, negrito) <= larguraMax || linha.length() == 0) {
                linha = new StringBuilder(tentativa);
            } else {
                linhas.add(linha.toString());
                linha = new StringBuilder(palavra);
            }
        }
        linhas.add(linha.toString());
        return linhas;
    }

    private float largura(String texto, float tamanho, boolean negrito) {
        float total = 0;
        for (char c : texto.toCharArray()) {
            char base = c;
            if (c < 32 || c > 126) {
                String d = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
                base = d.charAt(0);
            }
            total += (base >= 32 && base <= 126) ? LARGURAS[base - 32] : 556;
        }
        return total * tamanho / 1000f * (negrito ? 1.06f : 1f);
    }

    private void garantirEspaco(float necessario) {
        if (y - necessario < MARGEM) {
            novaPagina();
        }
    }

    private void novaPagina() {
        atual = new StringBuilder();
        paginas.add(atual);
        y = ALTURA - MARGEM;
    }

    private String escapar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (byte b : texto.getBytes(WIN_1252)) {
            int v = b & 0xFF;
            if (v == '\\' || v == '(' || v == ')') {
                sb.append('\\').append((char) v);
            } else if (v < 32 || v > 126) {
                sb.append('\\').append(String.format("%03o", v));
            } else {
                sb.append((char) v);
            }
        }
        return sb.toString();
    }

    private static String fmt(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    // ---------- Montagem do arquivo ----------

    private byte[] gerarBytes() throws IOException {
        int n = paginas.size();
        // objetos: 1 catálogo, 2 páginas, 3 e 4 fontes, depois (página, conteúdo) para cada página
        List<String> objetos = new ArrayList<>();
        objetos.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < n; i++) {
            kids.append(5 + 2 * i).append(" 0 R ");
        }
        objetos.add("<< /Type /Pages /Kids [" + kids + "] /Count " + n + " >>");
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        for (int i = 0; i < n; i++) {
            int conteudo = 6 + 2 * i;
            objetos.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + fmt(LARGURA) + " " + fmt(ALTURA) + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + conteudo + " 0 R >>");
            String stream = paginas.get(i).toString();
            int tamanho = stream.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1).length;
            objetos.add("<< /Length " + tamanho + " >>\nstream\n" + stream + "endstream");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        int[] posicoes = new int[objetos.size()];
        for (int i = 0; i < objetos.size(); i++) {
            posicoes[i] = out.size();
            out.write(((i + 1) + " 0 obj\n" + objetos.get(i) + "\nendobj\n")
                    .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        }
        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n");
        for (int p : posicoes) {
            x.append(String.format("%010d 00000 n \n", p));
        }
        x.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        out.write(x.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }
}