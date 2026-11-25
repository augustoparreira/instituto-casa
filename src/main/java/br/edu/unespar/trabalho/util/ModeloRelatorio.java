package br.edu.unespar.trabalho.util;

import br.edu.unespar.trabalho.model.RelatorioAcompanhamento;
import br.edu.unespar.trabalho.model.RelatorioAcompanhamento.Campo;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Modelo A4 do relatório institucional, compartilhado pela prévia e pela impressão. */
public final class ModeloRelatorio {
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM / yyyy", Locale.forLanguageTag("pt-BR"));
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"));
    private static final double ESPACO = 8;
    private static final double RODAPE = 16;
    private static final double MARGEM_INTERNA = 14;
    private static final double ALTURA_MINIMA_CAIXA = 90;

    private ModeloRelatorio() { }

    public static List<VBox> prepararPaginas(RelatorioAcompanhamento relatorio, double largura, double altura) {
        if (largura < 300 || altura < 400) throw new IllegalArgumentException("Área de impressão insuficiente.");
        List<Pagina> paginas = new ArrayList<>();
        Pagina atual = novaPagina(paginas, largura, altura);
        atual.adicionar(cabecalho(largura));
        atual.adicionar(titulo(relatorio, largura));

        atual = adicionarSecao(paginas, atual, "Identificação",
                textoIdentificacao(relatorio), false);
        atual = adicionarSecao(paginas, atual, "1. REGISTRO DE FREQUÊNCIA",
                relatorio.getRegistroFrequencia(), true);
        adicionarSecao(paginas, atual,
                "2. DESCUMPRIMENTO DA MEDIDA E CONTATO COM A FAMÍLIA E ACORDO FIRMADO",
                relatorio.getDescumprimento(), true);

        List<VBox> folhas = new ArrayList<>();
        for (int i = 0; i < paginas.size(); i++) {
            Pagina pagina = paginas.get(i);
            String data = i == paginas.size() - 1
                    ? "Apucarana, " + relatorio.getDataEmissao().format(DATA) : "";
            Region espaco = new Region();
            HBox.setHgrow(espaco, Priority.ALWAYS);
            HBox rodape = new HBox(texto(data, 0, 10, false), espaco,
                    texto("Página " + (i + 1) + " de " + paginas.size(), 0, 10, false));
            rodape.setPrefWidth(largura);
            rodape.setMinHeight(RODAPE);
            rodape.setPrefHeight(RODAPE);
            var espacoVertical = new Region();
            VBox.setVgrow(espacoVertical, Priority.ALWAYS);
            pagina.conteudo.getChildren().addAll(espacoVertical, rodape);
            folhas.add(pagina.conteudo);
        }
        return folhas;
    }

    private static Pagina adicionarSecao(List<Pagina> paginas, Pagina pagina, String titulo,
                                         String conteudo, boolean emCaixa) {
        String restante = conteudo.isEmpty() ? " " : conteudo;
        double largura = pagina.largura;
        double larguraTexto = emCaixa ? largura - 2*MARGEM_INTERNA : largura;
        double alturaMinima = emCaixa ? ALTURA_MINIMA_CAIXA : medir(texto("Ag", larguraTexto, 11, false), larguraTexto);
        boolean continuacao = false;
        while (!restante.isEmpty()) {
            Text cabecalho = texto(titulo + (continuacao ? " (continuação)" : ""), largura, 12, true);
            double disponivel = pagina.disponivel() - medir(cabecalho, largura) - ESPACO;
            if (disponivel < alturaMinima + 6) {
                pagina = novaPagina(paginas, largura, pagina.altura);
                disponivel = pagina.disponivel() - medir(cabecalho, largura) - ESPACO;
            }
            // A folga cobre o arredondamento do layout e o contorno da caixa.
            int corte = cortar(restante, larguraTexto, disponivel - (emCaixa ? 2*MARGEM_INTERNA : 0) - 4);
            if (corte==0) throw new IllegalArgumentException("Área insuficiente para o relatório.");
            Text corpo = texto(restante.substring(0,corte), larguraTexto, 11, false);
            Node bloco = corpo;
            if (emCaixa) {
                VBox caixa = new VBox(corpo);
                caixa.setPadding(new Insets(MARGEM_INTERNA));
                caixa.setMinHeight(ALTURA_MINIMA_CAIXA);
                caixa.setStyle("-fx-border-color: #222222; -fx-border-width: 1; -fx-background-color: white;");
                bloco = caixa;
            }
            pagina.adicionar(new VBox(ESPACO, cabecalho, bloco));
            restante = restante.substring(corte);
            if (!restante.isEmpty()) pagina = novaPagina(paginas, largura, pagina.altura);
            continuacao = true;
        }
        return pagina;
    }

    private static double medir(Node bloco, double largura) {
        double altura = bloco instanceof Region regiao ? regiao.prefHeight(largura) : bloco.getLayoutBounds().getHeight();
        return Math.ceil(altura) + 2;
    }

    private static int cortar(String texto, double largura, double altura) {
        int inicio = 0, fim = texto.length();
        while (inicio < fim) {
            int meio = (inicio + fim + 1) / 2;
            if (texto(texto.substring(0, meio), largura, 11, false).getLayoutBounds().getHeight() <= altura)
                inicio = meio;
            else fim = meio - 1;
        }
        if (inicio < texto.length() && inicio > 0 && Character.isHighSurrogate(texto.charAt(inicio - 1))) inicio--;
        if (inicio < texto.length()) {
            int espaco = texto.lastIndexOf(' ', inicio - 1);
            int quebra = texto.lastIndexOf('\n', inicio - 1);
            int corte = Math.max(espaco, quebra);
            if (corte > inicio / 2) inicio = corte + 1;
        }
        return inicio;
    }

    private static HBox cabecalho(double largura) {
        ImageView logo = new ImageView(new Image(ModeloRelatorio.class.getResource("/View/logo-casa.jpeg").toExternalForm()));
        logo.setFitWidth(112);
        logo.setFitHeight(112);
        logo.setPreserveRatio(true);
        VBox dados = new VBox(3,
                texto("CENTRO DE APOIO SOCIAL AO ADOLESCENTE", largura - 132, 13, true),
                texto("CNPJ: 04.313.535/0001-73", largura - 132, 11, false),
                texto("Rua Antônio Ostrenski, 100 – Apucarana – Paraná", largura - 132, 11, false),
                texto("Fone: (43) 3033 2023", largura - 132, 11, false),
                texto("casaapucarana@gmail.com", largura - 132, 11, false));
        dados.setAlignment(Pos.CENTER);
        HBox cabecalho = new HBox(20, logo, dados);
        cabecalho.setAlignment(Pos.CENTER_LEFT);
        return cabecalho;
    }

    private static VBox titulo(RelatorioAcompanhamento relatorio, double largura) {
        VBox titulo = new VBox(3,
                texto("RELATÓRIO DE ACOMPANHAMENTO DE", largura, 14, true),
                texto("MEDIDA SOCIOEDUCATIVA", largura, 14, true),
                texto(relatorio.getCompetencia().atDay(1).format(MES), largura, 12, true));
        titulo.setAlignment(Pos.CENTER);
        for (var linha : titulo.getChildren()) ((Text) linha).setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        return titulo;
    }

    private static String textoIdentificacao(RelatorioAcompanhamento relatorio) {
        StringBuilder linhas = new StringBuilder();
        for (Campo campo : Campo.values())
            linhas.append(campo.getRotulo()).append(": ").append(relatorio.getCampo(campo)).append('\n');
        return linhas.toString();
    }

    private static Text texto(String valor, double largura, int tamanho, boolean negrito) {
        Text texto = new Text(valor);
        texto.setFont(Font.font("Arial", negrito ? FontWeight.BOLD : FontWeight.NORMAL, tamanho));
        // A extremidade visual de alguns glifos ultrapassa a largura de quebra.
        texto.setWrappingWidth(Math.max(0, largura - 4));
        return texto;
    }

    private static Pagina novaPagina(List<Pagina> paginas, double largura, double altura) {
        Pagina pagina = new Pagina(largura, altura);
        paginas.add(pagina);
        return pagina;
    }

    private static final class Pagina {
        private final VBox conteudo = new VBox(ESPACO);
        private final double largura, altura;
        private double usada;

        private Pagina(double largura, double altura) {
            this.largura = largura;
            this.altura = altura;
            conteudo.setMinSize(largura, altura);
            conteudo.setPrefSize(largura, altura);
            conteudo.setMaxSize(largura, altura);
            conteudo.setStyle("-fx-background-color: white;");
        }

        private double disponivel() {
            return altura - RODAPE - ESPACO - usada;
        }

        private void adicionar(Node bloco) {
            double alturaBloco = medir(bloco, largura);
            if (alturaBloco > disponivel())
                throw new IllegalArgumentException("Área insuficiente para o relatório.");
            conteudo.getChildren().add(bloco);
            usada += alturaBloco + ESPACO;
        }
    }
}
