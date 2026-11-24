package br.edu.unespar.trabalho.util;

import br.edu.unespar.trabalho.model.RelatorioAcompanhamento;
import br.edu.unespar.trabalho.model.RelatorioAcompanhamento.Campo;
import javafx.geometry.Insets;
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
    private static final double RODAPE = 32;

    private ModeloRelatorio() { }

    public static List<VBox> prepararPaginas(RelatorioAcompanhamento relatorio, double largura, double altura) {
        if (largura < 300 || altura < 400) throw new IllegalArgumentException("Área de impressão insuficiente.");
        List<Pagina> paginas = new ArrayList<>();
        Pagina atual = novaPagina(paginas, largura);
        atual.adicionar(cabecalho(largura), 118);
        atual.adicionar(titulo(relatorio, largura), 74);
        VBox identificacao = identificacao(relatorio, largura);
        double alturaIdentificacao = 24 + textoIdentificacao(relatorio, largura).getLayoutBounds().getHeight();
        atual.adicionar(identificacao, alturaIdentificacao);

        atual = adicionarSecao(paginas, atual, "1. REGISTRO DE FREQUÊNCIA",
                relatorio.getRegistroFrequencia(), largura, altura);
        adicionarSecao(paginas, atual,
                "2. DESCUMPRIMENTO DA MEDIDA E CONTATO COM A FAMÍLIA E ACORDO FIRMADO",
                relatorio.getDescumprimento(), largura, altura);

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
            pagina.conteudo.getChildren().add(rodape);
            folhas.add(pagina.conteudo);
        }
        return folhas;
    }

    private static Pagina adicionarSecao(List<Pagina> paginas, Pagina pagina, String titulo,
                                         String conteudo, double largura, double altura) {
        String restante = conteudo.isEmpty() ? " " : conteudo;
        boolean continuacao = false;
        while (!restante.isEmpty()) {
            String subtitulo = titulo + (continuacao ? " (continuação)" : "");
            Text cabecalho = texto(subtitulo, largura, 12, true);
            double alturaCabecalho = cabecalho.getLayoutBounds().getHeight();
            double disponivel = altura - RODAPE - pagina.usada;
            if (disponivel < alturaCabecalho + 106) {
                pagina = novaPagina(paginas, largura);
                disponivel = altura - RODAPE;
            }
            int corte = cortar(restante, largura - 28, disponivel - alturaCabecalho - 40);
            if (corte == 0) throw new IllegalArgumentException("Área insuficiente para o relatório.");
            String trecho = restante.substring(0, corte);
            Text corpo = texto(trecho, largura - 28, 11, false);
            VBox caixa = new VBox(corpo);
            caixa.setPadding(new Insets(12));
            caixa.setMinHeight(90);
            caixa.setStyle("-fx-border-color: #222222; -fx-border-width: 1.2; -fx-background-color: white;");
            VBox secao = new VBox(8, cabecalho, caixa);
            pagina.adicionar(secao, cabecalho.getLayoutBounds().getHeight()
                    + 8 + Math.max(90, corpo.getLayoutBounds().getHeight() + 24));
            restante = restante.substring(corte);
            continuacao = true;
        }
        return pagina;
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

    private static VBox identificacao(RelatorioAcompanhamento relatorio, double largura) {
        return new VBox(5, texto("Identificação", largura, 13, true), textoIdentificacao(relatorio, largura));
    }

    private static Text textoIdentificacao(RelatorioAcompanhamento relatorio, double largura) {
        StringBuilder linhas = new StringBuilder();
        for (Campo campo : Campo.values())
            linhas.append(campo.getRotulo()).append(": ").append(relatorio.getCampo(campo)).append('\n');
        return texto(linhas.toString(), largura, 11, false);
    }

    private static Text texto(String valor, double largura, int tamanho, boolean negrito) {
        Text texto = new Text(valor);
        texto.setFont(Font.font("Arial", negrito ? FontWeight.BOLD : FontWeight.NORMAL, tamanho));
        texto.setWrappingWidth(largura);
        return texto;
    }

    private static Pagina novaPagina(List<Pagina> paginas, double largura) {
        Pagina pagina = new Pagina(largura);
        paginas.add(pagina);
        return pagina;
    }

    private static final class Pagina {
        private final VBox conteudo;
        private double usada;

        private Pagina(double largura) {
            conteudo = new VBox(8);
            conteudo.setPrefWidth(largura);
            conteudo.setMinWidth(largura);
            conteudo.setMaxWidth(largura);
            conteudo.setStyle("-fx-background-color: white;");
        }

        private void adicionar(javafx.scene.Node bloco, double altura) {
            conteudo.getChildren().add(bloco);
            usada += altura + 8;
        }
    }
}
